package com.donutclone.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/** Marker holder so we can recognise our own GUIs. */
public class Menu implements InventoryHolder {
    public final String type;
    public int page = 0;
    public String arg = "";
    public final Map<Integer, String> ids = new HashMap<>();
    private final Inventory inv;

    public Menu(String type, int size, Component title) {
        this.type = type;
        this.inv = Bukkit.createInventory(this, size, title);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
