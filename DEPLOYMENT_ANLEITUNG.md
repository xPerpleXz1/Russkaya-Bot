# 🚀 Schritt-für-Schritt Deployment Anleitung

## Schritt 1: Discord Bot erstellen

### 1.1 Discord Application erstellen
1. Gehe zu https://discord.com/developers/applications
2. Klicke auf **"New Application"**
3. Gib einen Namen ein: `Russkaya Bot`
4. Klicke **"Create"**

### 1.2 Bot erstellen und Token kopieren
1. Klicke links auf **"Bot"**
2. Klicke **"Add Bot"** → **"Yes, do it!"**
3. Unter "Token" klicke **"Copy"** 
4. **⚠️ WICHTIG:** Speichere den Token sicher! Du brauchst ihn später!

### 1.3 Bot Permissions einstellen
1. Gehe zu **"OAuth2"** → **"URL Generator"**
2. Unter **"Scopes"** wähle:
   - ✅ `bot`
   - ✅ `applications.commands`
3. Unter **"Bot Permissions"** wähle:
   - ✅ Send Messages
   - ✅ Use Slash Commands  
   - ✅ Read Message History
   - ✅ Add Reactions
   - ✅ Embed Links
4. Kopiere die generierte URL und öffne sie
5. Wähle deinen Discord Server aus
6. Klicke **"Authorize"**

## Schritt 2: Channel IDs herausfinden

### 2.1 Developer Mode aktivieren
1. Discord öffnen → Benutzereinstellungen (Zahnrad)
2. **"Erweitert"** → **"Entwicklermodus"** aktivieren

### 2.2 Channel IDs kopieren
1. **Pflanzen-Channel:** Rechtsklick → **"ID kopieren"**
2. **Solar-Channel:** Rechtsklick → **"ID kopieren"**
3. **⚠️ Speichere beide IDs!**

## Schritt 3: GitHub Repository erstellen

### 3.1 GitHub Account erstellen (falls nicht vorhanden)
1. Gehe zu https://github.com
2. Klicke **"Sign up"** und erstelle einen Account

### 3.2 Neues Repository erstellen
1. Klicke auf **"+"** (oben rechts) → **"New repository"**
2. Repository Name: `russkaya-bot`
3. Setze auf **"Public"**
4. ✅ **"Add a README file"**
5. Klicke **"Create repository"**

### 3.3 Code hochladen
1. Klicke **"uploading an existing file"**
2. Lade alle Dateien hoch:
   - `RusskayaBot.java` (in Ordner `src/main/java/de/russkaya/bot/`)
   - `pom.xml`
   - `Dockerfile`
   - `railway.toml`
   - `README.md`
   - `.gitignore`
3. Schreibe Commit message: `Initial bot code`
4. Klicke **"Commit changes"**

## Schritt 4: Railway Deployment

### 4.1 Railway Account erstellen
1. Gehe zu https://railway.app
2. Klicke **"Login"** → **"Login with GitHub"**
3. Autorisiere Railway für deinen GitHub Account

### 4.2 Neues Projekt erstellen
1. Klicke **"New Project"**
2. Wähle **"Deploy from GitHub repo"**
3. Wähle dein `russkaya-bot` Repository

### 4.3 Umgebungsvariablen setzen
1. Klicke auf dein Projekt
2. Gehe zu **"Variables"** Tab
3. Füge folgende Variablen hinzu:

| Variable Name | Wert | Beispiel |
|---------------|------|----------|
| `BOT_TOKEN` | Dein Discord Bot Token | `OTxxxxxxxxxxxxxxxxxxxx.Xxxxxx.xxxxxxxxxxxxxxxxxxxxxxxxxxxx` |
| `DATABASE_URL` | `file:data/bot.db` | `file:data/bot.db` |
| `PLANT_CHANNEL_ID` | Pflanzen Channel ID | `123456789012345678` |
| `SOLAR_CHANNEL_ID` | Solar Channel ID | `123456789012345678` |

⚠️ **Wichtig:** Klicke nach jeder Variable auf **"Add"**!

### 4.4 Deployment starten
1. Railway startet automatisch das Deployment
2. Warte bis Status **"Success"** anzeigt (kann 3-5 Minuten dauern)
3. Überprüfe die **"Deployments"** Logs auf Fehlermeldungen

## Schritt 5: Bot testen

### 5.1 Bot Status prüfen
1. Schaue in deinem Discord Server
2. Der Bot sollte als **"Online"** angezeigt werden
3. Aktivität sollte **"Watching Russkaya Familie 🇷🇺"** sein

### 5.2 Commands testen
Probiere diese Commands aus:

```
/pflanze-säen location:Testfeld
/pflanzen-status
/solar-aufstellen location:Dach
/solar-status
/logs anzahl:5
```

## Schritt 6: Channels konfigurieren

### 6.1 Dedicated Channels erstellen (empfohlen)
1. Erstelle einen Channel: `🌱-pflanzen`
2. Erstelle einen Channel: `☀️-solar`
3. Kopiere die neuen Channel IDs
4. Update die Umgebungsvariablen in Railway

### 6.2 Permissions setzen
Stelle sicher, dass der Bot in beiden Channels kann:
- ✅ Nachrichten lesen
- ✅ Nachrichten senden
- ✅ Reaktionen hinzufügen
- ✅ Slash Commands verwenden

## Troubleshooting 🔧

### Problem: Bot ist offline
**Lösung:**
1. Überprüfe Railway Logs: `Deployments` → `View Logs`
2. Überprüfe Bot Token in Variables
3. Prüfe ob alle Umgebungsvariablen gesetzt sind

### Problem: Commands funktionieren nicht
**Lösung:**
1. Warte 1 Stunde (Slash Commands brauchen Zeit)
2. Prüfe Bot Permissions im Discord Server
3. Teste `/` in einem Channel wo der Bot ist

### Problem: Erinnerungen kommen nicht
**Lösung:**
1. Prüfe Channel IDs in Railway Variables
2. Stelle sicher der Bot kann in die Channels schreiben
3. Teste mit `/pflanze-säen` und warte 35 Minuten

### Problem: "Database error"
**Lösung:**
1. Railway erstellt automatisch Speicher für SQLite
2. Bei anhaltenden Problemen: Service in Railway neu deployen
3. Überprüfe `DATABASE_URL` Variable

## Wartung 🛠️

### Logs einsehen
1. Railway → Dein Projekt → `Deployments` → `View Logs`
2. Hier siehst du alle Bot-Aktivitäten

### Updates deployen
1. Ändere Code in GitHub
2. Railway deployed automatisch neu
3. Warte bis Deployment abgeschlossen

### Datenbank verwalten
- SQLite Datenbank läuft automatisch
- Alte Daten werden nach 7 Tagen automatisch gelöscht
- Keine manuelle Wartung nötig

---

## ✅ Checkliste für erfolgreiche Installation

- [ ] Discord Bot erstellt und Token kopiert
- [ ] Bot zu Discord Server hinzugefügt mit korrekten Permissions
- [ ] Channel IDs kopiert (Pflanzen + Solar)
- [ ] GitHub Repository erstellt
- [ ] Alle Code-Dateien hochgeladen
- [ ] Railway Account erstellt und mit GitHub verbunden
- [ ] Railway Projekt aus GitHub Repository deployed
- [ ] Alle 4 Umgebungsvariablen in Railway gesetzt
- [ ] Deployment erfolgreich (Status: Success)
- [ ] Bot zeigt "Online" in Discord
- [ ] Commands funktionieren (`/pflanze-säen`, `/pflanzen-status`)
- [ ] Erinnerungen werden nach 35 Min gesendet

**Bei erfolgreicher Installation sollte der Bot jetzt vollständig funktionsfähig sein! 🎉**

Viel Spaß mit eurem Russkaya Bot! 🇷🇺
