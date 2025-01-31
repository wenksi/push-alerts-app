package ch.wenksi.pushalerts.services.auth

import android.app.Application
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ch.wenksi.pushalerts.models.Token
import ch.wenksi.pushalerts.util.Constants
import java.time.Instant
import java.util.*


abstract class SessionManager {
    companion object {
        private var sharedPreferences: SharedPreferences? = null
        private var tokenCached: Token? = null

        /**
         * Initializes the encrypted shared preferences
         */
        fun init(application: Application) {
            val spec = KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
                .build()

            val masterKey = MasterKey.Builder(application)
                .setKeyGenParameterSpec(spec)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                application,
                "encrypted_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        /**
         * Checks if a token exists and if it is not expired
         */
        fun hasValidToken(): Boolean {
            val token = getToken()
            return token != null
                    && token.expiryUtc > Date.from(Instant.now())
        }

        /**
         * Returns the session token, throws error if none exists.
         */
        fun requireToken(): Token {
            if (tokenCached != null) return tokenCached!!

            val value = decrypt(Constants.PREFS_TOKEN_VALUE)
            val expiry = decrypt(Constants.PREFS_TOKEN_EXPIRY)
            val email = decrypt(Constants.PREFS_TOKEN_EMAIL)
            val uuid = decrypt(Constants.PREFS_TOKEN_UUID)

            if (value.isNullOrEmpty() || expiry.isNullOrEmpty()
                || email.isNullOrEmpty() || uuid.isNullOrEmpty()
            ) {
                throw Error("No token found")
            }
            tokenCached = Token(value, Date(expiry), email, uuid)
            return tokenCached!!
        }

        /**
         * Encrypts and stores the given API token.
         */
        fun storeToken(token: Token) {
            encrypt(Constants.PREFS_TOKEN_VALUE, token.value)
            encrypt(Constants.PREFS_TOKEN_EXPIRY, token.expiryUtc.toString())
            encrypt(Constants.PREFS_TOKEN_EMAIL, token.email)
            encrypt(Constants.PREFS_TOKEN_UUID, token.uuid)
        }

        /**
         * Empties the local storage aka. EncryptedSharedPreferences.
         */
        fun clear() {
            tokenCached = null
            sharedPreferences!!.edit().clear().apply()
        }

        /**
         * Decrypts and returns the session token
         */
        private fun getToken(): Token? {
            if (tokenCached == null) {
                val value = decrypt(Constants.PREFS_TOKEN_VALUE)
                val expiry = decrypt(Constants.PREFS_TOKEN_EXPIRY)
                val email = decrypt(Constants.PREFS_TOKEN_EMAIL)
                val uuid = decrypt(Constants.PREFS_TOKEN_UUID)

                if (value.isNullOrEmpty() || expiry.isNullOrEmpty()
                    || email.isNullOrEmpty() || uuid.isNullOrEmpty()
                ) {
                    Log.i(
                        SessionManager::class.qualifiedName,
                        "Read token from shared preferences - No token available"
                    )
                    return null
                }
                tokenCached = Token(value, Date(expiry), email, uuid)
            }
            Log.i(
                SessionManager::class.qualifiedName,
                "Read token from shared preferences: ${tokenCached.toString()}"
            )
            return tokenCached!!
        }

        private fun encrypt(key: String, value: String) {
            Log.d(
                SessionManager::class.qualifiedName,
                "Encrypt and store to shared preferences {key: $key, value: $value"
            )
            sharedPreferences!!.edit().putString(key, value).apply()
        }

        private fun decrypt(key: String): String? {
            val value = sharedPreferences!!.getString(key, null)
            Log.d(
                SessionManager::class.qualifiedName,
                "Read and decrypt from shared preferences {key: $key, value: $value"
            )
            return value
        }
    }
}