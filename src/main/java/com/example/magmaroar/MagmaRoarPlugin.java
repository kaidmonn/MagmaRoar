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
        
        // Инициализация менеджеров (Важно: порядок имеет значение)
        this.itemManager = new ItemManager(this);
        this.kitManager = new KitManager(this);
        this.battleManager = new BattleManager(this);
        this.queueManager = new QueueManager(this);
        this.npcManager = new NPCManager(this);
        this.animationChest = new AnimationChest(this);
        
        // Регистрация всех обработчиков событий
        registerEvents();
        
        // Регистрация всех команд
        registerCommands();
        
        getLogger().info("§aMagmaRoarPlugin включён! Все менеджеры и команды загружены.");
    }

    private void registerEvents() {
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
        pm.registerEvents(new VillagerStaffHandler(), this); // Наш исправленный обработчик
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
        pm.registerEvents(new LudoSwordHandler(), this);
        pm.registerEvents(new MirrorSwordHandler(), this);
        pm.registerEvents(new ShrinkerHandler(), this);
        pm.registerEvents(new PoseidonTridentHandler(), this);
        pm.registerEvents(new TimeClockHandler(), this);
        pm.registerEvents(new TimeBowHandler(), this);
        pm.registerEvents(new ArtemisBowHandler(), this);
        pm.registerEvents(new CreationBowHandler(), this);
        pm.registerEvents(new FossilSwordHandler(), this);
        pm.registerEvents(new EventListener(this), this);
        pm.registerEvents(animationChest, this);
    }

    private void registerCommands() {
        // Команда для Посоха Жителя
        getCommand("villagerstaff").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) VillagerStaffItem.giveStaff(player);
            return true;
        });

        // Остальные команды
        getCommand("roar").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(MagmaHornItem.createHorn());
            return true;
        });

        getCommand("mace").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(LightMaceItem.createMace());
            return true;
        });

        getCommand("blood").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) BloodSwordItem.giveBloodSword(player);
            return true;
        });

        getCommand("mitapy").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                npcManager.spawnNPC(player.getLocation());
                player.sendMessage("§aNPC Митапы призван!");
            }
            return true;
        });

        getCommand("giveroll").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("magma.admin")) return true;
            if (args.length < 1) return true;
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                int amount = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
                animationChest.giveRoll(target, amount);
            }
            return true;
        });

        // Регистрация рандомного оружия (вынесено для чистоты)
        setupRandomWeaponCommands();
    }

    private void setupRandomWeaponCommands() {
        getCommand("randomweapon1").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) return true;
            List<ItemStack> weapons = Arrays.asList(
                BloodSwordItem.createBloodSword(), FrostSwordItem.createFrostSword(),
                ShadowSwordItem.createShadowSword(), SpiderBladeItem.createBlade(),
                MjolnirItem.createMjolnir(), StormBladeItem.createBlade(),
                ExcaliburItem.createExcalibur(), KatanaItem.createKatana(),
                LudoSwordItem.createSword(), MirrorSwordItem.createSword(),
                FossilSwordItem.createSword()
            );
            player.getInventory().addItem(weapons.get(new Random().nextInt(weapons.size())).clone());
            return true;
        });
        
        // ... можно добавить randomweapon2 и прочие аналогично
    }

    @Override
    public void onDisable() {
        if (npcManager != null) npcManager.removeAllNPCs();
        getLogger().info("§cMagmaRoarPlugin выключён!");
    }

    // ГЕТТЕРЫ (Критически важны для исправления ошибок компиляции)
    public static MagmaRoarPlugin getInstance() { return instance; }
    public NPCManager getNPCManager() { return npcManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public BattleManager getBattleManager() { return battleManager; }
    public KitManager getKitManager() { return kitManager; }
    public ItemManager getItemManager() { return itemManager; }
    public AnimationChest getAnimationChest() { return animationChest; }
}