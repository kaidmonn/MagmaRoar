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
        getServer().getPluginManager().registerEvents(new HypnosisStaffHandler(), this); // Жезл гипноза
        
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
        
        // Команда для Пылающего арбалета
        getCommand("flamingbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(FlamingCrossbowItem.createCrossbow());
            }
            return true;
        });
        
        // Команда для Кровавого меча
        getCommand("blood").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(BloodSwordItem.createBloodSword());
            }
            return true;
        });
        
        // Команды для тестовых зомби
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
        
        // Команда для Морозного меча
        getCommand("frost").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(FrostSwordItem.createFrostSword());
            }
            return true;
        });
        
        // Команда для Теневого меча
        getCommand("shadow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(ShadowSwordItem.createShadowSword());
            }
            return true;
        });
        
        // Команда для Орбитальной пушки
        getCommand("orbital").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(OrbitalCannonItem.createCannon());
            }
            return true;
        });
        
        // Команда для Скалкового арбалета
        getCommand("sculkbow").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(SculkCrossbowItem.createCrossbow());
            }
            return true;
        });
        
        // Команда для Посоха жителя
        getCommand("villagerstaff").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(VillagerStaffItem.createStaff());
            }
            return true;
        });
        
        // Команда для Взрывных зелий
        getCommand("explosivepotion").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(ExplosivePotionItem.createPotion());
            }
            return true;
        });
        
        // Команда для Паучьего клинка
        getCommand("spider").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(SpiderBladeItem.createBlade());
            }
            return true;
        });
        
        // Команда для Мьёльнира
        getCommand("mjolnir").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(MjolnirItem.createMjolnir());
            }
            return true;
        });
        
        // Команда для Жезла гипноза
        getCommand("hypnosis").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).getInventory().addItem(HypnosisStaffItem.createStaff());
            }
            return true;
        });
        
        getLogger().info("§aMagmaRoarPlugin включён! Загружено 14 предметов + взрывные зелья");
    }

    public static MagmaRoarPlugin getInstance() {
        return instance;
    }
}