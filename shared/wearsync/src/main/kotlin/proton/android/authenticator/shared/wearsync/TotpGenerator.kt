package proton.android.authenticator.shared.wearsync

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** RFC 6238 TOTP (SHA-1/256/512) plus Steam Guard codes, computed fully offline. */
object TotpGenerator {

    private const val STEAM_ALPHABET = "23456789BCDFGHJKMNPQRTVWXY"

    private const val STEAM_CODE_LENGTH = 5

    fun generate(entry: WearEntry, epochSeconds: Long): String {
        val counter = epochSeconds / entry.period.coerceAtLeast(1)
        val hash = hmac(entry, counter)
        val offset = hash.last().toInt() and 0x0F
        val binary = ByteBuffer.wrap(hash, offset, Int.SIZE_BYTES).int and Int.MAX_VALUE

        return when (entry.type) {
            WearEntryType.STEAM -> steamCode(binary)
            WearEntryType.TOTP -> totpCode(binary, entry.digits)
        }
    }

    fun secondsRemaining(entry: WearEntry, epochSeconds: Long): Int {
        val period = entry.period.coerceAtLeast(1)
        return period - (epochSeconds % period).toInt()
    }

    private fun hmac(entry: WearEntry, counter: Long): ByteArray {
        val algorithm = when (entry.algorithm) {
            WearEntryAlgorithm.SHA1 -> "HmacSHA1"
            WearEntryAlgorithm.SHA256 -> "HmacSHA256"
            WearEntryAlgorithm.SHA512 -> "HmacSHA512"
        }
        val key = Base32.decode(entry.secret)
        return Mac.getInstance(algorithm).run {
            init(SecretKeySpec(key.takeIf { it.isNotEmpty() } ?: ByteArray(1), algorithm))
            doFinal(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(counter).array())
        }
    }

    private fun totpCode(binary: Int, digits: Int): String {
        var modulus = 1L
        repeat(digits.coerceIn(1, 10)) { modulus *= 10 }
        return (binary % modulus).toString().padStart(digits, '0')
    }

    private fun steamCode(binary: Int): String {
        var remaining = binary
        return buildString {
            repeat(STEAM_CODE_LENGTH) {
                append(STEAM_ALPHABET[remaining % STEAM_ALPHABET.length])
                remaining /= STEAM_ALPHABET.length
            }
        }
    }

}
