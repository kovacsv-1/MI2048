package szofverfejlesztes.mi2048.data

import java.security.MessageDigest

object PasswordHasher {
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
