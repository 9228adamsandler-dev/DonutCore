package com.donutclone.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionManager {
    public static class Listing {
        public String id;
        public UUID seller;
        public String sellerName;
        public ItemStack item;
        public double price;
        public long expires;

        public boolean expired() {
            return System.currentTimeMillis() >= expires;
        }
    }

    private final DonutCore plugin;
    private final File file;
    private final Map<String, Listing> listings = new LinkedHashMap<>();

    public AuctionManager(DonutCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ah.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = yml.getConfigurationSection("listings");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(k);
            if (c == null) continue;
            ItemStack item = c.getItemStack("item");
            if (item == null) continue;
            Listing l = new Listing();
            l.id = k;
            l.seller = UUID.fromString(c.getString("seller", ""));
            l.sellerName = c.getString("name", "Unknown");
            l.item = item;
            l.price = c.getDouble("price");
            l.expires = c.getLong("expires");
            listings.put(k, l);
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        for (Listing l : listings.values()) {
            String p = "listings." + l.id + ".";
            out.set(p + "seller", l.seller.toString());
            out.set(p + "name", l.sellerName);
            out.set(p + "item", l.item);
            out.set(p + "price", l.price);
            out.set(p + "expires", l.expires);
        }
        try {
            out.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save ah.yml: " + e.getMessage());
        }
    }

    public Listing get(String id) {
        return listings.get(id);
    }

    public void remove(String id) {
        listings.remove(id);
    }

    public Listing add(UUID seller, String name, ItemStack item, double price) {
        Listing l = new Listing();
        l.id = UUID.randomUUID().toString().substring(0, 8);
        l.seller = seller;
        l.sellerName = name;
        l.item = item;
        l.price = price;
        long hours = plugin.getConfig().getLong("ah.duration-hours", 48);
        l.expires = System.currentTimeMillis() + hours * 3600_000L;
        listings.put(l.id, l);
        return l;
    }

    public List<Listing> active() {
        List<Listing> out = new ArrayList<>();
        for (Listing l : listings.values()) if (!l.expired()) out.add(l);
        return out;
    }

    public List<Listing> of(UUID seller) {
        List<Listing> out = new ArrayList<>();
        for (Listing l : listings.values()) if (l.seller.equals(seller)) out.add(l);
        return out;
    }
}
