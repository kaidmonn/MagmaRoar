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
        
        itemManager = new ItemManager(this);
        npcManager = new NPCManager(this);
        queueManager = new QueueManager(this);
        battleManager = new BattleManager(this);
        kitManager = new KitManager(this);
        animationChest = new AnimationChest(this);
        
        // Регистрация обработчиков
        getServer().getPluginManager().registerEvents(new StaffEvents(), this);
        getServer().getPluginManager().registerEvents(new LightMaceHandler(), this);
        getServer().getPluginManager().registerEvents(new FlamingCrossbowHandler(), this);
        getServer().getPluginManager().registerEvents(new BloodSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new TestZombieHandler(), this);
        getServer().getPluginManager().registerEvents(new FrostSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new ShadowSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new OrbitalCannonHandler(), this);
        getServer().getPluginManager().registerEvents(new SculkCrossbowHandler(), this);
        getServer().getPluginManager().registerEvents(new VillagerStaffHandler(), this);
        getServer().getPluginManager().registerEvents(new ExplosivePotionHandler(), this);
        getServer().getPluginManager().registerEvents(new SpiderBladeHandler(), this);
        getServer().getPluginManager().registerEvents(new MjolnirHandler(), this);
        getServer().getPluginManager().registerEvents(new HypnosisStaffHandler(), this);
        getServer().getPluginManager().registerEvents(new DeathScytheHandler(), this);
        getServer().getPluginManager().registerEvents(new RavagerHornHandler(), this);
        getServer().getPluginManager().registerEvents(new HellMeteorHandler(), this);
        getServer().getPluginManager().registerEvents(new LaserHandler(), this);
        getServer().getPluginManager().registerEvents(new StormBladeHandler(), this);
        getServer().getPluginManager().registerEvents(new ExcaliburHandler(), this);
        getServer().getPluginManager().registerEvents(new KatanaHandler(), this);
        getServer().getPluginManager().registerEvents(new ReaperScytheHandler(), this);
        getServer().getPluginManager().registerEvents(new LudoSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new TimeClockHandler(), this);
        getServer().getPluginManager().registerEvents(new TimeBowHandler(), this);
        getServer().getPluginManager().registerEvents(new ArtemisBowHandler(), this);
        getServer().getPluginManager().registerEvents(new CreationBowHandler(), this);
        getServer().getPluginManager().registerEvents(new FossilSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getServer().getPluginManager().registerEvents(animationChest, this);
        
        // Команды для предметов
        getCommand("roar").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(MagmaHornItem.createHorn());
            }
            return true;
        });
        
        getCommand("mace").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(LightMaceItem.createMace());
            }
            return true;
        });
        
        getCommand("flamingbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(FlamingCrossbowItem.createCrossbow());
            }
            return true;
        });
        
        // КРОВАВЫЙ МЕЧ
        getCommand("blood").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                BloodSwordItem.giveBloodSword(player);
            }
            return true;
        });
        
        getCommand("zombietotem").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(TestZombieItem.createZombieEgg(true));
            }
            return true;
        });
        
        getCommand("zombieshield").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(TestZombieItem.createZombieEgg(false));
            }
            return true;
        });
        
        getCommand("frost").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(FrostSwordItem.createFrostSword());
            }
            return true;
        });
        
        getCommand("shadow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(ShadowSwordItem.createShadowSword());
            }
            return true;
        });
        
        getCommand("orbital").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(OrbitalCannonItem.createCannon());
            }
            return true;
        });
        
        getCommand("sculkbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(SculkCrossbowItem.createCrossbow());
            }
            return true;
        });
        
        getCommand("villagerstaff").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(VillagerStaffItem.createStaff());
            }
            return true;
        });
        
        getCommand("explosivepotion").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(ExplosivePotionItem.createPotion());
            }
            return true;
        });
        
        getCommand("spider").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(SpiderBladeItem.createBlade());
            }
            return true;
        });
        
        getCommand("mjolnir").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(MjolnirItem.createMjolnir());
            }
            return true;
        });
        
        getCommand("hypnosis").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(HypnosisStaffItem.createStaff());
            }
            return true;
        });
        
        getCommand("scythe").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(DeathScytheItem.createScythe());
            }
            return true;
        });
        
        getCommand("ravager").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(RavagerHornItem.createHorn());
            }
            return true;
        });
        
        getCommand("meteor").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(HellMeteorItem.createMeteor());
            }
            return true;
        });
        
        getCommand("laser").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(LaserItem.createLaser());
            }
            return true;
        });
        
        getCommand("storm").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(StormBladeItem.createBlade());
            }
            return true;
        });
        
        getCommand("excalibur").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(ExcaliburItem.createExcalibur());
            }
            return true;
        });
        
        getCommand("katana").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(KatanaItem.createKatana());
            }
            return true;
        });
        
        getCommand("reaper").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(ReaperScytheItem.createScythe());
            }
            return true;
        });
        
        // ЛУДО-МЕЧ - РАБОЧАЯ ВЕРСИЯ
        getCommand("ludo").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                player.getInventory().addItem(LudoSwordItem.createSword());
                player.sendMessage("§aВы получили Лудо-меч!");
            }
            return true;
        });
        
        getCommand("timeclock").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(TimeClockItem.createClock());
            }
            return true;
        });
        
        getCommand("timebow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(TimeBowItem.createBow());
            }
            return true;
        });
        
        getCommand("artemis").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(ArtemisBowItem.createBow());
            }
            return true;
        });
        
        getCommand("creationbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(CreationBowItem.createBow());
            }
            return true;
        });
        
        getCommand("fossil").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(FossilSwordItem.createSword());
            }
            return true;
        });
        
        getCommand("mitapy").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                npcManager.spawnNPC(player.getLocation());
                player.sendMessage("§aNPC Митапы призван!");
            }
            return true;
        });
        
        // КОМАНДА ДЛЯ ВЫДАЧИ КРУТОК
        getCommand("giveroll").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("magma.admin")) {
                sender.sendMessage("§cУ вас нет прав!");
                return true;
            }
            
            if (args.length < 1) {
                sender.sendMessage("§cИспользование: /giveroll <игрок> [количество]");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден!");
                return true;
            }
            
            int amount = 1;
            if (args.length >= 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cНеверное количество!");
                    return true;
                }
            }
            
            animationChest.giveRoll(target, amount);
            sender.sendMessage("§aВыдано " + amount + " круток игроку " + target.getName());
            return true;
        });
        
        // Рандомные команды
        getCommand("randomweapon1").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player)) return true;
            
            List<ItemStack> weapons = Arrays.asList(
                FrostSwordItem.createFrostSword(),
                ShadowSwordItem.createShadowSword(),
                SpiderBladeItem.createBlade(),
                MjolnirItem.createMjolnir(),
                StormBladeItem.createBlade(),
                ExcaliburItem.createExcalibur(),
                KatanaItem.createKatana(),
                LudoSwordItem.createSword(),
                FossilSwordItem.createSword()
            );
            
            Random random = new Random();
            ItemStack randomWeapon = weapons.get(random.nextInt(weapons.size())).clone();
            ((org.bukkit.entity.Player) sender).getInventory().addItem(randomWeapon);
            ((org.bukkit.entity.Player) sender).sendMessage("§aВы получили рандомное оружие!");
            return true;
        });
        
        getCommand("randomweapon2").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player)) return true;
            
            List<ItemStack> weapons = Arrays.asList(
                LightMaceItem.createMace(),
                OrbitalCannonItem.createCannon(),
                SculkCrossbowItem.createCrossbow(),
                VillagerStaffItem.createStaff(),
                DeathScytheItem.createScythe(),
                HellMeteorItem.createMeteor(),
                ReaperScytheItem.createScythe(),
                TimeClockItem.createClock(),
                TimeBowItem.createBow(),
                ArtemisBowItem.createBow()
            );
            
            Random random = new Random();
            ItemStack randomWeapon = weapons.get(random.nextInt(weapons.size())).clone();
            ((org.bukkit.entity.Player) sender).getInventory().addItem(randomWeapon);
            ((org.bukkit.entity.Player) sender).sendMessage("§aВы получили рандомное оружие!");
            return true;
        });
        
        getCommand("randomweaponall1").setExecutor((sender, command, label, args) -> {
            List<ItemStack> weapons = Arrays.asList(
                FrostSwordItem.createFrostSword(),
                ShadowSwordItem.createShadowSword(),
                SpiderBladeItem.createBlade(),
                MjolnirItem.createMjolnir(),
                StormBladeItem.createBlade(),
                ExcaliburItem.createExcalibur(),
                KatanaItem.createKatana(),
                LudoSwordItem.createSword(),
                FossilSwordItem.createSword()
            );
            
            Random random = new Random();
            
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                ItemStack randomWeapon = weapons.get(random.nextInt(weapons.size())).clone();
                player.getInventory().addItem(randomWeapon);
                player.sendMessage("§aВы получили рандомное оружие!");
            }
            
            sender.sendMessage("§aРандомное оружие выдано всем игрокам!");
            return true;
        });
        
        getCommand("randomweaponall2").setExecutor((sender, command, label, args) -> {
            List<ItemStack> weapons = Arrays.asList(
                LightMaceItem.createMace(),
                OrbitalCannonItem.createCannon(),
                SculkCrossbowItem.createCrossbow(),
                VillagerStaffItem.createStaff(),
                DeathScytheItem.createScythe(),
                HellMeteorItem.createMeteor(),
                ReaperScytheItem.createScythe(),
                TimeClockItem.createClock(),
                TimeBowItem.createBow(),
                ArtemisBowItem.createBow()
            );
            
            Random random = new Random();
            
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                ItemStack randomWeapon = weapons.get(random.nextInt(weapons.size())).clone();
                player.getInventory().addItem(randomWeapon);
                player.sendMessage("§aВы получили рандомное оружие!");
            }
            
            sender.sendMessage("§aРандомное оружие выдано всем игрокам!");
            return true;
        });
        
        getLogger().info("§aMagmaRoarPlugin включён! Загружено 30+ предметов, NPC Митапы и Сундук-рулетка");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.removeAllNPCs();
        }
        getLogger().info("§cMagmaRoarPlugin выключён!");
    }

    public static MagmaRoarPlugin getInstance() {
        return instance;
    }
    
    public NPCManager getNPCManager() { return npcManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public BattleManager getBattleManager() { return battleManager; }
    public KitManager getKitManager() { return kitManager; }
    public ItemManager getItemManager() { return itemManager; }
    public AnimationChest getAnimationChest() { return animationChest; }
}