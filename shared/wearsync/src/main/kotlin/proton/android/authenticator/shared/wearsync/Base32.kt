package proton.android.authenticator.shared.wearsync

import java.io.ByteArrayOutputStream

/** Lenient RFC 4648 Base32 decoder: ignores case, spaces, dashes and padding. */
object Base32 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(input: String): ByteArray {
        val output = ByteArrayOutputStream()
        var buffer = 0
        var bits = 0

        for (char in input.uppercase()) {
            if (char == '=' || char == ' ' || char == '-') continue

            val value = ALPHABET.indexOf(char)
            if (value < 0) continue

            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                bits -= 8
                output.write((buffer shr bits) and 0xFF)
            }
        }

        return output.toByteArray()
    }

}
