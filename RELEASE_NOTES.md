# ChunkBug Checker 1.0.4

LabyMod-3-Addon für Minecraft 1.8.9. Der Bot prüft automatisch, ob ein Chunkbug-Chunk noch funktioniert.

## Installation

1. `ChunkBugAddon-1.0.4.jar` unten unter **Assets** herunterladen.
2. In den Ordner `%appdata%\.minecraft\LabyMod\addons-1.8` kopieren (Ordner anlegen, falls er fehlt).
3. **Alte Versionen (1.0 bis 1.0.3 und 1.1.0) aus dem Ordner löschen**, sonst lädt LabyMod das Addon doppelt.
4. LabyMod 1.8.9 starten → Einstellungen → Addons → **ChunkBug Checker**.

## So funktioniert der Bot

1. Steht er auf seiner Bot-Position und hat kein signiertes Item, holt er sich einen Stack aus der Kiste.
2. Er dreht sich in die gespeicherte Wurfrichtung und wirft ein Item.
3. Er wartet die eingestellte Zeit (Standard 30 Sek.) und achtet darauf, ob die Lampe angeht.
4. Liegt das Item danach noch herum oder ist die Lampe nie angegangen, ist der Chunk **kaputt**. Der Bot geht dann für die Weg-Zeit (Standard 10 Min.) auf ein anderes GS und kommt zurück.
5. Sonst ist der Chunk **OK**, und er wirft das nächste Item.

## Einrichten (einmalig)

1. Den **Zurück-Befehl** selbst eingeben, z. B. `/p h MeinGS`. Der Bot muss genau dort stehen, wo dieser Befehl ihn hinteleportiert.
2. Dort stehen bleiben, **zum Trichter schauen** und `#cb pos` eingeben. Das speichert Position **und** Wurfrichtung.
3. Auf die Lampe schauen und `#cb lampe` eingeben, dann auf die Kiste schauen und `#cb kiste` eingeben.
4. Mit `#cb status` prüfen, dann mit `#cb start` loslegen.

| Befehl | Wirkung |
|---|---|
| `#cb start` / `#cb stop` | Bot an- oder ausschalten |
| `#cb status` | Zustand, Position, Lampe AN/AUS, signiertes Item |
| `#cb pos` | Bot-Position + Wurfrichtung = wo du stehst und hinschaust |
| `#cb lampe` / `#cb kiste` | Koordinaten = Block, auf den du schaust |

Hinweis: Werte aus den `#cb`-Befehlen zeigt das Menü erst nach einem Neustart von Minecraft an. Gespeichert und benutzt werden sie aber sofort. Bis zum Neustart solltest du das jeweilige Feld im Menü nicht anfassen.

## Bugfixes und Änderungen

### 1.0.4
- **Fix: Bot hat in die falsche Richtung geworfen.** Der Server wirft ein Item immer in die Richtung, in die der Spieler schaut. Nach dem Griff in die Kiste hat der Bot aber zur Kiste geschaut, und nach dem Teleport zurück in die Richtung, die der GS-Home vorgibt. Das Item landete dadurch oft nicht im Trichter, und der Chunk wurde fälschlich als kaputt gemeldet. Jetzt speichert `#cb pos` die Wurfrichtung, und der Bot dreht sich vor jedem Wurf wieder genau dorthin.
- **Fix: Wurf kam zu früh beim Server an.** Slot-Wechsel und Drehung waren eventuell noch nicht beim Server, wenn schon geworfen wurde. Jetzt wartet der Bot 0,3 Sek. vor dem Wurf.
- **Fix: Kisten-Spam bei vollem Inventar.** Passte der Stack nicht ins Inventar, hätte der Bot die Kiste alle paar Sekunden neu geöffnet. Jetzt versucht er es höchstens alle 30 Sek.
- Meldung bei leerer Kiste sagt jetzt, wann der nächste Versuch kommt.

### 1.0.3
- **Fix: Bot konnte keine Items aus der Kiste nehmen.** Er hat im ersten Moment zugegriffen, in dem die Kiste aufging. Da hatte der Server den Kisten-Inhalt oft noch gar nicht geschickt, also fand er nichts. Jetzt wartet er vor dem Öffnen, vor dem Nehmen und vor dem Schließen.
- **Neu: „Kisten-Delay (ms)“ im Menü** (Standard 1000 ms, einstellbar 100 bis 10000). Klappt es mit 1000 nicht, auf 2000 oder 3000 stellen.

### 1.0.2
- **Fix: Lampe wurde vor dem Wurf geprüft.** Der Bot hat die Lampe schon kontrolliert, bevor er das Item geworfen hatte. War sie in dem Moment aus, galt der Chunk sofort als kaputt. Jetzt wirft er zuerst und achtet dann während der ganzen Wartezeit darauf, ob die Lampe angeht. Ein kurzes Aufleuchten reicht.

### 1.0.1
- **Neu: Ingame-Befehle** `#cb start`, `stop`, `status`, `pos`, `lampe` und `kiste`. Damit musst du keine Koordinaten mehr abtippen.
- Versionsnummer eingeführt.

### Build und Technik (seit 1.0)
- **Fix: Zahlenfelder im Menü hätten nicht kompiliert.** Die LabyMod-API hat für Zahlenfelder keinen Konstruktor, der automatisch speichert. Die Felder speichern ihre Werte jetzt selbst.
- **Fix: Build auf GitHub ging nicht mehr.** Das alte ForgeGradle 2.1 ist nicht mehr erreichbar. Der Build läuft jetzt mit Loom, und die .jar wird mit Vanilla-Namen gebaut, sonst lädt LabyMod das Addon nicht.
- Jede neue Version wird automatisch gebaut und hier als Release veröffentlicht.

## Bekannte Grenzen
- Die Kiste darf höchstens etwa 4 Blöcke von der Bot-Position entfernt sein.
- Die Lampe muss beim Bot geladen sein. Steht an den Koordinaten keine Lampe, wird die Lampenprüfung übersprungen.
- Ein Anti-Cheat auf dem Server könnte auf das schnelle Drehen und Klicken reagieren.
- Ob Bots auf deinem Server erlaubt sind, prüfst du bitte selbst.
