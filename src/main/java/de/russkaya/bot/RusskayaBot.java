package de.russkaya.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RusskayaBot extends ListenerAdapter {
    
    private JDA jda;
    private Connection database;
    private Timer reminderTimer;
    private ScheduledExecutorService scheduler;
    
    // Konfiguration - Diese Werte müssen angepasst werden
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    private static final String DATABASE_URL = System.getenv("DATABASE_URL");
    private static final long PLANT_CHANNEL_ID = Long.parseLong(System.getenv("PLANT_CHANNEL_ID"));
    private static final long SOLAR_CHANNEL_ID = Long.parseLong(System.getenv("SOLAR_CHANNEL_ID"));
    
    // Zeitkonstanten (in Stunden)
    private static final int PLANT_GROWTH_TIME = 4; // Pflanzen brauchen 4 Stunden
    private static final int SOLAR_BATTERY_TIME = 2; // Solarpanels alle 2 Stunden
    
    public static void main(String[] args) {
        new RusskayaBot().start();
    }
    
    public void start() {
        try {
            // Datenbank initialisieren
            initDatabase();
            
            // Bot starten
            jda = JDABuilder.createDefault(BOT_TOKEN)
                    .setActivity(Activity.watching("Russkaya Familie 🇷🇺"))
                    .addEventListeners(this)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGE_REACTIONS)
                    .build();
            
            jda.awaitReady();
            
            // Slash Commands registrieren
            registerCommands();
            
            // Timer für Erinnerungen starten
            startReminderSystem();
            
            System.out.println("✅ Russkaya Bot ist online! 🇷🇺");
            
        } catch (Exception e) {
            System.err.println("❌ Fehler beim Starten des Bots: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initDatabase() throws SQLException {
        database = DriverManager.getConnection(DATABASE_URL);
        
        // Tabellen erstellen
        String createPlantsTable = """
            CREATE TABLE IF NOT EXISTS plants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                username TEXT NOT NULL,
                planted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                location TEXT NOT NULL,
                status TEXT DEFAULT 'planted',
                fertilized_by TEXT,
                fertilized_at TIMESTAMP,
                harvested_by TEXT,
                harvested_at TIMESTAMP,
                car_stored TEXT,
                reminder_message_id TEXT
            )
        """;
        
        String createSolarTable = """
            CREATE TABLE IF NOT EXISTS solar_panels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                username TEXT NOT NULL,
                placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                location TEXT NOT NULL,
                status TEXT DEFAULT 'active',
                collected_by TEXT,
                collected_at TIMESTAMP,
                car_stored TEXT,
                reminder_message_id TEXT
            )
        """;
        
        try (Statement stmt = database.createStatement()) {
            stmt.executeUpdate(createPlantsTable);
            stmt.executeUpdate(createSolarTable);
        }
        
        System.out.println("✅ Datenbank initialisiert");
    }
    
    private void registerCommands() {
        jda.updateCommands().addCommands(
            // Pflanzen Commands
            Commands.slash("pflanze-säen", "Eine neue Pflanze säen")
                    .addOption(OptionType.STRING, "location", "Wo wurde die Pflanze gesät?", true),
            
            Commands.slash("pflanze-düngen", "Eine Pflanze düngen")
                    .addOption(OptionType.INTEGER, "id", "ID der Pflanze", true),
            
            Commands.slash("pflanze-ernten", "Eine Pflanze ernten")
                    .addOption(OptionType.INTEGER, "id", "ID der Pflanze", true)
                    .addOption(OptionType.STRING, "car", "In welches Auto wurde es gelegt?", true),
            
            Commands.slash("pflanzen-status", "Alle aktiven Pflanzen anzeigen"),
            
            // Solar Commands
            Commands.slash("solar-aufstellen", "Ein Solarpanel aufstellen")
                    .addOption(OptionType.STRING, "location", "Wo wurde das Panel aufgestellt?", true),
            
            Commands.slash("solar-sammeln", "Batterie von Solarpanel sammeln")
                    .addOption(OptionType.INTEGER, "id", "ID des Solarpanels", true)
                    .addOption(OptionType.STRING, "car", "In welches Auto wurde die Batterie gelegt?", true),
            
            Commands.slash("solar-status", "Alle aktiven Solarpanels anzeigen"),
            
            // Allgemeine Commands
            Commands.slash("logs", "Letzte Aktivitäten anzeigen")
                    .addOption(OptionType.INTEGER, "anzahl", "Anzahl der Logs (Standard: 10)", false)
        ).queue();
        
        System.out.println("✅ Slash Commands registriert");
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        
        try {
            switch (command) {
                case "pflanze-säen" -> handlePlantSeed(event);
                case "pflanze-düngen" -> handlePlantFertilize(event);
                case "pflanze-ernten" -> handlePlantHarvest(event);
                case "pflanzen-status" -> handlePlantsStatus(event);
                case "solar-aufstellen" -> handleSolarPlace(event);
                case "solar-sammeln" -> handleSolarCollect(event);
                case "solar-status" -> handleSolarStatus(event);
                case "logs" -> handleLogs(event);
                default -> event.reply("❌ Unbekannter Command!").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            event.reply("❌ Ein Fehler ist aufgetreten: " + e.getMessage()).setEphemeral(true).queue();
            e.printStackTrace();
        }
    }
    
    private void handlePlantSeed(SlashCommandInteractionEvent event) throws SQLException {
        String location = event.getOption("location").getAsString();
        String userId = event.getUser().getId();
        String username = event.getUser().getName();
        
        String sql = "INSERT INTO plants (user_id, username, location) VALUES (?, ?, ?)";
        PreparedStatement stmt = database.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, userId);
        stmt.setString(2, username);
        stmt.setString(3, location);
        stmt.executeUpdate();
        
        ResultSet keys = stmt.getGeneratedKeys();
        int plantId = keys.next() ? keys.getInt(1) : 0;
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🌱 Pflanze gesät!")
                .setDescription("**Spieler:** " + username + "\n" +
                               "**Standort:** " + location + "\n" +
                               "**ID:** #" + plantId + "\n" +
                               "**Wachstumszeit:** " + PLANT_GROWTH_TIME + " Stunden")
                .setColor(Color.GREEN)
                .setTimestamp(LocalDateTime.now());
        
        event.replyEmbeds(embed.build()).queue(response -> {
            // Erinnerung für diese Pflanze planen
            schedulePlantReminder(plantId, username, location, response.retrieveOriginal().complete());
        });
        
        stmt.close();
    }
    
    private void handlePlantFertilize(SlashCommandInteractionEvent event) throws SQLException {
        int plantId = event.getOption("id").getAsInt();
        String userId = event.getUser().getId();
        String username = event.getUser().getName();
        
        String checkSql = "SELECT * FROM plants WHERE id = ? AND status = 'planted'";
        PreparedStatement checkStmt = database.prepareStatement(checkSql);
        checkStmt.setInt(1, plantId);
        ResultSet result = checkStmt.executeQuery();
        
        if (!result.next()) {
            event.reply("❌ Pflanze #" + plantId + " nicht gefunden oder bereits geerntet!").setEphemeral(true).queue();
            return;
        }
        
        String updateSql = "UPDATE plants SET fertilized_by = ?, fertilized_at = CURRENT_TIMESTAMP WHERE id = ?";
        PreparedStatement updateStmt = database.prepareStatement(updateSql);
        updateStmt.setString(1, username);
        updateStmt.setInt(2, plantId);
        updateStmt.executeUpdate();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💚 Pflanze gedüngt!")
                .setDescription("**Spieler:** " + username + "\n" +
                               "**Pflanze:** #" + plantId + "\n" +
                               "**Standort:** " + result.getString("location"))
                .setColor(Color.decode("#32CD32"))
                .setTimestamp(LocalDateTime.now());
        
        event.replyEmbeds(embed.build()).queue();
        
        checkStmt.close();
        updateStmt.close();
    }
    
    private void handlePlantHarvest(SlashCommandInteractionEvent event) throws SQLException {
        int plantId = event.getOption("id").getAsInt();
        String car = event.getOption("car").getAsString();
        String username = event.getUser().getName();
        
        String updateSql = "UPDATE plants SET status = 'harvested', harvested_by = ?, harvested_at = CURRENT_TIMESTAMP, car_stored = ? WHERE id = ? AND status = 'planted'";
        PreparedStatement updateStmt = database.prepareStatement(updateSql);
        updateStmt.setString(1, username);
        updateStmt.setString(2, car);
        updateStmt.setInt(3, plantId);
        
        int affected = updateStmt.executeUpdate();
        
        if (affected == 0) {
            event.reply("❌ Pflanze #" + plantId + " nicht gefunden oder bereits geerntet!").setEphemeral(true).queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🌿 Pflanze geerntet!")
                .setDescription("**Spieler:** " + username + "\n" +
                               "**Pflanze:** #" + plantId + "\n" +
                               "**Auto:** " + car)
                .setColor(Color.decode("#228B22"))
                .setTimestamp(LocalDateTime.now());
        
        event.replyEmbeds(embed.build()).queue();
        updateStmt.close();
    }
    
    private void handleSolarPlace(SlashCommandInteractionEvent event) throws SQLException {
        String location = event.getOption("location").getAsString();
        String userId = event.getUser().getId();
        String username = event.getUser().getName();
        
        String sql = "INSERT INTO solar_panels (user_id, username, location) VALUES (?, ?, ?)";
        PreparedStatement stmt = database.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, userId);
        stmt.setString(2, username);
        stmt.setString(3, location);
        stmt.executeUpdate();
        
        ResultSet keys = stmt.getGeneratedKeys();
        int solarId = keys.next() ? keys.getInt(1) : 0;
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("☀️ Solarpanel aufgestellt!")
                .setDescription("**Spieler:** " + username + "\n" +
                               "**Standort:** " + location + "\n" +
                               "**ID:** #" + solarId + "\n" +
                               "**Batteriezeit:** " + SOLAR_BATTERY_TIME + " Stunden")
                .setColor(Color.YELLOW)
                .setTimestamp(LocalDateTime.now());
        
        event.replyEmbeds(embed.build()).queue(response -> {
            // Erinnerung für dieses Solarpanel planen
            scheduleSolarReminder(solarId, username, location, response.retrieveOriginal().complete());
        });
        
        stmt.close();
    }
    
    private void handleSolarCollect(SlashCommandInteractionEvent event) throws SQLException {
        int solarId = event.getOption("id").getAsInt();
        String car = event.getOption("car").getAsString();
        String username = event.getUser().getName();
        
        String updateSql = "UPDATE solar_panels SET status = 'collected', collected_by = ?, collected_at = CURRENT_TIMESTAMP, car_stored = ? WHERE id = ? AND status = 'active'";
        PreparedStatement updateStmt = database.prepareStatement(updateSql);
        updateStmt.setString(1, username);
        updateStmt.setString(2, car);
        updateStmt.setInt(3, solarId);
        
        int affected = updateStmt.executeUpdate();
        
        if (affected == 0) {
            event.reply("❌ Solarpanel #" + solarId + " nicht gefunden oder bereits eingesammelt!").setEphemeral(true).queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔋 Batterie eingesammelt!")
                .setDescription("**Spieler:** " + username + "\n" +
                               "**Solarpanel:** #" + solarId + "\n" +
                               "**Auto:** " + car)
                .setColor(Color.ORANGE)
                .setTimestamp(LocalDateTime.now());
        
        event.replyEmbeds(embed.build()).queue();
        updateStmt.close();
    }
    
    private void handlePlantsStatus(SlashCommandInteractionEvent event) throws SQLException {
        String sql = "SELECT * FROM plants WHERE status = 'planted' ORDER BY planted_at DESC";
        PreparedStatement stmt = database.prepareStatement(sql);
        ResultSet result = stmt.executeQuery();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🌱 Aktive Pflanzen")
                .setColor(Color.GREEN);
        
        int count = 0;
        while (result.next() && count < 10) {
            String plantInfo = String.format(
                "**ID:** #%d\n**Gesät von:** %s\n**Standort:** %s\n**Gedüngt:** %s\n",
                result.getInt("id"),
                result.getString("username"),
                result.getString("location"),
                result.getString("fertilized_by") != null ? "✅ von " + result.getString("fertilized_by") : "❌ Nicht gedüngt"
            );
            embed.addField("Pflanze #" + result.getInt("id"), plantInfo, true);
            count++;
        }
        
        if (count == 0) {
            embed.setDescription("Keine aktiven Pflanzen vorhanden.");
        }
        
        event.replyEmbeds(embed.build()).queue();
        stmt.close();
    }
    
    private void handleSolarStatus(SlashCommandInteractionEvent event) throws SQLException {
        String sql = "SELECT * FROM solar_panels WHERE status = 'active' ORDER BY placed_at DESC";
        PreparedStatement stmt = database.prepareStatement(sql);
        ResultSet result = stmt.executeQuery();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("☀️ Aktive Solarpanels")
                .setColor(Color.YELLOW);
        
        int count = 0;
        while (result.next() && count < 10) {
            String solarInfo = String.format(
                "**ID:** #%d\n**Aufgestellt von:** %s\n**Standort:** %s\n**Aufgestellt:** %s\n",
                result.getInt("id"),
                result.getString("username"),
                result.getString("location"),
                result.getTimestamp("placed_at").toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            );
            embed.addField("Panel #" + result.getInt("id"), solarInfo, true);
            count++;
        }
        
        if (count == 0) {
            embed.setDescription("Keine aktiven Solarpanels vorhanden.");
        }
        
        event.replyEmbeds(embed.build()).queue();
        stmt.close();
    }
    
    private void handleLogs(SlashCommandInteractionEvent event) throws SQLException {
        int limit = event.getOption("anzahl") != null ? event.getOption("anzahl").getAsInt() : 10;
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Letzte Aktivitäten")
                .setColor(Color.BLUE);
        
        // Pflanzen-Logs
        String plantSql = "SELECT 'PFLANZE' as type, username, 'gesät' as action, location, planted_at as timestamp FROM plants UNION ALL " +
                         "SELECT 'PFLANZE' as type, fertilized_by as username, 'gedüngt' as action, location, fertilized_at as timestamp FROM plants WHERE fertilized_by IS NOT NULL UNION ALL " +
                         "SELECT 'PFLANZE' as type, harvested_by as username, 'geerntet' as action, location, harvested_at as timestamp FROM plants WHERE harvested_by IS NOT NULL " +
                         "ORDER BY timestamp DESC LIMIT ?";
        
        PreparedStatement stmt = database.prepareStatement(plantSql);
        stmt.setInt(1, limit);
        ResultSet result = stmt.executeQuery();
        
        int count = 0;
        while (result.next() && count < limit) {
            String logEntry = String.format("**%s** %s eine %s bei *%s*\n🕐 %s",
                result.getString("username"),
                result.getString("action"),
                result.getString("type").toLowerCase(),
                result.getString("location"),
                result.getTimestamp("timestamp").toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            );
            embed.addField("", logEntry, false);
            count++;
        }
        
        if (count == 0) {
            embed.setDescription("Keine Logs vorhanden.");
        }
        
        event.replyEmbeds(embed.build()).queue();
        stmt.close();
    }
    
    private void startReminderSystem() {
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Jeden Tag um 00:00 alte Erinnerungen bereinigen
        scheduler.scheduleAtFixedRate(this::cleanupOldReminders, 0, 24, TimeUnit.HOURS);
        
        System.out.println("✅ Erinnerungssystem gestartet");
    }
    
    private void schedulePlantReminder(int plantId, String planter, String location, Message originalMessage) {
        scheduler.schedule(() -> {
            try {
                sendPlantReminder(plantId, planter, location, originalMessage.getChannel().asTextChannel());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 35, TimeUnit.MINUTES); // Erste Erinnerung nach 35 Minuten
        
        scheduler.schedule(() -> {
            try {
                sendPlantReminder(plantId, planter, location, originalMessage.getChannel().asTextChannel());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 55, TimeUnit.MINUTES); // Zweite Erinnerung nach 55 Minuten
    }
    
    private void scheduleSolarReminder(int solarId, String placer, String location, Message originalMessage) {
        scheduler.schedule(() -> {
            try {
                sendSolarReminder(solarId, placer, location, originalMessage.getChannel().asTextChannel());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 30, TimeUnit.MINUTES); // Erste Erinnerung nach 30 Minuten
        
        scheduler.schedule(() -> {
            try {
                sendSolarReminder(solarId, placer, location, originalMessage.getChannel().asTextChannel());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 50, TimeUnit.MINUTES); // Zweite Erinnerung nach 50 Minuten
        
        // Für Solar: Alle 2 Stunden erinnern
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendSolarCollectionReminder(solarId, placer, location, originalMessage.getChannel().asTextChannel());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, SOLAR_BATTERY_TIME, SOLAR_BATTERY_TIME, TimeUnit.HOURS);
    }
    
    private void sendPlantReminder(int plantId, String planter, String location, TextChannel channel) throws SQLException {
        // Prüfen ob Pflanze noch aktiv ist
        String checkSql = "SELECT * FROM plants WHERE id = ? AND status = 'planted'";
        PreparedStatement stmt = database.prepareStatement(checkSql);
        stmt.setInt(1, plantId);
        ResultSet result = stmt.executeQuery();
        
        if (!result.next()) {
            stmt.close();
            return; // Pflanze bereits geerntet
        }
        
        boolean isFertilized = result.getString("fertilized_by") != null;
        stmt.close();
        
        if (!isFertilized) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("⚠️ Dünger-Erinnerung!")
                    .setDescription("Die Pflanze #" + plantId + " von **" + planter + "** bei *" + location + "* muss gedüngt werden!")
                    .addField("Befehl", "`/pflanze-düngen id:" + plantId + "`", false)
                    .setColor(Color.RED)
                    .setTimestamp(LocalDateTime.now());
            
            channel.sendMessageEmbeds(embed.build()).queue(message -> {
                message.addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✅")).queue(); // Reaktion für "erledigt"
            });
        }
    }
    
    private void sendSolarReminder(int solarId, String placer, String location, TextChannel channel) throws SQLException {
        // Ähnlich wie Plant Reminder, aber für Solar
        String checkSql = "SELECT * FROM solar_panels WHERE id = ? AND status = 'active'";
        PreparedStatement stmt = database.prepareStatement(checkSql);
        stmt.setInt(1, solarId);
        ResultSet result = stmt.executeQuery();
        
        if (!result.next()) {
            stmt.close();
            return; // Panel bereits eingesammelt
        }
        stmt.close();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⚠️ Solarpanel-Erinnerung!")
                .setDescription("Das Solarpanel #" + solarId + " von **" + placer + "** bei *" + location + "* kann repariert werden!")
                .addField("Info", "Nach 4 Reparaturen kann eine Batterie eingesammelt werden", false)
                .setColor(Color.ORANGE)
                .setTimestamp(LocalDateTime.now());
        
        channel.sendMessageEmbeds(embed.build()).queue(message -> {
            message.addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("🔧")).queue(); // Reaktion für "repariert"
        });
    }
    
    private void sendSolarCollectionReminder(int solarId, String placer, String location, TextChannel channel) throws SQLException {
        String checkSql = "SELECT * FROM solar_panels WHERE id = ? AND status = 'active'";
        PreparedStatement stmt = database.prepareStatement(checkSql);
        stmt.setInt(1, solarId);
        ResultSet result = stmt.executeQuery();
        
        if (!result.next()) {
            stmt.close();
            return;
        }
        stmt.close();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔋 Batterie bereit!")
                .setDescription("Das Solarpanel #" + solarId + " von **" + placer + "** bei *" + location + "* hat eine Batterie bereit!")
                .addField("Befehl", "`/solar-sammeln id:" + solarId + " car:[Auto]`", false)
                .setColor(Color.GREEN)
                .setTimestamp(LocalDateTime.now());
        
        channel.sendMessageEmbeds(embed.build()).queue();
    }
    
    private void cleanupOldReminders() {
        try (Statement stmt = database.createStatement()) {
            // Alte abgeschlossene Einträge löschen (älter als 7 Tage)
            String cleanupSql = "DELETE FROM plants WHERE status = 'harvested' AND harvested_at < datetime('now', '-7 days')";
            stmt.executeUpdate(cleanupSql);
            
            cleanupSql = "DELETE FROM solar_panels WHERE status = 'collected' AND collected_at < datetime('now', '-7 days')";
            stmt.executeUpdate(cleanupSql);
            
            System.out.println("✅ Alte Einträge bereinigt");
        } catch (SQLException e) {
            System.err.println("❌ Fehler beim Bereinigen: " + e.getMessage());
        }
    }
    
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        
        // Sicherere Emoji-Behandlung
        String emoji = null;
        if (event.getReaction().getEmoji().getType() == net.dv8tion.jda.api.entities.emoji.Emoji.Type.UNICODE) {
            emoji = event.getReaction().getEmoji().asUnicode().getAsString();
        }
        
        if (emoji == null) return;
        
        // Reaktion auf Dünger-Erinnerungen
        if ("✅".equals(emoji)) {
            // Hier könnte man automatisch den Dünger-Status updaten
            // Für jetzt nur die Nachricht bearbeiten
            event.getChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if (message.getEmbeds().size() > 0) {
                    MessageEmbed embed = message.getEmbeds().get(0);
                    if (embed.getTitle() != null && embed.getTitle().contains("Dünger-Erinnerung")) {
                        EmbedBuilder newEmbed = new EmbedBuilder(embed)
                                .setTitle("✅ Erinnerung erledigt!")
                                .setColor(Color.GREEN);
                        message.editMessageEmbeds(newEmbed.build()).queue();
                    }
                }
            });
        }
        
        // Reaktion auf Solar-Erinnerungen
        if ("🔧".equals(emoji)) {
            event.getChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if (message.getEmbeds().size() > 0) {
                    MessageEmbed embed = message.getEmbeds().get(0);
                    if (embed.getTitle() != null && embed.getTitle().contains("Solarpanel-Erinnerung")) {
                        EmbedBuilder newEmbed = new EmbedBuilder(embed)
                                .setTitle("🔧 Repariert!")
                                .setColor(Color.GREEN);
                        message.editMessageEmbeds(newEmbed.build()).queue();
                    }
                }
            });
        }
    }
}
