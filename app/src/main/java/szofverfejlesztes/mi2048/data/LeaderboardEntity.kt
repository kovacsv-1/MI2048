package szofverfejlesztes.mi2048.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard")
data class LeaderboardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val username: String,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)
