package szofverfejlesztes.mi2048.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, GameStateEntity::class, LeaderboardEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gameStateDao(): GameStateDao
    abstract fun leaderboardDao(): LeaderboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mi2048_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
