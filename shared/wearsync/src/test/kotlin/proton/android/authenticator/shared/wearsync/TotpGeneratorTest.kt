package proton.android.authenticator.shared.wearsync

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpGeneratorTest {

    // RFC 6238 appendix B seeds, Base32 encoded.
    private val sha1Secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val sha256Secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA===="
    private val sha512Secret =
        "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA="

    @Test
    fun `matches RFC 6238 SHA1 vectors`() {
        mapOf(
            59L to "94287082",
            1111111109L to "07081804",
            1111111111L to "14050471",
            1234567890L to "89005924",
            2000000000L to "69279037",
            20000000000L to "65353130"
        ).forEach { (time, expected) ->
            assertEquals(expected, TotpGenerator.generate(entry(sha1Secret, WearEntryAlgorithm.SHA1, 8), time))
        }
    }

    @Test
    fun `matches RFC 6238 SHA256 and SHA512 vectors`() {
        assertEquals(
            "46119246",
            TotpGenerator.generate(entry(sha256Secret, WearEntryAlgorithm.SHA256, 8), 59L)
        )
        assertEquals(
            "90693936",
            TotpGenerator.generate(entry(sha512Secret, WearEntryAlgorithm.SHA512, 8), 59L)
        )
    }

    @Test
    fun `pads six digit codes`() {
        assertEquals("287082", TotpGenerator.generate(entry(sha1Secret, WearEntryAlgorithm.SHA1, 6), 59L))
        assertEquals("081804", TotpGenerator.generate(entry(sha1Secret, WearEntryAlgorithm.SHA1, 6), 1111111109L))
    }

    @Test
    fun `steam codes use the steam alphabet`() {
        val code = TotpGenerator.generate(
            entry(sha1Secret, WearEntryAlgorithm.SHA1, 5, type = WearEntryType.STEAM),
            59L
        )

        assertEquals(5, code.length)
        assertTrue(code.all { it in "23456789BCDFGHJKMNPQRTVWXY" })
    }

    @Test
    fun `computes remaining seconds`() {
        val entry = entry(sha1Secret, WearEntryAlgorithm.SHA1, 6)

        assertEquals(30, TotpGenerator.secondsRemaining(entry, 60L))
        assertEquals(1, TotpGenerator.secondsRemaining(entry, 59L))
    }

    @Test
    fun `base32 is lenient`() {
        assertArrayEquals(
            "12345678901234567890".toByteArray(),
            Base32.decode("gezd gnbv-GY3T QOJQ GEZD GNBV GY3T QOJQ==")
        )
    }

    @Test
    fun `payload round trips and rejects garbage`() {
        val payload = WearEntriesPayload(
            account = "me@proton.me",
            entries = listOf(entry(sha1Secret, WearEntryAlgorithm.SHA1, 6))
        )

        assertEquals(payload, WearSyncCodec.decode(WearSyncCodec.encode(payload)))
        assertNull(WearSyncCodec.decode("not json".toByteArray()))
        assertNull(WearSyncCodec.decode("""{"version":99,"entries":[]}""".toByteArray()))
    }

    private fun entry(
        secret: String,
        algorithm: WearEntryAlgorithm,
        digits: Int,
        type: WearEntryType = WearEntryType.TOTP
    ) = WearEntry(
        id = "id",
        name = "name",
        issuer = "issuer",
        secret = secret,
        period = 30,
        digits = digits,
        algorithm = algorithm,
        type = type,
        position = 0
    )

}
