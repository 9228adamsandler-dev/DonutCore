package com.donutclone.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    public static class PlayerData {
        public double money;
        public long shards;
        public int kills;
        public int deaths;
        public boolean scoreboard = true;
        public boolean messages = true;
        public boolean payments = true;
        public UUID lastMsg; // not saved
    }

    private final DonutCore plugin;
    private final File file;
    private final Map<UUID, PlayerData> map = new HashMap<>();

    public DataManager(DonutCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = yml.getConfigurationSection("players");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(k);
            if (c == null) continue;
            PlayerData d = new PlayerData();
            d.money = c.getDouble("money");
            d.shards = c.getLong("shards");
            d.kills = c.getInt("kills");
            d.deaths = c.getInt("deaths");
            d.scoreboard = c.getBoolean("scoreboard", true);
            d.messages = c.getBoolean("messages", true);
            d.payments = c.getBoolean("payments", true);
            try {
                map.put(UUID.fromString(k), d);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> e : map.entrySet()) {
            String p = "players." + e.getKey() + ".";
            PlayerData d = e.getValue();
            out.set(p + "money", d.money);
            out.set(p + "shards", d.shards);
            out.set(p + "kills", d.kills);
            out.set(p + "deaths", d.deaths);
            out.set(p + "scoreboard", d.scoreboard);
            out.set(p + "messages", d.messages);
            out.set(p + "payments", d.payments);
        }
        try {
            out.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save data.yml: " + ex.getMessage());
        }
    }

    public PlayerData get(UUID id) {
        return map.computeIfAbsent(id, k -> {
            PlayerData d = new PlayerData();
            d.money = plugin.getConfig().getDouble("starting-money", 0);
            d.shards = plugin.getConfig().getLong("starting-shards", 0);
            return d;
        });
    }
}
