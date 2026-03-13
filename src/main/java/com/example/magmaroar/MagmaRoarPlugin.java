package com.example.magmaroar;

import org.bukkit.plugin.java.JavaPlugin;

public class MagmaRoarPlugin extends JavaPlugin {

    private static MagmaRoarPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        
        // Регистрация обработчиков
        getServer().getPluginManager().registerEvents(new StaffEvents(), this);
        getServer().getPluginManager().registerEvents(new LightMaceHandler(), this);
        getServer().getPluginManager().registerEvents(new FlamingCrossbowHandler(), this); // НОВОЕ
        
        // Команда для Рога Магмы
        getCommand("roar").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(MagmaHornItem.createHorn());
            }
            return true;
        });
        
        // Команда для Легкой Булавы
        getCommand("mace").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(LightMaceItem.createMace());
            }
            return true;
        });
        
        // НОВАЯ КОМАНДА для Пылающего арбалета
        getCommand("flamingbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(FlamingCrossbowItem.createCrossbow());
            }
            return true;
        });
        
        getLogger().info("§aMagmaRoarPlugin включён! Легкая Булава и Пылающий арбалет добавлены.");
    }

    public static MagmaRoarPlugin getInstance() {
        return instance;
    }
}