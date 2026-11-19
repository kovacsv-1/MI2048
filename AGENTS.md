# AI Ágens Útmutató (AGENTS.md) - MI2048

Ez a dokumentum az MI2048 Android-projektben dolgozó mesterséges intelligencia ágensek (Gemini 3.6 Flash) számára határozza meg a fejlesztési elveket, az architektúrát és a kódszervezési szabályokat.

---

## Modellspecifikáció
- **Modell**: Gemini 3.6 Flash
- **Környezet**: Android Studio IDE, Kotlin 2.2.10, Jetpack Compose, Room Database 2.8.5, Gradle 9.4.1.

---

## Architektúra és Architekturális Elvek
1. **MVVM (Model-View-ViewModel)**:
   - **UI Réteg**: Jetpack Compose deklaratív elemek (`AuthScreen`, `GameScreen`, `LeaderboardScreen`). A felület reaktívan figyel a `StateFlow` állapotokra (`activeTiles`, `board`, `score`, `isGameOver`).
   - **ViewModel Réteg**: `MainViewModel` felelős az állapotkezelésért, az állapotáramlásokért (`StateFlow`) és a Room műveletek aszinkron elindításáért (`viewModelScope`).
   - **Üzleti Logika & Animációs Tracking (Game Engine)**: `GameEngine` tiszta Kotlin osztály a 2048 játék szabályaival és csempeazonosító tracking-gel (`TileData: id, value, row, col`).
   - **Adatréteg (Room)**: `AppDatabase`, entitások (`UserEntity`, `GameStateEntity`, `LeaderboardEntity`) és DAO-k (`UserDao`, `GameStateDao`, `LeaderboardDao`).

2. **Perzisztencia és Biztonság**:
   - A felhasználói jelszavak csak SHA-256 hash formájában tárolódnak a Room adatbázisban (`PasswordHasher`).
   - Minden érvényes lépés után bejelentkezett felhasználó esetén az állapot automatikusan mentésre kerül a Room-ba.

3. **UI Animációk (Compose)**:
   - Csempecsúszási animáció: `key(tile.id)` és `animateDpAsState` (150 ms tween) koordináta eltolással.
   - Összevonás / Új csempe skálázás: `animateFloatAsState`.

---

## Kódolási Szabályok Ágenseknek
- **Nyelv**: Kizárólag idiómatikus Kotlin kód írandó.
- **Állapotkezelés**: Compose-ban nem használunk közvetlen mutableState-et üzleti adatokra a composable-ök belsejében; a ViewModel `StateFlow` állapotait kell gyűjteni (`collectAsState()`).
- **Tesztelhetőség**: A `GameEngine` független az Android framework-től és a Compose UI-tól, így gyors JVM Unit tesztekkel futtatható.
