package com.sajid.numa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Single source of truth for app state and business rules.
 *
 * State is held in Compose snapshot state so screens recompose automatically, and every mutation is
 * written through [StateStorage]. Rule-checking methods return `null` on success or a user-facing error
 * message, so screens never have to re-implement validation.
 *
 * Nothing here contacts a carrier, messaging service, payment gateway or eSIM provider: numbers,
 * messages, calls, plans and credits are records kept on this device.
 */
class AppStore(
    private val storage: StateStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var signedIn by mutableStateOf(false); private set
    var displayName by mutableStateOf(""); private set
    var email by mutableStateOf(""); private set
    var credits by mutableStateOf(0); private set
    @set:JvmName("assign_themeMode")
    var themeMode by mutableStateOf(ThemeMode.SYSTEM); private set
    @set:JvmName("assign_notifications")
    var notifications by mutableStateOf(true); private set
    @set:JvmName("assign_messageAlerts")
    var messageAlerts by mutableStateOf(true); private set

    private val _numbers = mutableStateListOf<VirtualNumber>()
    private val _threads = mutableStateListOf<Thread>()
    private val _calls = mutableStateListOf<CallEntry>()
    private val _esims = mutableStateListOf<MyEsim>()
    private val _txns = mutableStateListOf<Txn>()
    private var nextId = 1

    val numbers: List<VirtualNumber> get() = _numbers.toList()
    val threads: List<Thread> get() = _threads.toList()
    val calls: List<CallEntry> get() = _calls.toList()
    val esims: List<MyEsim> get() = _esims.toList()
    val txns: List<Txn> get() = _txns.toList()

    val unreadCount: Int get() = _threads.count { it.unread }

    init {
        storage.load()?.let { SnapshotCodec.decode(it) }?.let(::restore)
    }

    // ---- Lookups ----
    fun number(id: Int) = _numbers.firstOrNull { it.id == id }
    fun thread(id: Int) = _threads.firstOrNull { it.id == id }
    fun esim(id: Int) = _esims.firstOrNull { it.id == id }

    // ---- Profile / session ----
    fun signIn(name: String, mail: String): String? {
        validateProfile(name, mail)?.let { return it }
        displayName = name.trim(); email = mail.trim(); signedIn = true
        if (_txns.isEmpty() && credits == 0) {
            credits = STARTER_CREDITS
            _txns.add(Txn("Starter credits", STARTER_CREDITS, clock()))
        }
        persist(); return null
    }

    fun updateProfile(name: String, mail: String): String? {
        validateProfile(name, mail)?.let { return it }
        displayName = name.trim(); email = mail.trim(); persist(); return null
    }

    private fun validateProfile(name: String, mail: String): String? = when {
        name.trim().length < 2 -> "Enter your name (at least 2 characters)."
        !isValidEmail(mail) -> "Enter a valid email address."
        else -> null
    }

    /** Ends the session; data stays on the device. */
    fun signOut() { signedIn = false; persist() }

    /** Erases every record and preference on this device. */
    fun deleteAllData() {
        signedIn = false; displayName = ""; email = ""; credits = 0
        themeMode = ThemeMode.SYSTEM; notifications = true; messageAlerts = true
        _numbers.clear(); _threads.clear(); _calls.clear(); _esims.clear(); _txns.clear(); nextId = 1
        storage.clear()
    }

    // ---- Settings ----
    fun setThemeMode(m: ThemeMode) { themeMode = m; persist() }
    fun setNotifications(on: Boolean) { notifications = on; persist() }
    fun setMessageAlerts(on: Boolean) { messageAlerts = on; persist() }

    // ---- Wallet ----
    /** Adds credits to the local balance. No payment is processed. */
    fun addCredits(amount: Int): String? {
        if (amount < 1) return "Enter an amount of at least 1 credit."
        if (amount > MAX_TOP_UP) return "The maximum per top-up is $MAX_TOP_UP credits."
        credits += amount
        _txns.add(0, Txn("Credits added", amount, clock())); persist(); return null
    }

    private fun spend(cost: Int, title: String): String? {
        if (credits < cost) return "Not enough credits (need $cost, have $credits)."
        credits -= cost
        _txns.add(0, Txn(title, -cost, clock())); return null
    }

    // ---- Numbers ----
    fun buyNumber(e164: String, c: Country): String? {
        if (_numbers.any { it.e164 == e164 }) return "You already own this number."
        spend(c.pricePerMonth, "Number $e164 (30 days)")?.let { return it }
        val now = clock()
        val n = VirtualNumber(nextId++, e164, c, "Line ${_numbers.size + 1}", now, now + NUMBER_TERM_DAYS * DAY_MS)
        _numbers.add(n)
        _threads.add(0, Thread(nextId++, "Numa", n.id, listOf(Message(false,
            "$e164 is now on your account. Incoming and outgoing SMS need a messaging provider, which this build does not have.", now)), true))
        persist(); return null
    }

    fun renameNumber(id: Int, label: String): String? {
        val i = _numbers.indexOfFirst { it.id == id }
        if (i < 0) return "Number not found."
        val l = label.trim()
        if (l.isEmpty()) return "Enter a name."
        if (l.length > 24) return "Keep the name to 24 characters."
        _numbers[i] = _numbers[i].copy(label = l); persist(); return null
    }

    /** Extends the term by 30 days from the later of now and the current expiry. */
    fun renewNumber(id: Int): String? {
        val i = _numbers.indexOfFirst { it.id == id }
        if (i < 0) return "Number not found."
        val n = _numbers[i]
        spend(n.country.pricePerMonth, "Renewal ${n.e164} (30 days)")?.let { return it }
        _numbers[i] = n.copy(expiresAt = maxOf(n.expiresAt, clock()) + NUMBER_TERM_DAYS * DAY_MS)
        persist(); return null
    }

    fun isExpired(n: VirtualNumber) = n.expiresAt <= clock()

    fun release(id: Int) {
        _numbers.removeAll { it.id == id }
        _threads.removeAll { it.numberId == id }
        persist()
    }

    // ---- Messages ----
    fun send(threadId: Int, text: String): String? {
        val i = _threads.indexOfFirst { it.id == threadId }
        if (i < 0) return "Conversation not found."
        val t = text.trim()
        if (t.isEmpty()) return "Type a message first."
        if (t.length > MAX_MESSAGE_LENGTH) return "Messages are limited to $MAX_MESSAGE_LENGTH characters."
        _threads[i] = _threads[i].let { it.copy(messages = it.messages + Message(true, t, clock()), unread = false) }
        persist(); return null
    }

    fun markRead(threadId: Int) {
        val i = _threads.indexOfFirst { it.id == threadId }
        if (i >= 0 && _threads[i].unread) { _threads[i] = _threads[i].copy(unread = false); persist() }
    }

    fun deleteThread(threadId: Int) { if (_threads.removeAll { it.id == threadId }) persist() }

    // ---- Calls ----
    /** Records an attempted outgoing call. Returns an error if [peer] is not a dialable string. */
    fun logCall(peer: String, seconds: Int): String? {
        if (!isValidDialString(peer)) return "Enter a valid number."
        _calls.add(0, CallEntry(peer.trim(), CallDir.OUT, clock(), seconds.coerceAtLeast(0))); persist(); return null
    }

    fun clearCalls() { _calls.clear(); persist() }

    // ---- eSIM ----
    fun buyEsim(p: EsimPlan): String? {
        spend(p.credits, "eSIM ${p.country.name} ${p.dataGb} GB / ${p.days} days")?.let { return it }
        _esims.add(0, MyEsim(nextId++, p, clock(), null)); persist(); return null
    }

    /** The user confirms they installed the profile; the validity period starts now. */
    fun markEsimInstalled(id: Int) {
        val i = _esims.indexOfFirst { it.id == id }
        if (i >= 0 && !_esims[i].installed) { _esims[i] = _esims[i].copy(installedAt = clock()); persist() }
    }

    fun removeEsim(id: Int) { if (_esims.removeAll { it.id == id }) persist() }

    // ---- Persistence ----
    private fun snapshot() = AppSnapshot(
        signedIn, displayName, email, credits, themeMode, notifications, messageAlerts, nextId,
        _numbers.toList(), _threads.toList(), _calls.toList(), _esims.toList(), _txns.toList(),
    )

    private fun restore(s: AppSnapshot) {
        signedIn = s.signedIn; displayName = s.name; email = s.email; credits = s.credits
        themeMode = s.themeMode; notifications = s.notifications; messageAlerts = s.messageAlerts; nextId = s.nextId
        _numbers.addAll(s.numbers); _threads.addAll(s.threads); _calls.addAll(s.calls)
        _esims.addAll(s.esims); _txns.addAll(s.txns)
    }

    private fun persist() = storage.save(SnapshotCodec.encode(snapshot()))
}
