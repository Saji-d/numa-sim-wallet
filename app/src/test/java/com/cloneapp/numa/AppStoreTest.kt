package com.cloneapp.numa

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AppStoreTest {
    private val t0 = 1_700_000_000_000L
    private var now = t0
    private lateinit var storage: MemoryStorage
    private lateinit var store: AppStore

    private val us = countryByCode("US")!!   // 5 credits / month
    private val gb = countryByCode("GB")!!   // 6 credits / month

    @Before
    fun setUp() {
        now = t0
        storage = MemoryStorage()
        store = AppStore(storage, clock = { now })
    }

    private fun signedIn(): AppStore {
        assertNull(store.signIn("Sam", "sam@example.com"))
        return store
    }

    // ---- sign-in / profile ----

    @Test fun signIn_rejectsShortName() {
        assertNotNull(store.signIn("S", "sam@example.com"))
        assertFalse(store.signedIn)
    }

    @Test fun signIn_rejectsBlankNameAfterTrim() {
        assertNotNull(store.signIn("   ", "sam@example.com"))
        assertFalse(store.signedIn)
    }

    @Test fun signIn_rejectsInvalidEmail() {
        assertNotNull(store.signIn("Sam", "not-an-email"))
        assertFalse(store.signedIn)
        assertEquals(0, store.credits)
    }

    @Test fun signIn_succeedsAndGrantsStarterCreditsOnce() {
        assertNull(store.signIn("  Sam  ", " sam@example.com "))
        assertTrue(store.signedIn)
        assertEquals("Sam", store.displayName)
        assertEquals("sam@example.com", store.email)
        assertEquals(STARTER_CREDITS, store.credits)
        assertEquals(1, store.txns.size)
        assertEquals(STARTER_CREDITS, store.txns[0].amount)

        // Signing out and back in must not hand out the starter credits again.
        store.signOut()
        assertNull(store.signIn("Sam", "sam@example.com"))
        assertEquals(STARTER_CREDITS, store.credits)
        assertEquals(1, store.txns.size)
    }

    @Test fun updateProfile_validatesAndUpdates() {
        signedIn()
        assertNotNull(store.updateProfile("A", "a@b.co"))
        assertNotNull(store.updateProfile("Alex", "bad"))
        assertEquals("Sam", store.displayName)
        assertNull(store.updateProfile(" Alex ", " alex@example.org "))
        assertEquals("Alex", store.displayName)
        assertEquals("alex@example.org", store.email)
    }

    @Test fun signOut_keepsData() {
        signedIn()
        assertNull(store.buyNumber("+1 555 0142", us))
        store.signOut()
        assertFalse(store.signedIn)
        assertEquals(1, store.numbers.size)
        assertEquals(1, store.threads.size)
        assertEquals(STARTER_CREDITS - us.pricePerMonth, store.credits)
        assertEquals("Sam", store.displayName)
    }

    @Test fun deleteAllData_wipesStateAndStorage() {
        signedIn()
        store.buyNumber("+1 555 0142", us)
        store.logCall("+15550100", 5)
        store.buyEsim(planById("US-1")!!)
        store.setThemeMode(ThemeMode.DARK)
        store.setNotifications(false)
        store.deleteAllData()

        assertFalse(store.signedIn)
        assertEquals("", store.displayName)
        assertEquals("", store.email)
        assertEquals(0, store.credits)
        assertEquals(ThemeMode.SYSTEM, store.themeMode)
        assertTrue(store.notifications)
        assertTrue(store.numbers.isEmpty())
        assertTrue(store.threads.isEmpty())
        assertTrue(store.calls.isEmpty())
        assertTrue(store.esims.isEmpty())
        assertTrue(store.txns.isEmpty())
        assertNull(storage.load())
    }

    // ---- wallet ----

    @Test fun addCredits_bounds() {
        signedIn()
        assertNotNull(store.addCredits(0))
        assertNotNull(store.addCredits(-5))
        assertNotNull(store.addCredits(MAX_TOP_UP + 1))
        assertEquals(STARTER_CREDITS, store.credits)

        assertNull(store.addCredits(1))
        assertNull(store.addCredits(MAX_TOP_UP))
        assertEquals(STARTER_CREDITS + 1 + MAX_TOP_UP, store.credits)
        // Most recent ledger entry first.
        assertEquals(MAX_TOP_UP, store.txns[0].amount)
        assertEquals(1, store.txns[1].amount)
    }

    // ---- numbers ----

    @Test fun buyNumber_success_chargesAndCreatesWelcomeThread() {
        signedIn()
        assertNull(store.buyNumber("+1 555 0142", us))

        assertEquals(STARTER_CREDITS - 5, store.credits)
        assertEquals(1, store.numbers.size)
        val n = store.numbers[0]
        assertEquals("+1 555 0142", n.e164)
        assertEquals(us, n.country)
        assertEquals("Line 1", n.label)
        assertEquals(t0, n.purchasedAt)
        assertEquals(t0 + NUMBER_TERM_DAYS * DAY_MS, n.expiresAt)
        assertFalse(store.isExpired(n))

        assertEquals(-5, store.txns[0].amount)

        assertEquals(1, store.threads.size)
        val th = store.threads[0]
        assertEquals("Numa", th.peer)
        assertEquals(n.id, th.numberId)
        assertTrue(th.unread)
        assertEquals(1, th.messages.size)
        assertFalse(th.messages[0].fromMe)
        assertEquals(t0, th.messages[0].at)
        assertEquals(1, store.unreadCount)
        assertSame(n, store.number(n.id))
        assertSame(th, store.thread(th.id))
    }

    @Test fun buyNumber_labelsIncrement() {
        signedIn()
        store.buyNumber("+1 555 0142", us)
        store.buyNumber("+1 555 0187", us)
        assertEquals(listOf("Line 1", "Line 2"), store.numbers.map { it.label })
    }

    @Test fun buyNumber_duplicateRejectedWithoutCharge() {
        signedIn()
        assertNull(store.buyNumber("+1 555 0142", us))
        val credits = store.credits
        assertEquals("You already own this number.", store.buyNumber("+1 555 0142", us))
        assertEquals(credits, store.credits)
        assertEquals(1, store.numbers.size)
        assertEquals(1, store.threads.size)
    }

    @Test fun buyNumber_insufficientCredits() {
        signedIn()                               // 10 credits
        assertNull(store.buyNumber("+1 555 0142", us))   // 5 left
        assertEquals("Not enough credits (need 6, have 5).", store.buyNumber("+44 555 0142", gb))
        assertEquals(5, store.credits)
        assertEquals(1, store.numbers.size)
    }

    @Test fun renameNumber_validation() {
        signedIn()
        store.buyNumber("+1 555 0142", us)
        val id = store.numbers[0].id
        assertEquals("Number not found.", store.renameNumber(9999, "Work"))
        assertEquals("Enter a name.", store.renameNumber(id, "   "))
        assertNotNull(store.renameNumber(id, "x".repeat(25)))
        assertEquals("Line 1", store.numbers[0].label)

        assertNull(store.renameNumber(id, "  Work  "))
        assertEquals("Work", store.numbers[0].label)
        assertNull(store.renameNumber(id, "y".repeat(24)))
        assertEquals(24, store.numbers[0].label.length)
    }

    @Test fun renewNumber_extendsFromExpiryAndCharges() {
        signedIn()
        store.buyNumber("+1 555 0142", us)       // 5 credits left
        val id = store.numbers[0].id
        val firstExpiry = store.numbers[0].expiresAt

        now = t0 + 10 * DAY_MS                   // still active: extend from current expiry
        assertNull(store.renewNumber(id))
        assertEquals(firstExpiry + NUMBER_TERM_DAYS * DAY_MS, store.numbers[0].expiresAt)
        assertEquals(0, store.credits)
        assertEquals(-5, store.txns[0].amount)
    }

    @Test fun renewNumber_expiredExtendsFromNow() {
        signedIn()
        store.buyNumber("+1 555 0142", us)
        val id = store.numbers[0].id

        now = t0 + 50 * DAY_MS
        assertTrue(store.isExpired(store.numbers[0]))
        assertNull(store.renewNumber(id))
        assertEquals(now + NUMBER_TERM_DAYS * DAY_MS, store.numbers[0].expiresAt)
        assertFalse(store.isExpired(store.numbers[0]))
    }

    @Test fun renewNumber_failuresDoNotChangeState() {
        signedIn()
        store.buyNumber("+1 555 0142", us)       // 5 left
        val id = store.numbers[0].id
        assertNull(store.renewNumber(id))        // 0 left
        val expiry = store.numbers[0].expiresAt
        assertEquals("Not enough credits (need 5, have 0).", store.renewNumber(id))
        assertEquals(expiry, store.numbers[0].expiresAt)
        assertEquals(0, store.credits)
        assertEquals("Number not found.", store.renewNumber(9999))
    }

    @Test fun release_removesNumberAndItsThreadsOnly() {
        signedIn()
        store.buyNumber("+1 555 0142", us)
        store.buyNumber("+1 555 0187", us)
        assertEquals(2, store.threads.size)
        val first = store.numbers[0]
        store.release(first.id)

        assertEquals(1, store.numbers.size)
        assertEquals("+1 555 0187", store.numbers[0].e164)
        assertEquals(1, store.threads.size)
        assertEquals(store.numbers[0].id, store.threads[0].numberId)
        assertNull(store.number(first.id))
    }

    // ---- messages ----

    private fun withThread(): Int {
        signedIn()
        store.buyNumber("+1 555 0142", us)
        return store.threads[0].id
    }

    @Test fun send_validation() {
        val id = withThread()
        assertEquals("Conversation not found.", store.send(9999, "hi"))
        assertEquals("Type a message first.", store.send(id, ""))
        assertEquals("Type a message first.", store.send(id, "   \n "))
        assertNotNull(store.send(id, "x".repeat(MAX_MESSAGE_LENGTH + 1)))
        assertEquals(1, store.thread(id)!!.messages.size)
    }

    @Test fun send_appendsTrimmedMessageAndClearsUnread() {
        val id = withThread()
        assertTrue(store.thread(id)!!.unread)
        now = t0 + 5_000
        assertNull(store.send(id, "  hello  "))
        val th = store.thread(id)!!
        assertFalse(th.unread)
        assertEquals(2, th.messages.size)
        assertEquals(Message(true, "hello", now), th.last)
        assertEquals(0, store.unreadCount)
    }

    @Test fun send_acceptsExactlyMaxLength() {
        val id = withThread()
        assertNull(store.send(id, "x".repeat(MAX_MESSAGE_LENGTH)))
        assertEquals(MAX_MESSAGE_LENGTH, store.thread(id)!!.last!!.text.length)
    }

    @Test fun markRead_clearsUnreadAndIsSafeForUnknownIds() {
        val id = withThread()
        assertEquals(1, store.unreadCount)
        store.markRead(9999)
        assertEquals(1, store.unreadCount)
        store.markRead(id)
        assertFalse(store.thread(id)!!.unread)
        assertEquals(0, store.unreadCount)
        store.markRead(id) // idempotent
        assertEquals(0, store.unreadCount)
    }

    @Test fun deleteThread_removesOnlyThatThread() {
        val id = withThread()
        store.buyNumber("+1 555 0187", us)
        assertEquals(2, store.threads.size)
        store.deleteThread(id)
        assertNull(store.thread(id))
        assertEquals(1, store.threads.size)
        store.deleteThread(9999) // unknown id is a no-op
        assertEquals(1, store.threads.size)
        assertEquals(2, store.numbers.size) // numbers are unaffected
    }

    // ---- calls ----

    @Test fun logCall_validationAndRecording() {
        signedIn()
        assertEquals("Enter a valid number.", store.logCall("abc", 3))
        assertEquals("Enter a valid number.", store.logCall("12", 3))
        assertTrue(store.calls.isEmpty())

        now = t0 + 1_000
        assertNull(store.logCall(" +1 555 0100 ", 42))
        assertNull(store.logCall("+44 555 0101", -7))
        assertEquals(2, store.calls.size)
        // Newest first; peer trimmed; negative durations clamped to zero.
        assertEquals("+44 555 0101", store.calls[0].peer)
        assertEquals(0, store.calls[0].seconds)
        assertEquals(CallEntry("+1 555 0100", CallDir.OUT, now, 42), store.calls[1])
    }

    @Test fun clearCalls_emptiesHistory() {
        signedIn()
        store.logCall("+15550100", 1)
        store.clearCalls()
        assertTrue(store.calls.isEmpty())
    }

    // ---- eSIM ----

    @Test fun buyEsim_chargesRoundedCredits() {
        signedIn()                               // 10 credits
        val plan = planById("US-1")!!            // 3.5 USD -> 4 credits (Math.round)
        assertEquals(4, plan.credits)
        assertNull(store.buyEsim(plan))
        assertEquals(6, store.credits)
        assertEquals(1, store.esims.size)
        val e = store.esims[0]
        assertEquals(plan, e.plan)
        assertEquals(t0, e.purchasedAt)
        assertNull(e.installedAt)
        assertFalse(e.installed)
        assertNull(e.expiresAt)
        assertEquals(-4, store.txns[0].amount)
        assertTrue(store.txns[0].title.contains("United States"))
        assertSame(e, store.esim(e.id))
    }

    @Test fun buyEsim_insufficientCredits() {
        signedIn()
        val big = planById("US-10")!!            // 16 credits
        assertEquals("Not enough credits (need 16, have 10).", store.buyEsim(big))
        assertEquals(10, store.credits)
        assertTrue(store.esims.isEmpty())
        assertEquals(1, store.txns.size)         // only the starter-credit entry
    }

    @Test fun markEsimInstalled_startsValidityOnceOnly() {
        signedIn()
        val plan = planById("US-1")!!            // 3 days validity
        store.buyEsim(plan)
        val id = store.esims[0].id

        now = t0 + 2 * DAY_MS
        store.markEsimInstalled(id)
        val e = store.esim(id)!!
        assertTrue(e.installed)
        assertEquals(now, e.installedAt)
        assertEquals(now + plan.days * DAY_MS, e.expiresAt)

        val firstInstall = now
        now += DAY_MS
        store.markEsimInstalled(id)              // second call must not restart the clock
        assertEquals(firstInstall, store.esim(id)!!.installedAt)

        store.markEsimInstalled(9999)            // unknown id is a no-op
        assertEquals(1, store.esims.size)
    }

    @Test fun removeEsim_removesIt() {
        signedIn()
        store.buyEsim(planById("US-1")!!)
        val id = store.esims[0].id
        store.removeEsim(id)
        assertTrue(store.esims.isEmpty())
        store.removeEsim(id) // second removal is a no-op
        assertTrue(store.esims.isEmpty())
    }

    // ---- settings ----

    @Test fun settings_setters() {
        store.setThemeMode(ThemeMode.DARK)
        store.setNotifications(false)
        store.setMessageAlerts(false)
        assertEquals(ThemeMode.DARK, store.themeMode)
        assertFalse(store.notifications)
        assertFalse(store.messageAlerts)
    }

    // ---- persistence ----

    @Test fun everyMutationWritesThrough() {
        assertNull(storage.load())
        store.signIn("Sam", "sam@example.com")
        assertNotNull(storage.load())
        storage.clear()
        store.setThemeMode(ThemeMode.LIGHT)
        assertNotNull(storage.load())
    }

    @Test fun persistence_roundTripRestoresAllState() {
        signedIn()
        store.addCredits(40)
        store.buyNumber("+1 555 0142", us)
        store.buyNumber("+44 555 0142", gb)
        store.renameNumber(store.numbers[0].id, "Work")
        store.send(store.threads[0].id, "héllo \"quoted\"\nline two")
        store.logCall("+1 555 0100", 30)
        store.buyEsim(planById("GB-3")!!)
        store.buyEsim(planById("US-1")!!)
        store.markEsimInstalled(store.esims[0].id)
        store.setThemeMode(ThemeMode.DARK)
        store.setNotifications(false)
        store.setMessageAlerts(false)

        val restored = AppStore(storage, clock = { now })

        assertEquals(store.signedIn, restored.signedIn)
        assertEquals(store.displayName, restored.displayName)
        assertEquals(store.email, restored.email)
        assertEquals(store.credits, restored.credits)
        assertEquals(store.themeMode, restored.themeMode)
        assertEquals(store.notifications, restored.notifications)
        assertEquals(store.messageAlerts, restored.messageAlerts)
        assertEquals(store.numbers, restored.numbers)
        assertEquals(store.threads, restored.threads)
        assertEquals(store.calls, restored.calls)
        assertEquals(store.esims, restored.esims)
        assertEquals(store.txns, restored.txns)
        assertEquals(store.unreadCount, restored.unreadCount)
    }

    @Test fun persistence_idsDoNotCollideAfterRestore() {
        signedIn()
        store.addCredits(50)
        store.buyNumber("+1 555 0142", us)
        val restored = AppStore(storage, clock = { now })
        restored.buyNumber("+1 555 0187", us)
        val ids = restored.numbers.map { it.id } + restored.threads.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun persistence_signOutIsRemembered() {
        signedIn()
        store.signOut()
        val restored = AppStore(storage, clock = { now })
        assertFalse(restored.signedIn)
        assertEquals("Sam", restored.displayName)
    }

    @Test fun persistence_corruptJsonStartsFresh() {
        val fresh = AppStore(MemoryStorage("{ this is not json"), clock = { now })
        assertFalse(fresh.signedIn)
        assertEquals(0, fresh.credits)
        assertTrue(fresh.numbers.isEmpty())
        // ...and the store is still usable.
        assertNull(fresh.signIn("Sam", "sam@example.com"))
        assertEquals(STARTER_CREDITS, fresh.credits)
    }

    @Test fun persistence_unknownVersionStartsFresh() {
        signedIn()
        val future = JSONObject(storage.load()!!).put("v", 99).toString()
        val fresh = AppStore(MemoryStorage(future), clock = { now })
        assertFalse(fresh.signedIn)
        assertEquals(0, fresh.credits)
        assertTrue(fresh.txns.isEmpty())
    }
}
