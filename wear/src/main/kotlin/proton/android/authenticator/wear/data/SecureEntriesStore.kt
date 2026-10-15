package proton.android.authenticator.wear.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the synced entries encrypted at rest with a non-exportable Android Keystore key. */
internal class SecureEntriesStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    private val tempFile = File(context.filesDir, "$FILE_NAME.tmp")

    @Synchronized
    fun read(): ByteArray? {
        if (!file.exists()) return null

        return try {
            decrypt(file.readBytes())
        } catch (_: GeneralSecurityException) {
            // Key lost or data corrupted: drop it, the phone will send the entries again.
            file.delete()
            null
        } catch (_: IOException) {
            null
        }
    }

    @Synchronized
    fun write(plainBytes: ByteArray) {
        tempFile.writeBytes(encrypt(plainBytes))

        if (!tempFile.renameTo(file)) {
            file.delete()
            tempFile.renameTo(file)
        }
    }

    private fun encrypt(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }

        return cipher.iv + cipher.doFinal(plainBytes)
    }

    private fun decrypt(encryptedBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, encryptedBytes, 0, IV_LENGTH))
        }

        return cipher.doFinal(encryptedBytes, IV_LENGTH, encryptedBytes.size - IV_LENGTH)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val FILE_NAME = "entries.bin"
        private const val IV_LENGTH = 12
        private const val KEY_ALIAS = "authenticator_wear_entries"
        private const val KEY_SIZE_BITS = 256
        private const val TAG_LENGTH_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

    }

}
