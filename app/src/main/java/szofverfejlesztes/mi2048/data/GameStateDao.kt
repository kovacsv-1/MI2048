package szofverfejlesztes.mi2048.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameState(gameState: GameStateEntity)

    @Query("SELECT * FROM game_state WHERE userId = :userId LIMIT 1")
    suspend fun getGameState(userId: Int): GameStateEntity?

    @Query("DELETE FROM game_state WHERE userId = :userId")
    suspend fun deleteGameState(userId: Int)
}
