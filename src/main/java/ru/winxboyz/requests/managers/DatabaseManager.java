package ru.winxboyz.requests.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ru.winxboyz.requests.ReQuests;
import ru.winxboyz.requests.utils.QuestData;
import ru.winxboyz.requests.utils.Tracker;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;

public class DatabaseManager {

    private ReQuests plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ReQuests plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        HikariConfig config = new HikariConfig();
        
        File file = new File(plugin.getDataFolder(), "requests.db");

        config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath() + "?foreign_keys=on");
        
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        
        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("База данных не инициализирована!");
        }
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> createTables() {
        CompletableFuture<Void> result = new CompletableFuture<>();

        String
            query1 = "CREATE TABLE IF NOT EXISTS quests ("
                  +  "quest_name VARCHAR2(32) NOT NULL,"
                  +  "player_uuid VARCHAR2(36) NOT NULL,"
                  +  "started_at INTEGER NOT NULL,"
                  +  "cooldown_started_at INTEGER,"
                  +  "PRIMARY KEY(player_uuid,quest_name)"
                  +  ");",
            query2 = "CREATE TABLE IF NOT EXISTS trackers ("
                  +  "quest_name VARCHAR2(32) NOT NULL,"
                  +  "player_uuid VARCHAR2(36) NOT NULL,"
                  +  "objective_id INTEGER NOT NULL,"
                  +  "progress_amount INTEGER NOT NULL DEFAULT 0,"
                  +  "FOREIGN KEY(player_uuid,quest_name) REFERENCES quests(player_uuid,quest_name) ON DELETE CASCADE,"
                  +  "PRIMARY KEY(player_uuid,quest_name,objective_id)"
                  +  ");";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try(Connection conn = getConnection()) {
                Statement stmt = conn.createStatement();
                stmt.addBatch(query1);
                stmt.addBatch(query2);
                stmt.executeBatch();
                result.complete(null);
            } catch(SQLException e) {
                plugin.getLogger().severe("Could not create tables\n" + e.getErrorCode() + ": " + e.getMessage());
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private record QuestSnapshot(String uuid, String questName, long startedAt, Long cooldownStartedAt, int[] progress) {}

    private List<QuestSnapshot> getSnapshots() {
        List<QuestSnapshot> snapshots = new ArrayList<>();
        for(Map.Entry<UUID,List<Tracker>> entry : plugin.getProgress().entrySet()) {
            String uuid = entry.getKey().toString();
            List<Tracker> trackers = entry.getValue();
            for(Tracker t : trackers) {
                if(!t.isDirty())
                    continue;
                snapshots.add(new QuestSnapshot(uuid, t.getQuest().getName(), t.getStartTime(), t.getCooldownStartTime(), t.getProgress()));
                t.clear();
            }
        }
        return snapshots;
    }

    public void writeData(List<QuestSnapshot> snapshots) throws SQLException {
        if(snapshots.isEmpty())
            return;
        String
            query1 =
                "INSERT INTO quests (player_uuid, quest_name, started_at, cooldown_started_at)" +
                "VALUES (?,?,?,?)" +
                "ON CONFLICT(player_uuid,quest_name) DO UPDATE SET cooldown_started_at=excluded.cooldown_started_at;",
            query2 =
                "INSERT INTO trackers (player_uuid,quest_name,objective_id,progress_amount)" +
                "VALUES (?,?,?,?)" +
                "ON CONFLICT(player_uuid,quest_name,objective_id) DO UPDATE SET progress_amount=excluded.progress_amount";
        try(Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement
                stmt1 = conn.prepareStatement(query1),
                stmt2 = conn.prepareStatement(query2);
            for(QuestSnapshot snapshot : snapshots) {         
                stmt1.setString(1, snapshot.uuid);
                stmt1.setString(2, snapshot.questName);
                stmt1.setLong(3, snapshot.startedAt);
                if(snapshot.cooldownStartedAt == null)
                    stmt1.setNull(4, Types.TIMESTAMP);
                else
                    stmt1.setLong(4, snapshot.cooldownStartedAt);
                stmt1.addBatch();
                for(int i = 0; i < snapshot.progress.length; i++) {
                    stmt2.setString(1, snapshot.uuid);
                    stmt2.setString(2, snapshot.questName);
                    stmt2.setInt(3, i);
                    stmt2.setInt(4, snapshot.progress[i]);
                    stmt2.addBatch();   
                }
            }
            stmt1.executeBatch();
            stmt2.executeBatch();
            conn.commit();
        }
    }

    public CompletableFuture<Void> saveProgress() {
        List<QuestSnapshot> snapshots = getSnapshots();
        if(snapshots.isEmpty()) return CompletableFuture.completedFuture(null);

        CompletableFuture<Void> result = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                writeData(snapshots);
                result.complete(null);
            } catch(SQLException e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    public void saveProgressSync() {
        List<QuestSnapshot> snapshots = getSnapshots();
        if(snapshots.isEmpty()) return;

        try { writeData(snapshots); }
        catch(SQLException e) {
            plugin.getLogger().severe("Couldn't write data to db:\n" + e.getMessage());
        }
    }

    public CompletableFuture<Map<UUID,List<Tracker>>> loadProgress() {
        CompletableFuture<Map<UUID,List<Tracker>>> result = new CompletableFuture<>();

        String query =
            "SELECT q.player_uuid,q.quest_name,q.started_at,q.cooldown_started_at,t.objective_id,t.progress_amount " +
            "FROM quests q JOIN trackers t ON q.player_uuid=t.player_uuid AND q.quest_name=t.quest_name;";
        
        Map<String,QuestData> quests = new HashMap<>();
        for(QuestData q : plugin.getQuests())
            quests.put(q.getName(), q);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try(Connection conn = getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();
                Map<UUID,List<Tracker>> p = new HashMap<>();
                Map<String,Tracker> trackersByKey = new HashMap<>();
                while(rs.next()) {
                    String questName = rs.getString("quest_name");

                    QuestData quest = quests.get(questName);
                    if(quest == null)
                        continue;

                    String uuid = rs.getString("player_uuid");


                    String key = uuid + ":" + questName;
                    Tracker tracker = trackersByKey.get(key);

                    if(tracker == null) {
                        long rawCooldown = rs.getLong("cooldown_started_at");
                        Long cooldownStartTime = rs.wasNull() ? null : rawCooldown;
                        long startTime = rs.getLong("started_at");
                        tracker = new Tracker(quest,startTime,cooldownStartTime);
                        List<Tracker> trackers = p
                        .computeIfAbsent(UUID.fromString(uuid),k->new ArrayList<>());
                        trackers.add(tracker);
                        trackersByKey.put(key, tracker);
                    }
                    int
                        objectiveIndex = rs.getInt("objective_id"),
                        progressAmount = rs.getInt("progress_amount");
                    tracker.setProgress(objectiveIndex, progressAmount);
                }
                for(List<Tracker> trackers : p.values())
                    for(Tracker t : trackers)
                        t.clear();
                result.complete(p);
            } catch(SQLException e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
