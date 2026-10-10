package com.donutclone.core;

import com.donutclone.core.AuctionManager.Listing;
import com.donutclone.core.DataManager.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Gui implements Listener {
    private final DonutCore plugin;

    public Gui(DonutCore plugin) {
        this.plugin = plugin;
    }

    private void fill(Inventory inv) {
        ItemStack f = Util.item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, f);
        }
    }

    // ------------------------------------------------------------ SHOP
    public void openShop(Player p) {
        Menu m = new Menu("shop", 27, Util.c("&8Shop"));
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("shop.categories");
        int slot = 10;
        if (cats != null) {
            for (String key : cats.getKeys(false)) {
                ConfigurationSection s = cats.getConfigurationSection(key);
                if (s == null) continue;
                Material icon = Util.mat(s.getString("icon"), Material.CHEST);
                m.getInventory().setItem(slot, Util.item(icon, s.getString("name", key), "&7Click to browse"));
                m.ids.put(slot, key);
                slot++;
                if (slot == 17) slot = 19;
                if (slot > 25) break;
            }
        }
        fill(m.getInventory());
        p.openInventory(m.getInventory());
    }

    public void openCategory(Player p, String key) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("shop.categories." + key);
        if (s == null) return;
        ConfigurationSection items = s.getConfigurationSection("items");
        Menu m = new Menu("cat", 54, Util.c("&8Shop - " + key));
        m.arg = key;
        int slot = 0;
        if (items != null) {
            for (String k : items.getKeys(false)) {
                if (slot >= 45) break;
                Material mat = Material.matchMaterial(k);
                if (mat == null) continue;
                ItemStack it = new ItemStack(mat);
                Util.addLore(it, "", "&7Price: &a$" + Util.fmt(items.getDouble(k)),
                        "&eLeft-click: &fbuy 1", "&eShift-click: &fbuy 64");
                m.getInventory().setItem(slot, it);
                m.ids.put(slot, mat.name());
                slot++;
            }
        }
        m.getInventory().setItem(45, Util.item(Material.GOLD_INGOT, "&aYour balance",
                "&f$" + Util.fmt(plugin.data().get(p.getUniqueId()).money)));
        m.getInventory().setItem(49, Util.item(Material.BARRIER, "&cBack"));
        m.ids.put(49, "back");
        fill(m.getInventory());
        p.openInventory(m.getInventory());
    }

    private void categoryClick(Player p, Menu m, String id, boolean shift) {
        if (id.equals("back")) {
            openShop(p);
            return;
        }
        Material mat = Material.matchMaterial(id);
        if (mat == null) return;
        double price = plugin.getConfig().getDouble("shop.categories." + m.arg + ".items." + id);
        int amount = shift ? 64 : 1;
        double total = price * amount;
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d.money < total) {
            p.sendMessage(Util.c("&cYou need &f$" + Util.fmt(total) + "&c."));
            return;
        }
        d.money -= total;
        Util.give(p, new ItemStack(mat, amount));
        p.sendMessage(Util.c("&aBought &f" + amount + "x " + Util.pretty(mat) + " &afor &f$" + Util.fmt(total)));
        openCategory(p, m.arg);
    }

    // ------------------------------------------------------------ SELL
    public void openSell(Player p) {
        Menu m = new Menu("sell", 54, Util.c("&8Sell - close to sell"));
        p.openInventory(m.getInventory());
        p.sendMessage(Util.c("&7Put items in the menu and close it to sell."));
    }

    // ------------------------------------------------------------ AUCTION HOUSE
    public void openAh(Player p, int page, boolean mine) {
        List<Listing> list = new ArrayList<>(mine
                ? plugin.auction().of(p.getUniqueId()) : plugin.auction().active());
        Collections.reverse(list);
        int per = 45;
        int pages = Math.max(1, (list.size() + per - 1) / per);
        page = Math.max(0, Math.min(page, pages - 1));

        String title = (mine ? "&8Your Listings" : "&8Auction House") + " (" + (page + 1) + "/" + pages + ")";
        Menu m = new Menu("ah", 54, Util.c(title));
        m.page = page;
        m.arg = mine ? "mine" : "all";

        for (int i = 0; i < per; i++) {
            int idx = page * per + i;
            if (idx >= list.size()) break;
            Listing l = list.get(idx);
            ItemStack it = l.item.clone();
            long left = Math.max(0, (l.expires - System.currentTimeMillis()) / 1000);
            Util.addLore(it, "", "&7Price: &a$" + Util.fmt(l.price),
                    "&7Seller: &f" + l.sellerName,
                    l.expired() ? "&cExpired" : "&7Expires in: &f" + Util.duration(left),
                    "",
                    mine ? (l.expired() ? "&eClick to claim item" : "&eClick to cancel listing")
                            : "&eClick to buy");
            m.getInventory().setItem(i, it);
            m.ids.put(i, l.id);
        }

        if (page > 0) {
            m.getInventory().setItem(45, Util.item(Material.ARROW, "&ePrevious page"));
            m.ids.put(45, "prev");
        }
        if (page < pages - 1) {
            m.getInventory().setItem(53, Util.item(Material.ARROW, "&eNext page"));
            m.ids.put(53, "next");
        }
        m.getInventory().setItem(47, Util.item(mine ? Material.CHEST : Material.ENDER_CHEST,
                mine ? "&aAll listings" : "&aYour listings", "&7Click to switch"));
        m.ids.put(47, "toggle");
        m.getInventory().setItem(49, Util.item(Material.NETHER_STAR, "&6Auction House",
                "&7Sell the item in your hand:", "&f/ah sell <price>",
                "", "&7Balance: &a$" + Util.fmt(plugin.data().get(p.getUniqueId()).money)));
        fillBottom(m.getInventory());
        p.openInventory(m.getInventory());
    }

    private void fillBottom(Inventory inv) {
        ItemStack f = Util.item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, f);
    }

    private void ahClick(Player p, Menu m, String id) {
        boolean mine = m.arg.equals("mine");
        switch (id) {
            case "prev" -> { openAh(p, m.page - 1, mine); return; }
            case "next" -> { openAh(p, m.page + 1, mine); return; }
            case "toggle" -> { openAh(p, 0, !mine); return; }
            default -> { }
        }
        Listing l = plugin.auction().get(id);
        if (l == null) {
            p.sendMessage(Util.c("&cThat listing is no longer available."));
            openAh(p, m.page, mine);
            return;
        }
        boolean own = l.seller.equals(p.getUniqueId());
        if (own) {
            if (!mine) {
                p.sendMessage(Util.c("&7That's your listing. Open &fYour listings &7to cancel it."));
                return;
            }
            plugin.auction().remove(id);
            Util.give(p, l.item.clone());
            p.sendMessage(Util.c(l.expired() ? "&aItem claimed." : "&aListing cancelled, item returned."));
            openAh(p, m.page, true);
            return;
        }
        if (mine) return;
        if (l.expired()) {
            openAh(p, m.page, false);
            return;
        }
        PlayerData buyer = plugin.data().get(p.getUniqueId());
        if (buyer.money < l.price) {
            p.sendMessage(Util.c("&cYou need &f$" + Util.fmt(l.price) + "&c."));
            return;
        }
        double tax = plugin.getConfig().getDouble("ah.tax-percent", 0) / 100.0;
        buyer.money -= l.price;
        plugin.data().get(l.seller).money += l.price * (1 - tax);
        plugin.auction().remove(id);
        Util.give(p, l.item.clone());
        p.sendMessage(Util.c("&aBought for &f$" + Util.fmt(l.price) + "&a."));
        Player seller = Bukkit.getPlayer(l.seller);
        if (seller != null) {
            seller.sendMessage(Util.c("&a" + p.getName() + " bought your item for &f$" + Util.fmt(l.price * (1 - tax)) + "&a."));
        }
        openAh(p, m.page, false);
    }

    // ------------------------------------------------------------ SHARD SHOP
    private static final int[] SHARD_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    public void openShardShop(Player p) {
        Menu m = new Menu("shards", 27, Util.c("&8Shard Shop"));
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("shardshop.items");
        int i = 0;
        if (s != null) {
            for (String k : s.getKeys(false)) {
                if (i >= SHARD_SLOTS.length) break;
                ConfigurationSection e = s.getConfigurationSection(k);
                if (e == null) continue;
                m.getInventory().setItem(SHARD_SLOTS[i], Util.item(
                        Util.mat(e.getString("icon"), Material.PAPER), e.getString("name", k),
                        "&7Cost: &d" + e.getInt("cost") + " shards", "", "&eClick to purchase"));
                m.ids.put(SHARD_SLOTS[i], k);
                i++;
            }
        }
        m.getInventory().setItem(4, Util.item(Material.AMETHYST_SHARD, "&dYour shards",
                "&f" + plugin.data().get(p.getUniqueId()).shards));
        fill(m.getInventory());
        p.openInventory(m.getInventory());
    }

    private void shardClick(Player p, String key) {
        ConfigurationSection e = plugin.getConfig().getConfigurationSection("shardshop.items." + key);
        if (e == null) return;
        int cost = e.getInt("cost");
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d.shards < cost) {
            p.sendMessage(Util.c("&cYou need &d" + cost + " shards&c."));
            return;
        }
        d.shards -= cost;
        String cmd = e.getString("command", "");
        if (!cmd.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
        }
        p.sendMessage(Util.c("&aPurchased for &d" + cost + " shards&a."));
        openShardShop(p);
    }

    // ------------------------------------------------------------ SETTINGS
    public void openSettings(Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        Menu m = new Menu("settings", 27, Util.c("&8Settings"));
        toggle(m, 11, "scoreboard", "Sidebar", Material.OAK_SIGN, d.scoreboard, "Show the stats sidebar");
        toggle(m, 13, "messages", "Private Messages", Material.PAPER, d.messages, "Receive /msg from others");
        toggle(m, 15, "payments", "Payments", Material.GOLD_INGOT, d.payments, "Receive /pay from others");
        fill(m.getInventory());
        p.openInventory(m.getInventory());
    }

    private void toggle(Menu m, int slot, String id, String name, Material icon, boolean on, String desc) {
        m.getInventory().setItem(slot, Util.item(icon, (on ? "&a" : "&c") + name,
                "&7" + desc, "", "&7Status: " + (on ? "&aEnabled" : "&cDisabled"), "&eClick to toggle"));
        m.ids.put(slot, id);
    }

    private void settingsClick(Player p, String id) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        switch (id) {
            case "scoreboard" -> d.scoreboard = !d.scoreboard;
            case "messages" -> d.messages = !d.messages;
            case "payments" -> d.payments = !d.payments;
            default -> { }
        }
        if (id.equals("scoreboard")) plugin.sidebar().update(p);
        openSettings(p);
    }

    // ------------------------------------------------------------ RTP MENU
    public void openRtp(Player p) {
        Menu m = new Menu("rtp", 27, Util.c("&8Random Teleport"));
        m.getInventory().setItem(11, Util.item(Material.GRASS_BLOCK, "&aOverworld", "&7Click to teleport"));
        m.ids.put(11, "overworld");
        m.getInventory().setItem(15, Util.item(Material.NETHERRACK, "&cNether", "&7Click to teleport"));
        m.ids.put(15, "nether");
        fill(m.getInventory());
        p.openInventory(m.getInventory());
    }

    // ------------------------------------------------------------ EVENTS
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (m.type.equals("sell")) return; // free interaction in sell menu
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getInventory())) return;
        String id = m.ids.get(e.getSlot());
        if (id == null) return;

        switch (m.type) {
            case "shop" -> openCategory(p, id);
            case "cat" -> categoryClick(p, m, id, e.isShiftClick());
            case "ah" -> ahClick(p, m, id);
            case "shards" -> shardClick(p, id);
            case "settings" -> settingsClick(p, id);
            case "rtp" -> {
                p.closeInventory();
                plugin.rtp().start(p, id.equals("nether") ? World.Environment.NETHER : World.Environment.NORMAL);
            }
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m)) return;
        if (m.type.equals("sell")) return;
        int top = e.getInventory().getSize();
        for (int slot : e.getRawSlots()) {
            if (slot < top) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m) || !m.type.equals("sell")) return;
        Player p = (Player) e.getPlayer();
        PlayerData d = plugin.data().get(p.getUniqueId());
        double total = 0;
        int count = 0;
        for (ItemStack it : e.getInventory().getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            double price = plugin.getConfig().getDouble("sell." + it.getType().name(), 0);
            if (price > 0 && !it.hasItemMeta()) {
                total += price * it.getAmount();
                count += it.getAmount();
            } else {
                Util.give(p, it);
            }
        }
        e.getInventory().clear();
        if (total > 0) {
            d.money += total;
            p.sendMessage(Util.c("&aSold &f" + count + " items &afor &f$" + Util.fmt(total) + "&a."));
        }
    }
}
