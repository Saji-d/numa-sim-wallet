package com.cloneapp.numa

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Where the serialized app state lives. Swap for a database-backed or remote implementation later. */
interface StateStorage {
    fun load(): String?
    fun save(json: String)
    fun clear()
}

class MemoryStorage(var value: String? = null) : StateStorage {
    override fun load() = value
    override fun save(json: String) { value = json }
    override fun clear() { value = null }
}

/** Single-file JSON storage in app-private storage; writes are atomic (temp file + rename). */
class FileStateStorage(private val file: File) : StateStorage {
    override fun load(): String? = runCatching { if (file.exists()) file.readText() else null }.getOrNull()

    override fun save(json: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json)
        if (!tmp.renameTo(file)) { file.writeText(json); tmp.delete() }
    }

    override fun clear() { file.delete() }
}

/** Everything that is persisted. Plain data so it can be unit-tested without Android or Compose. */
data class AppSnapshot(
    val signedIn: Boolean = false,
    val name: String = "",
    val email: String = "",
    val credits: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notifications: Boolean = true,
    val messageAlerts: Boolean = true,
    val nextId: Int = 1,
    val numbers: List<VirtualNumber> = emptyList(),
    val threads: List<Thread> = emptyList(),
    val calls: List<CallEntry> = emptyList(),
    val esims: List<MyEsim> = emptyList(),
    val txns: List<Txn> = emptyList(),
)

object SnapshotCodec {
    const val VERSION = 1

    fun encode(s: AppSnapshot): String = JSONObject().apply {
        put("v", VERSION)
        put("signedIn", s.signedIn); put("name", s.name); put("email", s.email)
        put("credits", s.credits); put("theme", s.themeMode.name)
        put("notifications", s.notifications); put("messageAlerts", s.messageAlerts)
        put("nextId", s.nextId)
        put("numbers", JSONArray(s.numbers.map {
            JSONObject().put("id", it.id).put("e164", it.e164).put("country", it.country.code)
                .put("label", it.label).put("purchasedAt", it.purchasedAt).put("expiresAt", it.expiresAt)
        }))
        put("threads", JSONArray(s.threads.map { t ->
            JSONObject().put("id", t.id).put("peer", t.peer).put("numberId", t.numberId).put("unread", t.unread)
                .put("messages", JSONArray(t.messages.map { m ->
                    JSONObject().put("me", m.fromMe).put("text", m.text).put("at", m.at)
                }))
        }))
        put("calls", JSONArray(s.calls.map {
            JSONObject().put("peer", it.peer).put("dir", it.dir.name).put("at", it.at).put("sec", it.seconds)
        }))
        put("esims", JSONArray(s.esims.map {
            JSONObject().put("id", it.id).put("plan", it.plan.id).put("purchasedAt", it.purchasedAt)
                .put("installedAt", it.installedAt ?: JSONObject.NULL)
        }))
        put("txns", JSONArray(s.txns.map {
            JSONObject().put("title", it.title).put("amount", it.amount).put("at", it.at)
        }))
    }.toString()

    /** Returns null when [json] is corrupt or from an unknown future version; callers then start fresh. */
    fun decode(json: String): AppSnapshot? = runCatching {
        val o = JSONObject(json)
        if (o.optInt("v", 0) != VERSION) return null
        fun <T> JSONArray.map2(f: (JSONObject) -> T?): List<T> = (0 until length()).mapNotNull { f(getJSONObject(it)) }
        AppSnapshot(
            signedIn = o.getBoolean("signedIn"), name = o.getString("name"), email = o.getString("email"),
            credits = o.getInt("credits"),
            themeMode = runCatching { ThemeMode.valueOf(o.getString("theme")) }.getOrDefault(ThemeMode.SYSTEM),
            notifications = o.optBoolean("notifications", true), messageAlerts = o.optBoolean("messageAlerts", true),
            nextId = o.getInt("nextId"),
            numbers = o.getJSONArray("numbers").map2 { n ->
                countryByCode(n.getString("country"))?.let { c ->
                    VirtualNumber(n.getInt("id"), n.getString("e164"), c, n.getString("label"), n.getLong("purchasedAt"), n.getLong("expiresAt"))
                }
            },
            threads = o.getJSONArray("threads").map2 { t ->
                Thread(t.getInt("id"), t.getString("peer"), t.getInt("numberId"),
                    t.getJSONArray("messages").map2 { m -> Message(m.getBoolean("me"), m.getString("text"), m.getLong("at")) },
                    t.getBoolean("unread"))
            },
            calls = o.getJSONArray("calls").map2 { c ->
                CallEntry(c.getString("peer"), CallDir.valueOf(c.getString("dir")), c.getLong("at"), c.getInt("sec"))
            },
            esims = o.getJSONArray("esims").map2 { e ->
                planById(e.getString("plan"))?.let { p ->
                    MyEsim(e.getInt("id"), p, e.getLong("purchasedAt"), if (e.isNull("installedAt")) null else e.getLong("installedAt"))
                }
            },
            txns = o.getJSONArray("txns").map2 { x -> Txn(x.getString("title"), x.getInt("amount"), x.getLong("at")) },
        )
    }.getOrNull()
}
