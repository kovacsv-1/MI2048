package szofverfejlesztes.mi2048.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey
    val userId: Int,
    val boardState: String,
    val score: Int,
    val isGameOver: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
