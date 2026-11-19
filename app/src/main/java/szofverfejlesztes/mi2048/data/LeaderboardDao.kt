package szofverfejlesztes.mi2048.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {
    @Insert
    suspend fun insertScore(entry: LeaderboardEntity): Long

    @Query("SELECT * FROM leaderboard ORDER BY score DESC, id ASC LIMIT :limit")
    fun getTopScores(limit: Int = 50): Flow<List<LeaderboardEntity>>

    @Query("SELECT * FROM leaderboard ORDER BY score DESC, id ASC LIMIT :limit")
    suspend fun getTopScoresList(limit: Int = 50): List<LeaderboardEntity>
}
