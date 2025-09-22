# 🇷🇺 Russkaya Familie Discord Bot

Ein Discord Bot für die Russkaya Familie auf GrandRP DE1 Server. Der Bot verwaltet Pflanzen und Solarpanels mit automatischen Erinnerungen.

## ✨ Features

### 🌱 Pflanzen-System
- **Säen tracken** - Wer hat wo eine Pflanze gesät
- **Düngen verfolgen** - Automatische Erinnerungen zum Düngen
- **Ernten loggen** - Wer hat geerntet und in welches Auto
- **Timer-System** - 4 Stunden Wachstumszeit mit Benachrichtigungen

### ☀️ Solarpanel-System  
- **Aufstellung tracken** - Wer hat wo ein Panel aufgestellt
- **Reparatur-Erinnerungen** - Alle 30 Minuten Benachrichtigung
- **Batterie-System** - Nach 2 Stunden Batterie einsammeln
- **Auto-Tracking** - Wohin wurde die Batterie gebracht

### 📋 Management
- **Detaillierte Logs** - Alle Aktivitäten werden gespeichert
- **Status-Übersicht** - Aktuelle Pflanzen und Panels anzeigen
- **Automatische Bereinigung** - Alte Daten werden automatisch gelöscht

## 🚀 Installation & Setup

### 1. Discord Bot erstellen

1. Gehe zu https://discord.com/developers/applications
2. Klicke auf "New Application" 
3. Gib einen Namen ein (z.B. "Russkaya Bot")
4. Gehe zu "Bot" im Seitenmenü
5. Klicke "Add Bot"
6. **WICHTIG:** Kopiere den Bot Token (wird später benötigt!)

### 2. Bot Permissions einstellen

Unter "OAuth2 > URL Generator":
- Scope: `bot` und `applications.commands`
- Bot Permissions:
  - Send Messages
  - Use Slash Commands
  - Read Message History
  - Add Reactions
  - Embed Links

### 3. Channel IDs herausfinden

1. In Discord: Gehe zu Benutzereinstellungen > Erweitert > Entwicklermodus aktivieren
2. Rechtsklick auf den Pflanzen-Channel > "ID kopieren"
3. Rechtsklick auf den Solar-Channel > "ID kopieren"

### 4. Railway Deployment

1. **Repository erstellen:**
   ```bash
   git clone https://github.com/dein-username/russkaya-bot.git
   cd russkaya-bot
   git add .
   git commit -m "Initial commit"
   git push origin main
   ```

2. **Railway Account erstellen:**
   - Gehe zu https://railway.app
   - Melde dich mit GitHub an

3. **Projekt deployen:**
   - Klicke "New Project"
   - Wähle "Deploy from GitHub repo"
   - Wähle dein Repository aus

4. **Umgebungsvariablen setzen:**
   
   In Railway unter "Variables":
   ```
   BOT_TOKEN = dein_bot_token_hier
   DATABASE_URL = file:data/bot.db
   PLANT_CHANNEL_ID = deine_pflanzen_channel_id
   SOLAR_CHANNEL_ID = deine_solar_channel_id
   ```

5. **Deploy starten:**
   - Railway deployed automatisch
   - Schaue in die Logs ob alles funktioniert

## 🎮 Commands

### 🌱 Pflanzen Commands
```
/pflanze-säen location:[Standort] - Eine neue Pflanze säen
/pflanze-düngen id:[Pflanzen-ID] - Eine Pflanze düngen  
/pflanze-ernten id:[Pflanzen-ID] car:[Auto] - Eine Pflanze ernten
/pflanzen-status - Alle aktiven Pflanzen anzeigen
```

### ☀️ Solar Commands
```
/solar-aufstellen location:[Standort] - Ein Solarpanel aufstellen
/solar-sammeln id:[Panel-ID] car:[Auto] - Batterie einsammeln
/solar-status - Alle aktiven Solarpanels anzeigen
```

### 📋 Allgemeine Commands
```
/logs anzahl:[Anzahl] - Letzte Aktivitäten anzeigen (Standard: 10)
```

## ⏰ Timer-System

### Pflanzen (4 Stunden Wachstumszeit):
- **Minute 35**: Erste Dünger-Erinnerung
- **Minute 55**: Zweite Dünger-Erinnerung  
- **Nach 4 Stunden**: Pflanze ist erntereif

### Solarpanels (2 Stunden Batteriezeit):
- **Minute 30**: Erste Reparatur-Erinnerung
- **Minute 50**: Zweite Reparatur-Erinnerung
- **Alle 2 Stunden**: Batterie einsammeln möglich

## 🔧 Lokale Entwicklung

### Voraussetzungen:
- Java 17 oder höher
- Maven 3.6+
- Git

### Setup:
```bash
# Repository klonen
git clone https://github.com/dein-username/russkaya-bot.git
cd russkaya-bot

# Dependencies installieren
mvn clean install

# Bot kompilieren
mvn package

# Bot lokal starten (mit Umgebungsvariablen)
export BOT_TOKEN=dein_token_hier
export DATABASE_URL=file:local.db
export PLANT_CHANNEL_ID=deine_channel_id
export SOLAR_CHANNEL_ID=deine_channel_id
java -jar target/discord-bot-1.0.0-jar-with-dependencies.jar
```

## 📁 Projektstruktur

```
russkaya-bot/
├── src/main/java/de/russkaya/bot/
│   └── RusskayaBot.java          # Hauptklasse
├── Dockerfile                    # Docker Konfiguration
├── railway.toml                  # Railway Deployment Config
├── pom.xml                      # Maven Dependencies
├── README.md                    # Diese Anleitung
└── .gitignore                   # Git Ignore Datei
```

## 🗄️ Datenbank Schema

### Tabelle: plants
```sql
id INTEGER PRIMARY KEY AUTOINCREMENT
user_id TEXT NOT NULL
username TEXT NOT NULL  
planted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
location TEXT NOT NULL
status TEXT DEFAULT 'planted'
fertilized_by TEXT
fertilized_at TIMESTAMP
harvested_by TEXT
harvested_at TIMESTAMP
car_stored TEXT
reminder_message_id TEXT
```

### Tabelle: solar_panels
```sql
id INTEGER PRIMARY KEY AUTOINCREMENT
user_id TEXT NOT NULL
username TEXT NOT NULL
placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
location TEXT NOT NULL
status TEXT DEFAULT 'active'
collected_by TEXT
collected_at TIMESTAMP
car_stored TEXT
reminder_message_id TEXT
```

## 🛠️ Troubleshooting

### Bot startet nicht:
- Überprüfe den Bot Token
- Stelle sicher, dass alle Umgebungsvariablen gesetzt sind
- Schaue in die Railway Logs

### Commands funktionieren nicht:
- Bot muss die nötigen Permissions haben
- Slash Commands brauchen Zeit zum Registrieren (bis zu 1 Stunde)

### Erinnerungen kommen nicht:
- Überprüfe die Channel IDs
- Bot muss Nachrichten senden können

### Datenbank Probleme:
- Railway erstellt automatisch ein Volume für die SQLite Datenbank
- Bei Problemen: Service neu deployen

## 📝 Changelog

### Version 1.0.0
- ✅ Grundlegendes Pflanzen-System
- ✅ Solarpanel-Management
- ✅ Automatische Erinnerungen
- ✅ Detaillierte Logs
- ✅ Railway Deployment ready

## 🤝 Support

Bei Problemen oder Fragen:
1. Schaue in die Railway Logs
2. Überprüfe die Umgebungsvariablen
3. Stelle sicher, dass der Bot die nötigen Permissions hat

## 📜 Lizenz

Dieses Projekt ist für die private Nutzung der Russkaya Familie erstellt.

---

**Развивайся с семьёй Русская! 🇷🇺**
