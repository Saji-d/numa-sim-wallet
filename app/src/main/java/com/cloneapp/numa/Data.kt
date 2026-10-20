package com.cloneapp.numa

// ---- Domain models ----
data class Country(val code: String, val name: String, val dial: String, val pricePerMonth: Int, val flag: String)

data class VirtualNumber(
    val id: Int,
    val e164: String,
    val country: Country,
    val label: String,
    val purchasedAt: Long,
    val expiresAt: Long,
)

data class Message(val fromMe: Boolean, val text: String, val at: Long)

data class Thread(val id: Int, val peer: String, val numberId: Int, val messages: List<Message>, val unread: Boolean) {
    val last: Message? get() = messages.lastOrNull()
}

enum class CallDir { IN, OUT, MISSED }
data class CallEntry(val peer: String, val dir: CallDir, val at: Long, val seconds: Int)

data class EsimPlan(val id: String, val country: Country, val dataGb: Int, val days: Int, val priceUsd: Double) {
    /** Price in wallet credits (1 credit = 1 USD, rounded). */
    val credits: Int get() = Math.round(priceUsd).toInt()
}

/** A purchased plan. Validity starts when the user marks it installed. */
data class MyEsim(val id: Int, val plan: EsimPlan, val purchasedAt: Long, val installedAt: Long?) {
    val installed: Boolean get() = installedAt != null
    val expiresAt: Long? get() = installedAt?.let { it + plan.days * DAY_MS }
}

/** Wallet ledger entry. [amount] is in credits; negative = spend. */
data class Txn(val title: String, val amount: Int, val at: Long)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

const val DAY_MS = 24L * 60 * 60 * 1000
const val NUMBER_TERM_DAYS = 30
const val STARTER_CREDITS = 10
const val MAX_TOP_UP = 500
const val MAX_MESSAGE_LENGTH = 1000

// ---- Catalogue (static reference data bundled with the app) ----
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

/** Quick-add amounts shown in the wallet (credits). */
val CREDIT_PACKS = listOf(10, 25, 60)

fun countryByCode(code: String): Country? = COUNTRIES.firstOrNull { it.code == code }
fun planById(id: String): EsimPlan? = ESIM_PLANS.firstOrNull { it.id == id }
