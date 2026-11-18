# Házi feladat specifikáció – 2048 Android-alkalmazás

## Cél
Androidos 2048 játék, amelyben a felhasználó regisztrálhat, bejelentkezhet, játszhat. Bejelentkezve az állapot automatikusan mentődik, és a játék vége után a pontszám toplistára kerül. Az adatkezelést és a perzisztenciát a Room adatbázis látja el.

## Architektúra és indoklás
Az alkalmazás natív Android (Kotlin). A backend funkciókat – tartós adattárolás, felhasználók, mentett játékállapot, toplista – a Room adatbázis biztosítja DAO-kon keresztül. Külön szerver nem készül: az app önmagában arányosan komplex üzleti logikát (mozgatás, összevonás, pontszámítás, játék vége detektálás) és perzisztens adatkezelést tartalmaz, így a tantárgyi követelmények szerinti "arányosan komplex" kivétel teljesül. ("Lehetőség van backend/frontend nélküli alkalmazások beadására is, amennyiben a
megmaradó oldal arányosan komplex (pl. mobil alkalmazás)." - Házi feladat követelmények; Technológia bekezdés)

## Szerepkörök
- **Játékos**: regisztrál, bejelentkezik, játszik, mentés/betöltés automatikus, toplistát tekint.
- **Vendég**: játszik regisztráció nélkül, nem kerül toplistára, mentés/betöltés nincs, toplistát tekint.
- **Admin**: nincs külön admin szerepkör; a toplista automatikusan épül.

## Forgatókönyvek
1. Regisztráció felhasználónévvel és jelszóval; bejelentkezés.
2. Új játék indítása: 4x4 tábla, két véletlen csempe (2 vagy 4), bejelentkezve vagy vendégként.
3. Mozgatás swipe gesztussal; a logika összevonja a csempéket, pontszámot növel, új csempét generál.
4. Minden érvényes lépés után a bejelentkezett felhasználó állapota automatikusan mentődik Roomba.
5. Játék vége: teli tábla és nincs érvényes lépés; bejelentkezett felhasználó esetén a pontszám bekerül a toplistára.
6. Toplista megtekintése csökkenő pontszám szerint.

## Elfogadási feltételek
1. **Normál**: 4x4 táblán egy érvényes mozgatás után a csempék a 2048 szabályai szerint összevonódnak, a pontszám az összevont értékekkel nő.
2. **Normál**: minden érvényes mozgatás után egy új csempe (2 vagy 4) jelenik meg egy üres helyen, a tábla mérete nem változik.
3. **Határeset**: teli táblánál, ha nincs érvényes mozgatás, a játék véget ér, és bejelentkezett felhasználó esetén a pontszám bekerül a toplistára.
4. **Határeset**: egy lépésben egy csempe csak egyszer vonható össze (pl. 2+2+2+2 → 4+4, nem 8); a pontszám ennek megfelelően nő.
5. **Hibás bemenet**: üres vagy már létező felhasználónévvel történő regisztráció, illetve hibás jelszóval történő bejelentkezés hibaüzenetet ad, és nem hoz létre új felhasználót / nem enged be.
6. **Hibás bemenet**: érvénytelen swipe (pl. átlós irány) esetén a tábla nem változik, a pontszám nem nő.
7. **Perzisztencia**: az app újraindítása után a bejelentkezett felhasználó legutóbbi mentett játékállapota betöltődik a Roomból, és azonos állapotot ad vissza.
8. **Toplista**: játék vége után a bejelentkezett felhasználó pontszáma megjelenik a toplistán, a sorrend csökkenő pontszám szerint helyes; azonos pontszámnál a korábbi bejegyzés előrébb van.

## Bizonytalanságok és döntések
- **Külön backend?** Döntés: nincs; a Room tölti be a backend szerepét (lásd Architektúra).
- **Felhasználókezelés**: Döntés: helyi, Room-alapú felhasználói tábla (jelszó hash-elve); külső szolgáltatás nem szükséges.
- **Új csempe valószínűsége**: Döntés: 90% 2, 10% 4.
- **Táblaméret**: Döntés: 4x4.
- **Mentés**: Döntés: automatikus minden érvényes lépés után bejelentkezett felhasználó esetén; vendégnél nincs mentés.
- **Toplista**: Döntés: csak regisztrált és bejelentkezett felhasználók pontszámai kerülnek be.

## Tesztelés
A játéklogika (mozgatás, összevonás, játék vége) unit tesztekkel; a Room DAO-k instrumented tesztekkel; a hibás bemenetek (5–6. feltétel) unit- és UI-teszttel. A perzisztenciát (7.) újraindításos instrumented teszt igazolja.

## Változtatási igény
A végleges verzióban az oktató által kiadott kis módosítást a rögzített első verzióból kiindulva valósítom meg, regressziós tesztekkel igazolva a korábbi működés és a tárolt adatok megőrzését.