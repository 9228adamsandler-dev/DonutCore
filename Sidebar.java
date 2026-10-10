package com.donutclone.core;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Sidebar {
    private final DonutCore plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public Sidebar(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void remove(UUID id) {
        boards.remove(id);
    }

    public void update(Player p) {
        DataManager.PlayerData d = plugin.data().get(p.getUniqueId());

        if (!d.scoreboard) {
            if (boards.remove(p.getUniqueId()) != null) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
            return;
        }

        List<String> lines = plugin.getConfig().getStringList("sidebar.lines");
        if (lines.size() > 15) lines = lines.subList(0, 15);

        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null || sb.getObjective("donut") == null || sb.getTeams().size() != lines.size()) {
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = sb.registerNewObjective("donut", Criteria.DUMMY, Component.empty());
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.numberFormat(NumberFormat.blank()); // hide red numbers
            for (int i = 0; i < lines.size(); i++) {
                Team t = sb.registerNewTeam("l" + i);
                String entry = "\u00a7" + Integer.toHexString(i) + "\u00a7r";
                t.addEntry(entry);
                obj.getScore(entry).setScore(lines.size() - i);
            }
            boards.put(p.getUniqueId(), sb);
            p.setScoreboard(sb);
        }

        Objective obj = sb.getObjective("donut");
        obj.displayName(Util.c(apply(plugin.getConfig().getString("sidebar.title", "&f{name}"), p, d)));
        for (int i = 0; i < lines.size(); i++) {
            Team t = sb.getTeam("l" + i);
            if (t != null) t.prefix(Util.c(apply(lines.get(i), p, d)));
        }
    }

    private String apply(String s, Player p, DataManager.PlayerData d) {
        long playSec = p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
        return s.replace("{name}", p.getName())
                .replace("{money}", Util.fmt(d.money))
                .replace("{shards}", Util.fmt(d.shards))
                .replace("{kills}", String.valueOf(d.kills))
                .replace("{deaths}", String.valueOf(d.deaths))
                .replace("{playtime}", Util.duration(playSec))
                .replace("{ping}", String.valueOf(p.getPing()));
    }
}
