package com.donutclone.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Util {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private Util() {}

    public static Component c(String s) {
        return LEGACY.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    /** 1500 -> 1.5K, 1000000000 -> 1B */
    public static String fmt(double v) {
        String[] suffix = {"", "K", "M", "B", "T"};
        int i = 0;
        double x = v;
        while (Math.abs(x) >= 1000 && i < suffix.length - 1) {
            x /= 1000;
            i++;
        }
        String n;
        if (x == Math.floor(x)) {
            n = String.valueOf((long) x);
        } else {
            n = String.format(Locale.US, "%.2f", x).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return n + suffix[i];
    }

    /** Parses 1000, 1k, 2.5m, 1b. Returns -1 if invalid. */
    public static double parse(String s) {
        try {
            s = s.toLowerCase(Locale.ROOT).replace(",", "").trim();
            if (s.isEmpty()) return -1;
            double mult = 1;
            char last = s.charAt(s.length() - 1);
            switch (last) {
                case 'k' -> mult = 1e3;
                case 'm' -> mult = 1e6;
                case 'b' -> mult = 1e9;
                case 't' -> mult = 1e12;
                default -> { }
            }
            if (mult != 1) s = s.substring(0, s.length() - 1);
            double v = Double.parseDouble(s) * mult;
            return Double.isFinite(v) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** seconds -> "14d 16h" style */
    public static String duration(long sec) {
        long d = sec / 86400;
        long h = (sec % 86400) / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public static ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        if (lore.length > 0) {
            List<Component> l = new ArrayList<>();
            Arrays.stream(lore).forEach(s -> l.add(c(s)));
            meta.lore(l);
        }
        it.setItemMeta(meta);
        return it;
    }

    public static void addLore(ItemStack it, String... lines) {
        ItemMeta meta = it.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null
                ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        for (String l : lines) lore.add(c(l));
        meta.lore(lore);
        it.setItemMeta(meta);
    }

    public static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String w : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    public static Material mat(String name, Material def) {
        if (name == null) return def;
        Material m = Material.matchMaterial(name);
        return m == null ? def : m;
    }

    public static void give(Player p, ItemStack it) {
        p.getInventory().addItem(it).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }
}
