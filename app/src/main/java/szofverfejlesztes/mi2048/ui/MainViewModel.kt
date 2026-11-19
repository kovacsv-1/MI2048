package szofverfejlesztes.mi2048.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import szofverfejlesztes.mi2048.data.AppDatabase
import szofverfejlesztes.mi2048.data.GameStateEntity
import szofverfejlesztes.mi2048.data.GuestPreferences
import szofverfejlesztes.mi2048.data.LeaderboardEntity
import szofverfejlesztes.mi2048.data.PasswordHasher
import szofverfejlesztes.mi2048.data.UserEntity
import szofverfejlesztes.mi2048.game.Direction
import szofverfejlesztes.mi2048.game.GameEngine
import szofverfejlesztes.mi2048.game.TileData

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val gameStateDao = db.gameStateDao()
    private val leaderboardDao = db.leaderboardDao()
    private val guestPrefs = GuestPreferences(application)

    val gameEngine = GameEngine()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _board = MutableStateFlow<List<Int>>(List(16) { 0 })
    val board: StateFlow<List<Int>> = _board.asStateFlow()

    private val _activeTiles = MutableStateFlow<List<TileData>>(emptyList())
    val activeTiles: StateFlow<List<TileData>> = _activeTiles.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val topScores: StateFlow<List<LeaderboardEntity>> = leaderboardDao.getTopScores(50)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        gameEngine.resetGame()
        syncEngineState()
    }

    private fun syncEngineState() {
        _board.value = gameEngine.board
        _activeTiles.value = gameEngine.activeTiles
        _score.value = gameEngine.score
        _isGameOver.value = gameEngine.isGameOver
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun register(usernameInput: String, passwordInput: String, onSuccess: () -> Unit) {
        val username = usernameInput.trim()
        val password = passwordInput.trim()

        if (username.isBlank()) {
            _errorMessage.value = "A felhasználónév nem lehet üres."
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "A jelszó nem lehet üres."
            return
        }

        viewModelScope.launch {
            val existingUser = userDao.getUserByUsername(username)
            if (existingUser != null) {
                _errorMessage.value = "A felhasználónév már létezik."
                return@launch
            }

            val passwordHash = PasswordHasher.hashPassword(password)
            val newUser = UserEntity(username = username, passwordHash = passwordHash)
            val userId = userDao.insertUser(newUser).toInt()
            val userWithId = newUser.copy(userId = userId)

            _currentUser.value = userWithId
            _isGuest.value = false
            _errorMessage.value = null

            // Start clean game for newly registered user
            gameEngine.resetGame()
            syncEngineState()
            saveCurrentGameState()

            onSuccess()
        }
    }

    fun login(usernameInput: String, passwordInput: String, onSuccess: () -> Unit) {
        val username = usernameInput.trim()
        val password = passwordInput.trim()

        if (username.isBlank()) {
            _errorMessage.value = "A felhasználónév nem lehet üres."
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "A jelszó nem lehet üres."
            return
        }

        viewModelScope.launch {
            val user = userDao.getUserByUsername(username)
            val inputHash = PasswordHasher.hashPassword(password)

            if (user == null || user.passwordHash != inputHash) {
                _errorMessage.value = "Hibás felhasználónév vagy jelszó."
                return@launch
            }

            _currentUser.value = user
            _isGuest.value = false
            _errorMessage.value = null

            // Restore game state if available in Room
            val savedState = gameStateDao.getGameState(user.userId)
            if (savedState != null) {
                val boardList = savedState.boardState.split(",").mapNotNull { it.toIntOrNull() }
                if (boardList.size == 16) {
                    gameEngine.loadGame(boardList, savedState.score, savedState.isGameOver)
                    syncEngineState()
                } else {
                    gameEngine.resetGame()
                    syncEngineState()
                    saveCurrentGameState()
                }
            } else {
                gameEngine.resetGame()
                syncEngineState()
                saveCurrentGameState()
            }

            onSuccess()
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        _currentUser.value = null
        _isGuest.value = true
        _errorMessage.value = null

        if (guestPrefs.hasSavedGuestGame()) {
            val guestBoardStr = guestPrefs.getGuestBoard()
            val boardList = guestBoardStr?.split(",")?.mapNotNull { it.toIntOrNull() }
            if (boardList != null && boardList.size == 16) {
                gameEngine.loadGame(boardList, guestPrefs.getGuestScore(), guestPrefs.getGuestIsGameOver())
                syncEngineState()
            } else {
                gameEngine.resetGame()
                syncEngineState()
                saveCurrentGameState()
            }
        } else {
            gameEngine.resetGame()
            syncEngineState()
            saveCurrentGameState()
        }

        onSuccess()
    }

    fun logout() {
        _currentUser.value = null
        _isGuest.value = false
        _errorMessage.value = null
        gameEngine.resetGame()
        syncEngineState()
    }

    fun move(direction: Direction) {
        val moveResult = gameEngine.move(direction)
        syncEngineState()

        if (moveResult.moved) {
            saveCurrentGameState()

            val user = _currentUser.value
            if (user != null && gameEngine.isGameOver) {
                saveLeaderboardEntry(user.userId, user.username, gameEngine.score)
            }
        }
    }

    fun restartGame() {
        gameEngine.resetGame()
        syncEngineState()
        saveCurrentGameState()
    }

    private fun saveCurrentGameState() {
        val user = _currentUser.value
        val boardStr = gameEngine.board.joinToString(",")

        if (user != null) {
            viewModelScope.launch {
                val entity = GameStateEntity(
                    userId = user.userId,
                    boardState = boardStr,
                    score = gameEngine.score,
                    isGameOver = gameEngine.isGameOver
                )
                gameStateDao.saveGameState(entity)
            }
        } else if (_isGuest.value) {
            guestPrefs.saveGuestGameState(boardStr, gameEngine.score, gameEngine.isGameOver)
        }
    }

    private fun saveLeaderboardEntry(userId: Int, username: String, score: Int) {
        viewModelScope.launch {
            val entry = LeaderboardEntity(
                userId = userId,
                username = username,
                score = score
            )
            leaderboardDao.insertScore(entry)
        }
    }
}
