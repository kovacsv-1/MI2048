package szofverfejlesztes.mi2048.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val username: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
