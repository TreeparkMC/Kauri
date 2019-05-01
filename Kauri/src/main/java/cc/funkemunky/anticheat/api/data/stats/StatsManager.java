package cc.funkemunky.anticheat.api.data.stats;

import cc.funkemunky.anticheat.Kauri;
import cc.funkemunky.anticheat.api.data.PlayerData;
import cc.funkemunky.api.Atlas;
import cc.funkemunky.api.database.Database;
import cc.funkemunky.api.database.DatabaseType;
import cc.funkemunky.api.utils.ConfigSetting;
import cc.funkemunky.api.utils.Init;
import cc.funkemunky.api.utils.Priority;
import lombok.Getter;

import java.util.UUID;

@Getter
@Init(priority = Priority.HIGH)
public class StatsManager {
    private int flagged;
    private int banned;

    @ConfigSetting(path = "data.stats", name = "type")
    public String type = "FLATFILE";

    public StatsManager() {
        flagged = 0;
        banned = 0;
        Atlas.getInstance().getDatabaseManager().createDatabase("KauriStats", DatabaseType.valueOf(type));
        loadStats();
    }

    public void saveStats() {
        Database database = Atlas.getInstance().getDatabaseManager().getDatabase("KauriStats");
        database.getDatabaseValues().put("Kauri;flagged", flagged);
        database.getDatabaseValues().put("Kauri;banned", banned);
        database.saveDatabase();
    }

    public void loadStats() {
        Database database = Atlas.getInstance().getDatabaseManager().getDatabase("KauriStats");
        database.loadDatabase();
        flagged = (int) database.getDatabaseValues().getOrDefault("Kauri;flagged", 0);
        banned = (int) database.getDatabaseValues().getOrDefault("Kauri;banned", 0);
    }

    public void resetStats() {
        flagged = banned = 0;
        Database database = Atlas.getInstance().getDatabaseManager().getDatabase("KauriStats");
        database.getDatabaseValues().clear();
        database.saveDatabase();
    }

    public void addFlag() {
        flagged++;
    }

    public void addFlag(int amount) {
        flagged += amount;
    }

    public void addBan() {
        banned++;
    }

    public void removeBan() {
        banned -= banned > 0 ? 1 : 0;
    }

    public boolean isPlayerBanned(PlayerData data) {
        return Kauri.getInstance().getLoggerManager().isBanned(data.getUuid());
    }

    public boolean isPlayerBanned(UUID uuid) {
        return Kauri.getInstance().getLoggerManager().isBanned(uuid);
    }
}