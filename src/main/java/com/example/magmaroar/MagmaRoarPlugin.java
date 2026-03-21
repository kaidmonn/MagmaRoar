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
        
        // РЕГИСТРАЦИЯ ОБРАБОТЧИКОВ (Events)
        getServer().getPluginManager().registerEvents(new StaffEvents(), this);
        getServer().getPluginManager().registerEvents(new LightMaceHandler(), this);
        getServer().getPluginManager().registerEvents(new FlamingCrossbowHandler(), this);
        getServer().getPluginManager().registerEvents(new BloodSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new TestZombieHandler(), this);
        getServer().getPluginManager().registerEvents(new FrostSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new ShadowSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new OrbitalCannonHandler(), this);
        getServer().getPluginManager().registerEvents(new SculkCrossbowHandler(), this);
        
        // Тот самый обработчик, который мы починили:
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
        getServer().getPluginManager().registerEvents(new MirrorSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new ShrinkerHandler(), this);
        getServer().getPluginManager().registerEvents(new PoseidonTridentHandler(), this);
        getServer().getPluginManager().registerEvents(new TimeClockHandler(), this);
        getServer().getPluginManager().registerEvents(new TimeBowHandler(), this);
        getServer().getPluginManager().registerEvents(new ArtemisBowHandler(), this);
        getServer().getPluginManager().registerEvents(new CreationBowHandler(), this);
        getServer().getPluginManager().registerEvents(new FossilSwordHandler(), this);
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getServer().getPluginManager().registerEvents(animationChest, this);
        
        // КОМАНДЫ (Executors)
        
        // Посох жителя
        getCommand("villagerstaff").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                VillagerStaffItem.giveStaff(player);
            }
            return true;
        });

        getCommand("roar").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(MagmaHornItem.createHorn());
            return true;
        });
        
        getCommand("mace").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(LightMaceItem.createMace());
            return true;
        });
        
        getCommand("flamingbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(FlamingCrossbowItem.createCrossbow());
            return true;
        });
        
        getCommand("blood").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) BloodSwordItem.giveBloodSword(player);
            return true;
        });
        
        getCommand("zombietotem").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(TestZombieItem.createZombieEgg(true));
            return true;
        });
        
        getCommand("zombieshield").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(TestZombieItem.createZombieEgg(false));
            return true;
        });
        
        getCommand("frost").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(FrostSwordItem.createFrostSword());
            return true;
        });
        
        getCommand("shadow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(ShadowSwordItem.createShadowSword());
            return true;
        });
        
        getCommand("orbital").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(OrbitalCannonItem.createCannon());
            return true;
        });
        
        getCommand("sculkbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(SculkCrossbowItem.createCrossbow());
            return true;
        });
        
        getCommand("explosivepotion").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(ExplosivePotionItem.createPotion());
            return true;
        });
        
        getCommand("spider").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(SpiderBladeItem.createBlade());
            return true;
        });
        
        getCommand("mjolnir").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(MjolnirItem.createMjolnir());
            return true;
        });
        
        getCommand("hypnosis").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(HypnosisStaffItem.createStaff());
            return true;
        });
        
        getCommand("scythe").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(DeathScytheItem.createScythe());
            return true;
        });
        
        getCommand("ravager").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(RavagerHornItem.createHorn());
            return true;
        });
        
        getCommand("meteor").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(HellMeteorItem.createMeteor());
            return true;
        });
        
        getCommand("laser").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(LaserItem.createLaser());
            return true;
        });
        
        getCommand("storm").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(StormBladeItem.createBlade());
            return true;
        });
        
        getCommand("excalibur").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(ExcaliburItem.createExcalibur());
            return true;
        });
        
        getCommand("katana").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(KatanaItem.createKatana());
            return true;
        });
        
        getCommand("reaper").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(ReaperScytheItem.createScythe());
            return true;
        });
        
        getCommand("ludo").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                player.getInventory().addItem(LudoSwordItem.createSword());
                player.sendMessage("§aВы получили Лудо-меч!");
            }
            return true;
        });
        
        getCommand("mirror").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                player.getInventory().addItem(MirrorSwordItem.createSword());
                player.sendMessage("§aВы получили Зеркальный меч!");
            }
            return true;
        });
        
        getCommand("shrinker").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                player.getInventory().addItem(ShrinkerItem.createShrinker());
                player.sendMessage("§aВы получили Уменьшитель!");
            }
            return true;
        });
        
        getCommand("poseidon").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                player.getInventory().addItem(PoseidonTridentItem.createTrident());
                player.sendMessage("§aВы получили Трезубец Посейдона!");
            }
            return true;
        });
        
        getCommand("timeclock").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(TimeClockItem.createClock());
            return true;
        });
        
        getCommand("timebow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(TimeBowItem.createBow());
            return true;
        });
        
        getCommand("artemis").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(ArtemisBowItem.createBow());
            return true;
        });
        
        getCommand("creationbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(CreationBowItem.createBow());
            return true;
        });
        
        getCommand("fossil").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) player.getInventory().addItem(FossilSwordItem.createSword());
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
                try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage("§cНеверное количество!"); return true; }
            }
            animationChest.giveRoll(target, amount);
            sender.sendMessage("§aВыдано " + amount + " круток игроку " + target.getName());
            return true;
        });

        // Команды для рандомного оружия
        setupRandomWeaponCommands();

        getLogger().info("§aMagmaRoarPlugin включён! Загружено 30+ предметов.");
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
            player.sendMessage("§aВы получили рандомное оружие!");
            return true;
        });

        getCommand("randomweapon2").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) return true;
            List<ItemStack> weapons = Arrays.asList(
                LightMaceItem.createMace(), OrbitalCannonItem.createCannon(),
                SculkCrossbowItem.createCrossbow(), VillagerStaffItem.createStaff(),
                DeathScytheItem.createScythe(), HellMeteorItem.createMeteor(),
                ReaperScytheItem.createScythe(), TimeClockItem.createClock(),
                TimeBowItem.createBow(), ArtemisBowItem.createBow()
            );
            player.getInventory().addItem(weapons.get(new Random().nextInt(weapons.size())).clone());
            player.sendMessage("§aВы получили рандомное оружие!");
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
    public AnimationChest getAnimationChest() { return animationChest; }
}