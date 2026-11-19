package szofverfejlesztes.mi2048.data

import android.content.Context
import android.content.SharedPreferences

class GuestPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("guest_prefs", Context.MODE_PRIVATE)

    fun saveGuestGameState(boardStr: String, score: Int, isGameOver: Boolean) {
        prefs.edit()
            .putString("guest_board", boardStr)
            .putInt("guest_score", score)
            .putBoolean("guest_is_game_over", isGameOver)
            .putBoolean("has_guest_game", true)
            .apply()
    }

    fun getGuestBoard(): String? = prefs.getString("guest_board", null)
    fun getGuestScore(): Int = prefs.getInt("guest_score", 0)
    fun getGuestIsGameOver(): Boolean = prefs.getBoolean("guest_is_game_over", false)
    fun hasSavedGuestGame(): Boolean = prefs.getBoolean("has_guest_game", false)

    fun clearGuestGameState() {
        prefs.edit().clear().apply()
    }
}
