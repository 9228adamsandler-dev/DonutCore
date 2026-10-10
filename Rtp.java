package com.donutclone.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Rtp {
    private static final Set<Material> BAD = EnumSet.of(
            Material.MAGMA_BLOCK, Material.CACTUS, Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE, Material.BEDROCK);

    private final DonutCore plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();

    public Rtp(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void start(Player p, World.Environment env) {
        long left = cooldowns.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
        if (left > 0 && !p.hasPermission("donutcore.admin")) {
            p.sendMessage(Util.c("&cRTP is on cooldown for " + Util.duration(left / 1000 + 1) + "."));
            return;
        }
        World world = null;
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == env) {
                world = w;
                break;
            }
        }
        if (world == null) {
            p.sendMessage(Util.c("&cThat world is not available."));
            return;
        }
        p.sendMessage(Util.c("&7Searching for a safe location..."));
        attempt(p, world, 0);
    }

    private void attempt(Player p, World w, int n) {
        if (!p.isOnline()) return;
        if (n >= 20) {
            p.sendMessage(Util.c("&cCouldn't find a safe spot. Try again."));
            return;
        }
        int min = plugin.getConfig().getInt("rtp.min", 500);
        int max = plugin.getConfig().getInt("rtp.max", 5000);
        double scale = w.getEnvironment() == World.Environment.NETHER ? 0.125 : 1.0;
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = (min + random.nextDouble() * Math.max(1, max - min)) * scale;
        Location spawn = w.getSpawnLocation();
        final int x = (int) (spawn.getX() + Math.cos(angle) * dist);
        final int z = (int) (spawn.getZ() + Math.sin(angle) * dist);

        w.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Location loc = findSafe(w, x, z);
                    if (loc == null || !w.getWorldBorder().isInside(loc)) {
                        attempt(p, w, n + 1);
                        return;
                    }
                    p.teleportAsync(loc).thenAccept(ok -> {
                        if (ok) {
                            long cd = plugin.getConfig().getLong("rtp.cooldown-seconds", 30) * 1000L;
                            cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + cd);
                            p.sendMessage(Util.c("&aTeleported to &f" + x + ", " + loc.getBlockY() + ", " + z + "&a."));
                        }
                    });
                }));
    }

    private Location findSafe(World w, int x, int z) {
        if (w.getEnvironment() == World.Environment.NETHER) {
            for (int y = 120; y > 32; y--) {
                Block b = w.getBlockAt(x, y, z);
                if (b.getType().isSolid() && !BAD.contains(b.getType())
                        && w.getBlockAt(x, y + 1, z).isEmpty()
                        && w.getBlockAt(x, y + 2, z).isEmpty()) {
                    return new Location(w, x + 0.5, y + 1, z + 0.5);
                }
            }
            return null;
        }
        int y = w.getHighestBlockYAt(x, z);
        Block b = w.getBlockAt(x, y, z);
        if (!b.getType().isSolid() || BAD.contains(b.getType())) return null;
        if (!w.getBlockAt(x, y + 1, z).isEmpty() || !w.getBlockAt(x, y + 2, z).isEmpty()) return null;
        return new Location(w, x + 0.5, y + 1, z + 0.5);
    }
}
