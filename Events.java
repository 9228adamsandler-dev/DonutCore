package com.donutclone.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Events implements Listener {
    private final DonutCore plugin;

    public Events(DonutCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.data().get(e.getPlayer().getUniqueId());
        plugin.sidebar().update(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.sidebar().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        plugin.data().get(victim.getUniqueId()).deaths++;
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            plugin.data().get(killer.getUniqueId()).kills++;
        }
    }
}
