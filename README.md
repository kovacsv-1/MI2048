# MI2048 – 2048 Android Alkalmazás

A **MI2048** egy natív Androidos 2048 játék, amely a *Szoftverfejlesztes MI támogatással* tantárgy keretében készült el. Az alkalmazást az MI ágens támogatásával (**Gemini 3.6 Flash**) fejlesztettük az 1. beadási mérföldkőnek ("Első működő verzió") megfelelően.

---

## Főbb Funkciók
1. **Felhasználókezelés**: Regisztráció és bejelentkezés helyi Room adatbázisban tárolt, SHA-256-tal hash-elt jelszavakkal. Hibás bemenetek és duplikált felhasználónevek kezelése hibaüzenetekkel.
2. **Vendég Mód & Állapotmegőrzés**: Játék lehetőség regisztráció nélkül. Az alkalmazás háttérbe helyezésekor (Home gomb megnyomása) a félbehagyott vendég játék állapota perzisztensen megőrződik `SharedPreferences` segítségével és visszatéréskor folytatható.
3. **2048 Játéklogika Engine**: 4x4-es tábla, swipe gesztus érzékelés, összevonási szabályok (szigorú `2+2+2+2 -> 4+4` szabály), pontszámítás, véletlenszerű csempegenerálás (90% 2, 10% 4), játék vége detektálás. Érvénytelen swipe esetén a tábla nem változik.
4. **Csempecsúszási & Kombinálódási Animációk**: Jetpack Compose `animateDpAsState` (150 ms tween) eltolással mindkét összeadódó csempe elcsúszik a célpozícióra `(row, col)`, majd fúziós animációval adja át a helyét az új csempének.
5. **Automatikus Állapotmentés (Perzisztencia)**: Bejelentkezett felhasználó esetén minden érvényes lépés után automatikus mentés a Room adatbázisba. Újraindításkor a legutóbb elmentett játékállapot töltődik be.
6. **Toplista (Leaderboard)**: Játék vége után bejelentkezett felhasználó pontszáma mentésre kerül. A toplista megtekinthető csökkenő sorrendben.

---

## Indítási és Fordítási Útmutató (Build & Run)

### Előfeltételek
- Android Studio Ladybug / ME 2026.1 vagy újabb
- JDK 17
- Android SDK 35/37

### Alkalmazás fordítása parancssorból
```bash
# Debug APK fordítása
./gradlew assembleDebug
```

### Tesztek futtatása
```bash
# Unit tesztek futtatása (11/11 sikeres teszt)
./gradlew test
```

---

## Dokumentációk
- Specifikáció: [`SzoftverfejlesztesMIvel-Specifikacio-F07YSM.md`](SzoftverfejlesztesMIvel-Specifikacio-F07YSM.md)
- AI Ágens Útmutató: [`AGENTS.md`](AGENTS.md)
- MI Használat és Fejlesztési Folyamat: [`AI_DEVELOPMENT.md`](AI_DEVELOPMENT.md)
