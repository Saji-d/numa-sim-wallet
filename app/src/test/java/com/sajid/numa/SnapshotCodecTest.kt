package com.sajid.numa

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SnapshotCodecTest {
    private val us = countryByCode("US")!!
    private val gb = countryByCode("GB")!!

    private fun populated() = AppSnapshot(
        signedIn = true,
        name = "Sam O'Neil",
        email = "sam@example.com",
        credits = 37,
        themeMode = ThemeMode.DARK,
        notifications = false,
        messageAlerts = true,
        nextId = 12,
        numbers = listOf(
            VirtualNumber(1, "+1 555 0142", us, "Work", 1_700_000_000_000L, 1_702_592_000_000L),
            VirtualNumber(3, "+44 555 0187", gb, "Line 2", 1_700_000_100_000L, 1_702_592_100_000L),
        ),
        threads = listOf(
            Thread(2, "Numa", 1, listOf(
                Message(false, "Welcome", 1_700_000_000_000L),
                Message(true, "Quotes \"here\", unicode é 日本, and a\nnewline", 1_700_000_050_000L),
            ), unread = true),
            Thread(4, "Numa", 3, emptyList(), unread = false),
        ),
        calls = listOf(
            CallEntry("+1 555 0100", CallDir.OUT, 1_700_000_200_000L, 42),
            CallEntry("+44 555 0101", CallDir.MISSED, 1_700_000_300_000L, 0),
            CallEntry("+1 555 0102", CallDir.IN, 1_700_000_400_000L, 15),
        ),
        esims = listOf(
            MyEsim(5, planById("GB-3")!!, 1_700_000_500_000L, 1_700_000_600_000L),
            MyEsim(6, planById("US-1")!!, 1_700_000_700_000L, null),
        ),
        txns = listOf(
            Txn("eSIM United States 1 GB / 3 days", -4, 1_700_000_700_000L),
            Txn("Starter credits", 10, 1_700_000_000_000L),
        ),
    )

    @Test fun roundTrip_populatedSnapshot() {
        val s = populated()
        assertEquals(s, SnapshotCodec.decode(SnapshotCodec.encode(s)))
    }

    @Test fun roundTrip_preservesNullInstalledAt() {
        val decoded = SnapshotCodec.decode(SnapshotCodec.encode(populated()))!!
        assertNotNull(decoded.esims[0].installedAt)
        assertNull(decoded.esims[1].installedAt)
        assertFalse(decoded.esims[1].installed)
    }

    @Test fun roundTrip_defaultSnapshot() {
        val s = AppSnapshot()
        assertEquals(s, SnapshotCodec.decode(SnapshotCodec.encode(s)))
    }

    @Test fun encode_writesCurrentVersion() {
        val o = JSONObject(SnapshotCodec.encode(AppSnapshot()))
        assertEquals(SnapshotCodec.VERSION, o.getInt("v"))
    }

    @Test fun decode_garbageReturnsNull() {
        assertNull(SnapshotCodec.decode("not json"))
        assertNull(SnapshotCodec.decode(""))
        assertNull(SnapshotCodec.decode("[]"))
    }

    @Test fun decode_missingVersionOrFieldsReturnsNull() {
        assertNull(SnapshotCodec.decode("{}"))
        assertNull(SnapshotCodec.decode("""{"v":1}"""))
    }

    @Test fun decode_unknownVersionReturnsNull() {
        val o = JSONObject(SnapshotCodec.encode(populated())).put("v", SnapshotCodec.VERSION + 1)
        assertNull(SnapshotCodec.decode(o.toString()))
    }

    @Test fun decode_unknownCountryDropsOnlyThatNumber() {
        val o = JSONObject(SnapshotCodec.encode(populated()))
        o.getJSONArray("numbers").getJSONObject(0).put("country", "ZZ")
        val decoded = SnapshotCodec.decode(o.toString())!!
        assertEquals(1, decoded.numbers.size)
        assertEquals("+44 555 0187", decoded.numbers[0].e164)
        assertEquals(gb, decoded.numbers[0].country)
    }

    @Test fun decode_unknownPlanDropsOnlyThatEsim() {
        val o = JSONObject(SnapshotCodec.encode(populated()))
        o.getJSONArray("esims").getJSONObject(0).put("plan", "XX-99")
        val decoded = SnapshotCodec.decode(o.toString())!!
        assertEquals(1, decoded.esims.size)
        assertEquals("US-1", decoded.esims[0].plan.id)
    }

    @Test fun decode_unknownThemeFallsBackToSystem() {
        val o = JSONObject(SnapshotCodec.encode(populated())).put("theme", "NEON")
        assertEquals(ThemeMode.SYSTEM, SnapshotCodec.decode(o.toString())!!.themeMode)
    }

    @Test fun memoryStorage_loadSaveClear() {
        val m = MemoryStorage()
        assertNull(m.load())
        m.save("x")
        assertEquals("x", m.load())
        m.clear()
        assertNull(m.load())
    }

    @Test fun fileStorage_roundTripAndClear() {
        val dir = java.nio.file.Files.createTempDirectory("numa-test").toFile()
        try {
            val f = java.io.File(dir, "state.json")
            val storage = FileStateStorage(f)
            assertNull(storage.load())
            storage.save("one")
            assertEquals("one", storage.load())
            storage.save("two") // overwrites via temp file + rename
            assertEquals("two", storage.load())
            assertFalse(java.io.File(dir, "state.json.tmp").exists())
            storage.clear()
            assertNull(storage.load())
        } finally {
            dir.deleteRecursively()
        }
    }
}
