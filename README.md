# ChunkBug Checker (LabyMod 3, Minecraft 1.8.9)

Bot holt signierte Items aus einer Kiste, wirft sie auf seiner Position und prüft nach X Sekunden,
ob der Trichter sie eingesaugt hat. Zusätzlich: Lampe AN = Chunk ok, Lampe AUS = kaputt.
Bei kaputtem Chunk geht der Bot für X Minuten auf ein anderes GS und kommt zurück.

**Download:** rechts unter *Releases* → neuester Build → `ChunkBugAddon-1.1.0.jar`.

Die .jar nach `.minecraft/LabyMod/addons-1.8/` kopieren.

Einstellungen: LabyMod-Einstellungen → Addons → ChunkBug Checker.

## Lizenz über deinen Discord-Bot

1. `discord-bot/lizenz.js` in den Ordner deines Bots kopieren (discord.js v14).
2. In der Hauptdatei des Bots vor `client.login(...)` einfügen:
   ```js
   require('./lizenz')(client);
   ```
3. Port 3000 auf dem Server freigeben (anderer Port: Umgebungsvariable `LIZENZ_PORT`).
4. In Discord (nur Admins): `/lizenz erstellen`, `/lizenz liste`, `/lizenz loeschen`, `/lizenz reset`.
5. Den Key trägt der Nutzer im Addon-Menü unter „Lizenz-Key“ ein. Beim ersten Benutzen wird er an seinen Minecraft-Account gebunden.

Die Keys liegen in `lizenzen.json` neben `lizenz.js`. Diese Datei gehört nicht ins Repo.
