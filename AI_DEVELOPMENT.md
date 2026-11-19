# AZ MI HASZNÁLATÁNAK ÉS A FEJLESZTÉS FOLYAMATÁNAK DOKUMENTÁCIÓJA

**Alkalmazás**: MI2048 (2048 Android App)  
**Felhasznált MI Modell**: Gemini 3.6 Flash  
**Mérföldkő**: 1. Beadandó - Első működő verzió + Kétfázisú Időzített Animációk & Vendég Állapotmegőrzés  

---

## 1. Az MI Használatának Összefoglalása Fejlesztési Fázisok Szerint

A fejlesztési folyamat során a **Gemini 3.6 Flash** modellt használtuk hat fő fejlesztési fázisban:

### A) Követelményelemzés és Specifikáció Fázis
- **Cél**: A tantárgyi elvárások (`Hazi_feladat_kovetelmenyek.pdf`) és a 2048 játék specifikációjának (`SzoftverfejlesztesMIvel-Specifikacio-F07YSM.md`) tisztázása.
- **MI szerepe**: A követelmények elemzése a "backend / perzisztencia" kérdésben. Az MI segített megerősíteni, hogy mobilalkalmazás esetén a Room helyi adatbázis entitásokkal és DAO-kkal megfelel az "arányosan komplex" kivételnek, így külön Node/Spring backend szerver nélkül is teljesíthetők a követelmények.
- **Eredmény**: 8 számozott, jól ellenőrizhető elfogadási feltétel megfogalmazása (normál működés, határesetek, hibás bemenet kezelése).

### B) Implementációs Fázis
- **Cél**: A natív Kotlin kód elrendezése, Room entitások, 2048 összevonási algoritmus, Jetpack Compose felület és Swipe gesztuskezelés megvalósítása.
- **MI szerepe**: 
  - A 2048 sorösszevonási szabályának pontos algoritmizálása (egy lépésben egy csempe csak egyszer vonható össze: `2+2+2+2 -> 4+4`, nem `8`).
  - Lépésenkénti automatikus mentési logika megtervezése `StateFlow` és Room DAO segítségével.
  - Jetpack Compose Swipe érzékelés megvalósítása `pointerInput` és `detectDragGestures` használatával.

### C) UI Animációs Fázis (Csempecsúszás & Ugrásmentes Összevonás)
- **Cél**: A csempék hirtelen ugrása helyett folytonos elcsúszási és összevonási animációk megvalósítása.
- **MI szerepe**: `TileData` adatosztály és egyedi csempeazonosító (`id: Long`) tervezése a `GameEngine`-ben.

### D) Kétfázisú Időzített Animációk (1. Csúszás -> 2. Eltűnés & Megjelenés)
- **Cél**: Garantálni, hogy eltoláskor az összevonandó csempék (pl. két [2]-es csempe) láthatóan beletolódnak a fúzió helyére (1. fázis: 0-150 ms), és az új összevont [4]-es csempevalamint az új véletlen csempe csak a csúszás befejeződése után (2. fázis: 150 ms után) jelenik meg.
- **MI szerepe**: Kétfázisú `isRevealed` állapotvezérlés és `LaunchedEffect` késleltetési architektúra kifejlesztése Compose-ban.

### E) Vendég Mód Állapotmegőrzés (Home gomb / Háttérbe helyezés)
- **Cél**: Biztosítani, hogy amennyiben a felhasználó Vendég módban nyomja meg a Home gombot (alkalmazás hátterezése `onPause`/`onStop`), a félbehagyott játéka ne vesszen el.
- **MI szerepe**: `GuestPreferences` megtervezése `SharedPreferences` alapon, valamint a `MainViewModel` automatikus mentési és betöltési folyamatainak kiterjesztése vendég játéknál.

### F) Tesztelési és Hibafeltárási Fázis
- **Cél**: Automatisált unit tesztek és szándékosan hibás kódváltozat tesztelése a beadási követelményeknek megfelelően.
- **MI szerepe**:
  - Unit tesztesetek generálása az összes elfogadási feltételre.
  - A szándékosan hibás összevonó algoritmus (dupla összevonást engedő hibás logika) elkészítése a tesztelési bizonyítékhoz.

---

## 2. Részletes Döntési Esetek

### 1. Döntési Eset: Adatarchitektúra és Perzisztencia Választás (Követelményelemzés)
- **Probléma**: A tantárgyi követelmények alapján tisztázni kellett, hogy a 2048 játékhoz szükséges-e külön REST backend szerver, vagy a mobilalkalmazás önmagában Room adatbázissal teljesíti az arányos komplexitást.
- **Eszköz / Modell**: Gemini 3.6 Flash
- **Kontextus & Utasítás**: 
  - *Kontextus*: `Hazi_feladat_kovetelmenyek.pdf` Technológia fejezete.
  - *Prompt*: *"Elemzed a tantárgyi követelményeket! Szükséges-e külön REST API backend a 2048 játékhoz, vagy a Room adatbázis elegendő arányosan komplex perzisztenciaként a mobil appban?"*
- **Kapott Javaslat**: A Room adatbázis használata entitásokkal (`UserEntity`, `GameStateEntity`, `LeaderboardEntity`) teljesen elegendő, amennyiben az alkalmazás tartalmazza a jelszó hash-elést, a lépésenkénti automatikus mentést/betöltést és a toplista kezelést.
- **Saját Döntés és Indoklás**: Elfogadva. A Room-alapú helyi perzisztencia tiszta, függetlenül futtatható és tesztelhető architektúrát eredményez, elkerülve a felesleges hálózati overhead-et.
- **Ellenőrző Kód / Bizonyíték**: [`SzoftverfejlesztesMIvel-Specifikacio-F07YSM.md`](SzoftverfejlesztesMIvel-Specifikacio-F07YSM.md) (Architektúra fejezet) és [`AppDatabase.kt`](app/src/main/java/szofverfejlesztes/mi2048/data/AppDatabase.kt).

---

### 2. Döntési Eset: Szigorú 2048 Összevonási Algoritmus (`2+2+2+2 -> 4+4`) (Implementáció)
- **Probléma**: A 2048 játék alapvető szabálya, hogy egy lépésben egy csempe legfeljebb egyszer vonható össze. Például a `[2, 2, 2, 2]` sorból balra mozdításkor `[4, 4, 0, 0]` keletkezhet, és NEM vonható össze közvetlenül `[8, 0, 0, 0]` értékre egyetlen lépésben.
- **Eszköz / Modell**: Gemini 3.6 Flash
- **Kontextus & Utasítás**:
  - *Prompt*: *"Írj egy Kotlin függvényt egy Int listára, ami megvalósítja a 2048 sorösszevonási logikáját, úgy hogy egy elem egy lépésben csak egyszer vonható össze! Vadd vissza a keletkezett pontszámot is!"*
- **Kapott Javaslat**: A nem-nulla elemeken végighaladó ciklus, ami egyezés esetén `i += 2`-vel ugrik a következő nem összevont elemre.
- **Saját Döntés és Indoklás**: Elfogadva. A kapott javaslat pontosan megakadályozza a láncolt összevonást egy lépésen belül. Beépítettük a `GameEngine.kt` `mergeLine` metódusába.
- **Ellenőrző Kód / Teszt**: [`GameEngine.kt`](app/src/main/java/szofverfejlesztes/mi2048/game/GameEngine.kt) (`mergeLine` metódus) és [`GameEngineTest.kt`](app/src/test/java/szofverfejlesztes/mi2048/game/GameEngineTest.kt) (`testSingleMergeRulePerStep`).

---

### 3. Döntési Eset: Érvénytelen Gesztusok Kiszűrése és Mentési Állapot Kezelése (Tesztelés & Debug)
- **Probléma**: Ha a játékos olyan irányba swipe-ol, ahol egyetlen csempe sem tud elmozdulni vagy összevonódni, nem szabad új csempét generálni, és nem szabad feleslegesen adatbázis mentést végezni.
- **Eszköz / Modell**: Gemini 3.6 Flash
- **Kontextus & Utasítás**:
  - *Prompt*: *"Hogyan detektáljuk tisztán, hogy történt-e érvényes elmozdulás a táblán, és hogyan biztosítsuk, hogy érvénytelen lépésre ne szülessen új csempe és ne fusson le a Room mentés?"*
- **Kapott Javaslat**: Az eltolási művelet után az új táblaállapot összehasonlítása a régi táblaállapittal (`newBoard != board`).
- **Saját Döntés és Indoklás**: Elfogadva. Ezzel a megoldással elkerültük a felesleges I/O műveleteket és a szabálytalan csempegenerálást.
- **Ellenőrző Kód / Teszt**: [`GameEngine.kt`](app/src/main/java/szofverfejlesztes/mi2048/game/GameEngine.kt) (`move` metódus) és [`GameEngineTest.kt`](app/src/test/java/szofverfejlesztes/mi2048/game/GameEngineTest.kt) (`testInvalidSwipeDoesNotChangeBoardOrSpawnTile`).

---

### 4. Döntési Eset: Ugrásmentes Összevonási Animáció és Renderelési Sorrend (UI Animáció)
- **Probléma**: Összevonáskor az új összevont csempe kitakarta a célhelyre elcsúszó forráscsempéket, emiatt úgy tűnt, mintha az animáció átugrotta volna a csúszást.
- **Eszköz / Modell**: Gemini 3.6 Flash
- **Kontextus & Utasítás**:
  - *Prompt*: *"Hogyan biztosítsuk Jetpack Compose-ban, hogy összevonáskor mindkét forráscsempe elcsúszzon a célpozícióra anélkül, hogy az új összevont csempe kitakarná őket az animáció alatt?"*
- **Kapott Javaslat**: Válaszuk szét az `activeTiles` listát forráscsempékre (`!isMerged`) és összevont csempékre (`isMerged`).
- **Saját Döntés és Indoklás**: Elfogadva. A szétválasztás megszünteti a takarási hibát.
- **Ellenőrző Kód / Teszt**: [`GameScreen.kt`](app/src/main/java/szofverfejlesztes/mi2048/ui/GameScreen.kt).

---

### 5. Döntési Eset: Vendég Mód Állapotmegőrzés Hátterezéskor (Home Gomb)
- **Probléma**: Vendég módban az alkalmazás háttérbe helyezésekor (Home gomb megnyomása) a félbehagyott játékállapot ne vesszen el.
- **Eszköz / Modell**: Gemini 3.6 Flash
- **Kontextus & Utasítás**:
  - *Prompt*: *"Hogyan biztosítsuk a vendég mód játékállapotának megőrzését SharedPreferences segítségével az alkalmazás háttérbe kerülésekor?"*
- **Kapott Javaslat**: Létrehozunk egy `GuestPreferences` segédosztályt `SharedPreferences` alapon, amibe a `MainViewModel` automatikusan kimenti minden érvényes vendég lépést.
- **Saját Döntés és Indoklás**: Elfogadva. Így a Vendég játékos a Home gomb megnyomása után is zökkenőmentesen folytathatja a játékát.
- **Ellenőrző Kód / Teszt**: [`GuestPreferences.kt`](app/src/main/java/szofverfejlesztes/mi2048/data/GuestPreferences.kt) és [`MainViewModel.kt`](app/src/main/java/szofverfejlesztes/mi2048/ui/MainViewModel.kt).

---

### 6. Döntési Eset: Kétfázisú Időzített Animációk (1. Csúszás -> 2. Eltűnés & Új elemek felbukkanása)
- **Probléma**: Az új generált csempe (pl. [2]) a swipe pillanatában megjelent az üres helyén, így a csúszó csempék áthaladtak rajta eltolás közben. Emellett az összeadódó csempéknek először át kell csúszniuk a fúzió helyére, és csak a csúszás végén jelenhet meg az új összevont csempe.
- **Eszköz / Modell**: Gemini 3.6 Flash
- **Kontextus & Utasítás**:
  - *Prompt*: *"Hogyan valósítsunk meg kétfázisú animációt Compose-ban, ahol a csúszás alatt (0-150 ms) az új és összevont csempék rejtve maradnak, és csak a csúszás befejeztével (150 ms delay után) bukkannak fel?"*
- **Kapott Javaslat**:
  1. Használjunk egy `isRevealed` állapotváltozót, amely `activeTiles` frissülésekor `false`-ra vált, majd `LaunchedEffect`-ben `delay(150)` után `true`-ra vált.
  2. Amíg `!isRevealed`, addig a csúszó forráscsempék láthatóak (`alpha = 1.0f`), miközben az új/összevont csempék rejtve vannak (`alpha = 0.0f`).
  3. Amint `isRevealed` `true` lesz, a forráscsempék eltűnnek, és az új/összevont csempék felbukkannak (`scale` és `alpha` animációval).
- **Saját Döntés és Indoklás**: Elfogadva. Ez a kétfázisú időzítés biztosítja a 100%-osan pontos 2048 animációs élményt.
- **Ellenőrző Kód / Teszt**: [`GameScreen.kt`](app/src/main/java/szofverfejlesztes/mi2048/ui/GameScreen.kt) (`isRevealed` és `delay(150L)` időzítés).

---

## 3. Követelmények és Pontozási Rendszer Megfelelőségi Audit (1. Beadási Mérföldkő)

A kiadott `Hazi_feladat_kovetelmenyek.pdf` követelményei alapján a 2. Mérföldkő ("Első működő verzió") szempontjai:

| Követelmény Szempont | Státusz | Bizonyíték / Implementáció |
| :--- | :--- | :--- |
| **Fő használati folyamat forráskódja** | TELESÍTVE | Regisztráció, Bejelentkezés, Vendég mód állapottartással, 2048 játék kétfázisú animációkkal, auto-mentés Room-ban, Toplista. |
| **Indítási és fordítási útmutató** | TELJESÍTVE | [`README.md`](README.md) tartalmazza a fordítási (`./gradlew assembleDebug`) és tesztfuttatási lépéseket. |
| **Alaptesztek & Hibás változat bemutatása** | TELJESÍTVE | 11/11 sikeres unit teszt (`./gradlew test`), megőrzött hibás vs. javított kódteszt (`testFlawedEngineFailsSingleMergeRule`). |
| **MI használat dokumentációja & AI elemek** | TELJESÍTVE | [`AGENTS.md`](AGENTS.md), [`AI_DEVELOPMENT.md`](AI_DEVELOPMENT.md) 6 részletes döntési esettel (Gemini 3.6 Flash). |
