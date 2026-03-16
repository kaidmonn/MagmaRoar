package com.example.magmaroar;

import org.bukkit.plugin.java.JavaPlugin;

public class MythicNPC extends JavaPlugin {
    
    private static MythicNPC instance;
    private NPCManager npcManager;
    private QueueManager queueManager;
    private BattleManager battleManager;
    private KitManager kitManager;
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        instance = this;
        
        npcManager = new NPCManager(this);
        queueManager = new QueueManager(this);
        battleManager = new BattleManager(this);
        kitManager = new KitManager(this);
        itemManager = new ItemManager(this);
        
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        
        getCommand("mitapy").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                npcManager.spawnNPC(player.getLocation());
                player.sendMessage("§aNPC Митапы призван!");
            }
            return true;
        });
        
        getLogger().info("MythicNPC включён!");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) npcManager.removeAllNPCs();
        getLogger().info("MythicNPC выключён!");
    }

    public static MythicNPC getInstance() { return instance; }
    public NPCManager getNPCManager() { return npcManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public BattleManager getBattleManager() { return battleManager; }
    public KitManager getKitManager() { return kitManager; }
    public ItemManager getItemManager() { return itemManager; }
}