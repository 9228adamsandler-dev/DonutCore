package com.donutclone.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public class DonutCore extends JavaPlugin {
    private DataManager data;
    private AuctionManager auction;
    private Sidebar sidebar;
    private Gui gui;
    private Rtp rtp;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataManager(this);
        data.load();
        auction = new AuctionManager(this);
        auction.load();
        sidebar = new Sidebar(this);
        gui = new Gui(this);
        rtp = new Rtp(this);

        Commands cmds = new Commands(this);
        String[] names = {"balance", "shards", "pay", "shop", "sell", "ah", "rtp",
                "shardshop", "settings", "msg", "r", "eco"};
        for (String n : names) {
            if (getCommand(n) != null) getCommand(n).setExecutor(cmds);
        }

        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(new Events(this), this);

        // sidebar refresh every second
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) sidebar.update(p);
        }, 20L, 20L);

        // autosave every 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            data.save();
            auction.save();
        }, 6000L, 6000L);

        getLogger().info("DonutCore enabled.");
    }

    @Override
    public void onDisable() {
        // close open menus so /sell items get returned
        for (Player p : Bukkit.getOnlinePlayers()) {
            InventoryHolder h = p.getOpenInventory().getTopInventory().getHolder();
            if (h instanceof Menu) p.closeInventory();
        }
        if (data != null) data.save();
        if (auction != null) auction.save();
    }

    public DataManager data() { return data; }
    public AuctionManager auction() { return auction; }
    public Sidebar sidebar() { return sidebar; }
    public Gui gui() { return gui; }
    public Rtp rtp() { return rtp; }
}
