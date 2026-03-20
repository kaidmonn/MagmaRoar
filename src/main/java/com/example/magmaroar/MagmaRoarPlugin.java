package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MagmaRoarPlugin extends JavaPlugin {

    private static MagmaRoarPlugin instance;
    
    private NPCManager npcManager;
    private QueueManager queueManager;
    private BattleManager battleManager;
    private KitManager kitManager;
    private ItemManager itemManager;
    private AnimationChest animationChest;

    @Override
    public void onEnable() {
        instance = this;
        
        // Инициализация менеджеров
        itemManager = new ItemManager(this);
        npcManager = new NPCManager(this);
        queueManager = new QueueManager(this);
        battleManager = new BattleManager(this);
        kitManager = new KitManager(this);
        animationChest = new AnimationChest(this);
        
        // Регистрация всех обработчиков событий (Handlers)
        registerAllEvents();
        
        // Регистрация команд
        registerCommands();
        
        getLogger().info("§aMagmaRoarPlugin включён! Модель Лудо-меча (1004) активна.");
    }

    private void registerAllEvents() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new StaffEvents(), this);
        pm.registerEvents(new LightMaceHandler(), this);
        pm.registerEvents(new FlamingCrossbowHandler(), this);
        pm.registerEvents(new BloodSwordHandler(), this);
        pm.registerEvents(new TestZombieHandler(), this);
        pm.registerEvents(new FrostSwordHandler(), this);
        pm.registerEvents(new ShadowSwordHandler(), this);
        pm.registerEvents(new OrbitalCannonHandler(), this);
        pm.registerEvents(new SculkCrossbowHandler(), this);
        pm.registerEvents(new VillagerStaffHandler(), this);
        pm.registerEvents(new ExplosivePotionHandler(), this);
        pm.registerEvents(new SpiderBladeHandler(), this);
        pm.registerEvents(new MjolnirHandler(), this);
        pm.registerEvents(new HypnosisStaffHandler(), this);
        pm.registerEvents(new DeathScytheHandler(), this);
        pm.registerEvents(new RavagerHornHandler(), this);
        pm.registerEvents(new HellMeteorHandler(), this);
        pm.registerEvents(new LaserHandler(), this);
        pm.registerEvents(new StormBladeHandler(), this);
        pm.registerEvents(new ExcaliburHandler(), this);
        pm.registerEvents(new KatanaHandler(), this);
        pm.registerEvents(new ReaperScytheHandler(), this);
        pm.registerEvents(new LudoSwordHandler(), this); // Тот самый обработчик
        pm.registerEvents(new TimeClockHandler(), this);
        pm.registerEvents(new TimeBowHandler(), this);
        pm.registerEvents(new ArtemisBowHandler(), this);
        pm.registerEvents(new CreationBowHandler(), this);
        pm.registerEvents(new FossilSwordHandler(), this);
        pm.registerEvents(new EventListener(this), this);
        pm.registerEvents(animationChest, this);
    }

    private void registerCommands() {
        // Команда для получения Лудо-меча (с моделью 1004)
        getCommand("ludo").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                player.getInventory().addItem(LudoSwordItem.createSword());
                player.sendMessage("§a§l[!] §fВы получили §5§lЛудо-меч §7(Model: 1004)");
            }
            return true;
        });

        // Остальные команды предметов
        getCommand("roar").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(MagmaHornItem.createHorn());
            return true;
        });

        getCommand("mace").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(LightMaceItem.createMace());
            return true;
        });

        getCommand("frost").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(FrostSwordItem.createFrostSword());
            return true;
        });

        getCommand("shadow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(ShadowSwordItem.createShadowSword());
            return true;
        });

        getCommand("spider").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(SpiderBladeItem.createBlade());
            return true;
        });

        getCommand("mjolnir").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(MjolnirItem.createMjolnir());
            return true;
        });

        getCommand("scythe").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(DeathScytheItem.createScythe());
            return true;
        });

        getCommand("storm").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(StormBladeItem.createBlade());
            return true;
        });

        getCommand("excalibur").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(ExcaliburItem.createExcalibur());
            return true;
        });

        getCommand("katana").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(KatanaItem.createKatana());
            return true;
        });

        getCommand("reaper").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) p.getInventory().addItem(ReaperScytheItem.createScythe());
            return true;
        });

        getCommand("mitapy").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                npcManager.spawnNPC(player.getLocation());
                player.sendMessage("§aNPC Митапы призван!");
            }
            return true;
        });

        // Команда для выдачи круток
        getCommand("giveroll").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("magma.admin")) return true;
            if (args.length < 1) return true;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) return true;
            int amount = (args.length >= 2) ? Integer.parseInt(args[1]) : 1;
            animationChest.giveRoll(target, amount);
            sender.sendMessage("§aВыдано " + amount + " круток игроку " + target.getName());
            return true;
        });
    }

    @Override
    public void onDisable() {
        if (npcManager != null) npcManager.removeAllNPCs();
        getLogger().info("§cMagmaRoarPlugin выключён!");
    }

    public static MagmaRoarPlugin getInstance() { return instance; }
    
    public NPCManager getNPCManager() { return npcManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public BattleManager getBattleManager() { return battleManager; }
    public KitManager getKitManager() { return kitManager; }
    public ItemManager getItemManager() { return itemManager; }
    public AnimationChest getAnimationChest() { return animationChest; }
}