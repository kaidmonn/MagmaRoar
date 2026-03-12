package com.example.dragonstaff;

import org.bukkit.plugin.java.JavaPlugin;

public final class DragonStaff extends JavaPlugin {

    private static DragonStaff instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new StaffEvents(), this);
        getCommand("staff").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(StaffItem.createStaff());
            }
            return true;
        });
    }

    public static DragonStaff getInstance() {
        return instance;
    }
}