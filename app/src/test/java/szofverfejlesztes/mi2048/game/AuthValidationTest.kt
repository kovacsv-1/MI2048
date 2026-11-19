package szofverfejlesztes.mi2048.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import szofverfejlesztes.mi2048.data.PasswordHasher

class AuthValidationTest {

    @Test
    fun testPasswordHashingSHA256() {
        val rawPassword = "mySecretPassword123"
        val hash1 = PasswordHasher.hashPassword(rawPassword)
        val hash2 = PasswordHasher.hashPassword(rawPassword)

        assertEquals(hash1, hash2)
        assertNotEquals(rawPassword, hash1)
        assertEquals(64, hash1.length) // SHA-256 hex string is 64 characters
    }

    @Test
    fun testDifferentPasswordsProduceDifferentHashes() {
        val hash1 = PasswordHasher.hashPassword("Password123")
        val hash2 = PasswordHasher.hashPassword("Password124")

        assertNotEquals(hash1, hash2)
    }
}
