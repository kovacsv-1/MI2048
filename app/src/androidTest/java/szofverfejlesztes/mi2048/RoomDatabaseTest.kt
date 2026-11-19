package szofverfejlesztes.mi2048

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import szofverfejlesztes.mi2048.data.AppDatabase
import szofverfejlesztes.mi2048.data.GameStateEntity
import szofverfejlesztes.mi2048.data.LeaderboardEntity
import szofverfejlesztes.mi2048.data.PasswordHasher
import szofverfejlesztes.mi2048.data.UserEntity

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // Elfogadási feltétel 5: Registráció és bejelentkezés perszisztenciája
    @Test
    fun testUserRegistrationAndLogin() = runBlocking {
        val userDao = db.userDao()
        val username = "testuser"
        val passwordHash = PasswordHasher.hashPassword("secret123")

        val user = UserEntity(username = username, passwordHash = passwordHash)
        val userId = userDao.insertUser(user).toInt()

        val fetchedUser = userDao.getUserByUsername(username)
        assertNotNull(fetchedUser)
        assertEquals(userId, fetchedUser!!.userId)
        assertEquals(passwordHash, fetchedUser.passwordHash)
    }

    // Elfogadási feltétel 7: Perzisztencia – Lépésenkénti mentett játékállapot betöltése
    @Test
    fun testGameStateSaveAndRestore() = runBlocking {
        val gameStateDao = db.gameStateDao()
        val userId = 100
        val boardStateStr = "2,4,8,16,0,0,0,0,0,0,0,0,0,0,0,0"

        val gameState = GameStateEntity(
            userId = userId,
            boardState = boardStateStr,
            score = 128,
            isGameOver = false
        )

        gameStateDao.saveGameState(gameState)

        val restored = gameStateDao.getGameState(userId)
        assertNotNull(restored)
        assertEquals(boardStateStr, restored!!.boardState)
        assertEquals(128, restored.score)

        // Delete game state
        gameStateDao.deleteGameState(userId)
        assertNull(gameStateDao.getGameState(userId))
    }

    // Elfogadási feltétel 8: Toplista – Pontszám mentés és csökkenő sorrend (korábbi bejegyzés előbb)
    @Test
    fun testLeaderboardOrderingAndTieBreaker() = runBlocking {
        val leaderboardDao = db.leaderboardDao()

        val entry1 = LeaderboardEntity(userId = 1, username = "player1", score = 200, timestamp = 1000L)
        val entry2 = LeaderboardEntity(userId = 2, username = "player2", score = 500, timestamp = 2000L)
        val entry3 = LeaderboardEntity(userId = 3, username = "player3", score = 200, timestamp = 1500L)

        leaderboardDao.insertScore(entry1)
        leaderboardDao.insertScore(entry2)
        leaderboardDao.insertScore(entry3)

        val topScores = leaderboardDao.getTopScoresList(10)

        assertEquals(3, topScores.size)
        // 1st place: 500 points
        assertEquals("player2", topScores[0].username)
        assertEquals(500, topScores[0].score)

        // 2nd and 3rd place: tie 200 points -> entry1 (id lower) came earlier
        assertEquals("player1", topScores[1].username)
        assertEquals(200, topScores[1].score)
        assertEquals("player3", topScores[2].username)
        assertEquals(200, topScores[2].score)
    }
}
