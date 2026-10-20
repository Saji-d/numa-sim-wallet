package com.cloneapp.numa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ---- Domain models ----
data class Country(val code: String, val name: String, val dial: String, val pricePerMonth: Int, val flag: String)
data class VirtualNumber(val id: Int, val e164: String, val country: Country, val label: String)
data class Message(val fromMe: Boolean, val text: String, val time: String)
data class Thread(val id: Int, val peer: String, val numberId: Int, val messages: List<Message>, val unread: Boolean)
enum class CallDir { IN, OUT, MISSED }
data class CallEntry(val peer: String, val dir: CallDir, val time: String, val seconds: Int)
data class EsimPlan(val id: String, val country: Country, val dataGb: Int, val days: Int, val priceUsd: Double)
data class MyEsim(val id: Int, val plan: EsimPlan, val status: String, val usedGb: Double)
data class Txn(val title: String, val amount: Int, val time: String) // credits, negative = spend

// ---- Mock catalogue (demo data only) ----
val COUNTRIES = listOf(
    Country("US", "United States", "+1", 5, "🇺🇸"),
    Country("GB", "United Kingdom", "+44", 6, "🇬🇧"),
    Country("CA", "Canada", "+1", 5, "🇨🇦"),
    Country("DE", "Germany", "+49", 7, "🇩🇪"),
    Country("AU", "Australia", "+61", 8, "🇦🇺"),
    Country("BD", "Bangladesh", "+880", 4, "🇧🇩"),
    Country("JP", "Japan", "+81", 9, "🇯🇵"),
    Country("TH", "Thailand", "+66", 6, "🇹🇭"),
)

fun candidateNumbers(c: Country): List<String> =
    listOf("555 0142", "555 0187", "555 0123", "555 0166", "555 0109").map { "${c.dial} $it" }

val ESIM_PLANS: List<EsimPlan> = COUNTRIES.flatMap { c ->
    listOf(1 to 3, 3 to 7, 5 to 15, 10 to 30).mapIndexed { i, (gb, days) ->
        EsimPlan("${c.code}-$gb", c, gb, days, listOf(3.5, 6.0, 9.5, 16.0)[i])
    }
}

/** (credits, display price). Demo only: no payment is taken. */
val CREDIT_PACKS = listOf(10 to "$9.99", 25 to "$19.99", 60 to "$39.99")

/** In-memory MOCK store. Nothing here talks to a network, carrier or payment provider. */
object Store {
    var signedIn by mutableStateOf(false)
    var displayName by mutableStateOf("Guest")
    var email by mutableStateOf("")
    var credits by mutableStateOf(10)
    val numbers = mutableStateListOf<VirtualNumber>()
    val threads = mutableStateListOf<Thread>()
    val calls = mutableStateListOf<CallEntry>()
    val esims = mutableStateListOf<MyEsim>()
    val txns = mutableStateListOf<Txn>()
    private var nextId = 1

    fun signIn(name: String, mail: String) {
        displayName = name.ifBlank { "Guest" }
        email = mail
        signedIn = true
        if (txns.isEmpty()) {
            txns.add(Txn("Welcome bonus (demo)", 10, "Today"))
            calls.addAll(listOf(
                CallEntry("+1 555 0100", CallDir.IN, "Yesterday", 124),
                CallEntry("+44 555 0101", CallDir.MISSED, "Mon", 0),
            ))
        }
    }

    fun signOut() {
        signedIn = false; numbers.clear(); threads.clear(); calls.clear(); esims.clear(); txns.clear(); credits = 10
    }

    /** Returns null on success, else an error message. */
    fun buyNumber(e164: String, c: Country): String? {
        if (numbers.any { it.e164 == e164 }) return "You already own this number."
        if (credits < c.pricePerMonth) return "Not enough credits (need ${c.pricePerMonth}, have $credits)."
        credits -= c.pricePerMonth
        txns.add(0, Txn("Number $e164", -c.pricePerMonth, "now"))
        val n = VirtualNumber(nextId++, e164, c, "Line ${numbers.size + 1}")
        numbers.add(n)
        threads.add(0, Thread(nextId++, "Numa", n.id, listOf(Message(false, "Welcome! $e164 is a demo number — no real SMS.", "now")), true))
        return null
    }

    fun release(n: VirtualNumber) {
        numbers.removeAll { it.id == n.id }
        threads.removeAll { it.numberId == n.id }
    }

    fun addCredits(n: Int) { credits += n; txns.add(0, Txn("Top-up (demo, no payment)", n, "now")) }

    /** Plan price in credits = round(priceUsd). Returns null on success, else an error. */
    fun buyEsim(p: EsimPlan): String? {
        val cost = Math.round(p.priceUsd).toInt()
        if (credits < cost) return "Not enough credits (need $cost, have $credits)."
        credits -= cost
        txns.add(0, Txn("eSIM ${p.country.name} ${p.dataGb}GB", -cost, "now"))
        esims.add(0, MyEsim(nextId++, p, "Ready to install (demo)", 0.0))
        return null
    }

    fun send(threadId: Int, text: String) {
        val i = threads.indexOfFirst { it.id == threadId }
        if (i < 0 || text.isBlank()) return
        val t = threads[i]
        threads[i] = t.copy(messages = t.messages + Message(true, text.trim(), "now"), unread = false)
    }

    fun markRead(threadId: Int) {
        val i = threads.indexOfFirst { it.id == threadId }
        if (i >= 0 && threads[i].unread) threads[i] = threads[i].copy(unread = false)
    }

    fun logCall(peer: String, seconds: Int) { calls.add(0, CallEntry(peer, CallDir.OUT, "now", seconds)) }
}
