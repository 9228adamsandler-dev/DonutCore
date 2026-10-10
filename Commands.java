package com.donutclone.core;

import com.donutclone.core.DataManager.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Commands implements CommandExecutor {
    private final DonutCore plugin;

    public Commands(DonutCore plugin) {
        this.plugin = plugin;
    }

    private Player need(CommandSender s) {
        if (s instanceof Player p) return p;
        s.sendMessage("Players only.");
        return null;
    }

    private void msg(CommandSender s, String m) {
        s.sendMessage(Util.c(m));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        DataManager dm = plugin.data();
        switch (cmd.getName().toLowerCase()) {

            case "balance", "shards" -> {
                boolean shards = cmd.getName().equalsIgnoreCase("shards");
                UUID id;
                String name;
                if (a.length > 0) {
                    OfflinePlayer t = Bukkit.getOfflinePlayer(a[0]);
                    if (!t.hasPlayedBefore() && !t.isOnline()) {
                        msg(s, "&cPlayer not found.");
                        return true;
                    }
                    id = t.getUniqueId();
                    name = a[0];
                } else if (s instanceof Player p) {
                    id = p.getUniqueId();
                    name = p.getName();
                } else {
                    msg(s, "&cSpecify a player.");
                    return true;
                }
                PlayerData d = dm.get(id);
                if (shards) msg(s, "&d" + name + "'s shards: &f" + d.shards);
                else msg(s, "&a" + name + "'s balance: &f$" + Util.fmt(d.money));
            }

            case "pay" -> {
                Player p = need(s);
                if (p == null) return true;
                if (a.length < 2) {
                    msg(s, "&cUsage: /pay <player> <amount>");
                    return true;
                }
                Player t = Bukkit.getPlayerExact(a[0]);
                if (t == null) { msg(s, "&cPlayer not online."); return true; }
                if (t.equals(p)) { msg(s, "&cYou can't pay yourself."); return true; }
                double amt = Util.parse(a[1]);
                if (amt <= 0) { msg(s, "&cInvalid amount."); return true; }
                PlayerData me = dm.get(p.getUniqueId());
                PlayerData th = dm.get(t.getUniqueId());
                if (!th.payments) { msg(s, "&c" + t.getName() + " has payments disabled."); return true; }
                if (me.money < amt) { msg(s, "&cNot enough money."); return true; }
                me.money -= amt;
                th.money += amt;
                msg(p, "&aYou paid &f" + t.getName() + " $" + Util.fmt(amt) + "&a.");
                msg(t, "&a" + p.getName() + " paid you &f$" + Util.fmt(amt) + "&a.");
            }

            case "shop" -> { Player p = need(s); if (p != null) plugin.gui().openShop(p); }
            case "sell" -> { Player p = need(s); if (p != null) plugin.gui().openSell(p); }
            case "shardshop" -> { Player p = need(s); if (p != null) plugin.gui().openShardShop(p); }
            case "settings" -> { Player p = need(s); if (p != null) plugin.gui().openSettings(p); }
            case "rtp" -> { Player p = need(s); if (p != null) plugin.gui().openRtp(p); }

            case "ah" -> {
                Player p = need(s);
                if (p == null) return true;
                if (a.length >= 1 && a[0].equalsIgnoreCase("sell")) {
                    if (a.length < 2) { msg(s, "&cUsage: /ah sell <price>"); return true; }
                    double price = Util.parse(a[1]);
                    if (price <= 0) { msg(s, "&cInvalid price."); return true; }
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (hand.getType() == Material.AIR) { msg(s, "&cHold an item to sell."); return true; }
                    int max = plugin.getConfig().getInt("ah.max-listings", 10);
                    if (plugin.auction().of(p.getUniqueId()).size() >= max) {
                        msg(s, "&cYou can only have " + max + " listings.");
                        return true;
                    }
                    ItemStack copy = hand.clone();
                    p.getInventory().setItemInMainHand(null);
                    plugin.auction().add(p.getUniqueId(), p.getName(), copy, price);
                    msg(s, "&aListed for &f$" + Util.fmt(price) + "&a.");
                } else {
                    plugin.gui().openAh(p, 0, false);
                }
            }

            case "msg" -> {
                Player p = need(s);
                if (p == null) return true;
                if (a.length < 2) { msg(s, "&cUsage: /msg <player> <message>"); return true; }
                Player t = Bukkit.getPlayerExact(a[0]);
                if (t == null) { msg(s, "&cPlayer not online."); return true; }
                sendPm(p, t, String.join(" ", java.util.Arrays.copyOfRange(a, 1, a.length)));
            }

            case "r" -> {
                Player p = need(s);
                if (p == null) return true;
                if (a.length < 1) { msg(s, "&cUsage: /r <message>"); return true; }
                UUID last = dm.get(p.getUniqueId()).lastMsg;
                Player t = last == null ? null : Bukkit.getPlayer(last);
                if (t == null) { msg(s, "&cNobody to reply to."); return true; }
                sendPm(p, t, String.join(" ", a));
            }

            case "eco" -> {
                if (!s.hasPermission("donutcore.admin")) { msg(s, "&cNo permission."); return true; }
                if (a.length < 3) {
                    msg(s, "&cUsage: /eco <setmoney|givemoney|takemoney|setshards|giveshards|takeshards> <player> <amount>");
                    return true;
                }
                OfflinePlayer t = Bukkit.getOfflinePlayer(a[1]);
                if (!t.hasPlayedBefore() && !t.isOnline()) { msg(s, "&cPlayer not found."); return true; }
                double amt = Util.parse(a[2]);
                if (amt < 0) { msg(s, "&cInvalid amount."); return true; }
                PlayerData d = dm.get(t.getUniqueId());
                switch (a[0].toLowerCase()) {
                    case "setmoney" -> d.money = amt;
                    case "givemoney" -> d.money += amt;
                    case "takemoney" -> d.money = Math.max(0, d.money - amt);
                    case "setshards" -> d.shards = (long) amt;
                    case "giveshards" -> d.shards += (long) amt;
                    case "takeshards" -> d.shards = Math.max(0, d.shards - (long) amt);
                    default -> { msg(s, "&cUnknown action."); return true; }
                }
                msg(s, "&aDone. " + a[1] + " now has &f$" + Util.fmt(d.money) + " &aand &d" + d.shards + " shards&a.");
            }

            default -> { return false; }
        }
        return true;
    }

    private void sendPm(Player from, Player to, String text) {
        PlayerData tf = plugin.data().get(to.getUniqueId());
        if (!tf.messages && !from.hasPermission("donutcore.admin")) {
            msg(from, "&c" + to.getName() + " has private messages disabled.");
            return;
        }
        Component body = Component.text(text, NamedTextColor.WHITE);
        from.sendMessage(Util.c("&d[You -> " + to.getName() + "] ").append(body));
        to.sendMessage(Util.c("&d[" + from.getName() + " -> You] ").append(body));
        tf.lastMsg = from.getUniqueId();
        plugin.data().get(from.getUniqueId()).lastMsg = to.getUniqueId();
    }
}
