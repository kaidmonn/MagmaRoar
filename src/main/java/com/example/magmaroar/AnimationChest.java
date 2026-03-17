package com.example.magmaroar;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AnimationChest implements Listener {

    private final MagmaRoarPlugin plugin;
    private final Map<UUID, Integer> playerRolls = new HashMap<>();
    private final Map<UUID, Integer> playerAnimations = new HashMap<>();
    private final Random random = new Random();
    
    private static final String CHEST_NAME = "§6§lСундук-рулетка";
    private static final int DEFAULT_ROLLS = 1;

    public AnimationChest(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CHEST) return;
        
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Chest)) return;
        
        Chest chest = (Chest) block.getState();
        if (chest.getCustomName() == null || !chest.getCustomName().equals(CHEST_NAME)) return;
        
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        openRollGUI(player);
    }

    private void openRollGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8§lСундук-рулетка");
        
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, glass);
        }
        
        // Информация о крутках
        int rolls = playerRolls.getOrDefault(player.getUniqueId(), 0);
        
        ItemStack infoItem = new ItemStack(Material.CHEST);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§e§lКруток: §f" + rolls);
        List<String> lore = new ArrayList<>();
        lore.add("§7Нажми на сундук, чтобы");
        lore.add("§7потратить 1 крутку");
        lore.add("§7Шанс каждой анимации: §a10%");
        lore.add("");
        
        // Показываем текущую анимацию
        int currentAnim = playerAnimations.getOrDefault(player.getUniqueId(), -1);
        if (currentAnim != -1) {
            lore.add("§aТекущая анимация:");
            lore.add("§f" + getAnimationName(currentAnim));
        } else {
            lore.add("§cУ вас нет выбранной анимации");
        }
        
        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(13, infoItem);
        
        // Кнопка для прокрутки
        if (rolls > 0) {
            ItemStack rollButton = new ItemStack(Material.ENDER_CHEST);
            ItemMeta rollMeta = rollButton.getItemMeta();
            rollMeta.setDisplayName("§a§lНАЖМИ, ЧТОБЫ КРУТНУТЬ!");
            List<String> rollLore = new ArrayList<>();
            rollLore.add("§7Потратить 1 крутку");
            rollLore.add("§7и получить случайную анимацию");
            rollMeta.setLore(rollLore);
            rollButton.setItemMeta(rollMeta);
            gui.setItem(11, rollButton);
        } else {
            ItemStack noRolls = new ItemStack(Material.BARRIER);
            ItemMeta noMeta = noRolls.getItemMeta();
            noMeta.setDisplayName("§c§lНЕТ КРУТОК");
            List<String> noLore = new ArrayList<>();
            noLore.add("§7Получите крутки у администратора");
            noMeta.setLore(noLore);
            noRolls.setItemMeta(noMeta);
            gui.setItem(11, noRolls);
        }
        
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("§8§lСундук-рулетка")) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        if (slot == 11) { // Нажали на кнопку крутки
            performRoll(player);
            player.closeInventory();
        }
    }

    private void performRoll(Player player) {
        UUID uuid = player.getUniqueId();
        int rolls = playerRolls.getOrDefault(uuid, 0);
        
        if (rolls <= 0) {
            player.sendMessage("§cУ вас нет круток!");
            return;
        }
        
        // Тратим крутку
        playerRolls.put(uuid, rolls - 1);
        
        // Выбираем случайную анимацию (1-11)
        int animId = random.nextInt(11) + 1; // 1-11
        
        // Сохраняем новую анимацию (заменяем старую)
        playerAnimations.put(uuid, animId);
        
        // Сообщение игроку
        player.sendMessage("§a§l✦ ВАМ ВЫПАЛА АНИМАЦИЯ! ✦");
        player.sendMessage("§f" + getAnimationName(animId));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Показываем в центре экрана
        player.sendTitle("§6§lРУЛЕТКА", getAnimationName(animId), 10, 40, 10);
    }

    public void giveRoll(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = playerRolls.getOrDefault(uuid, 0);
        playerRolls.put(uuid, current + amount);
        player.sendMessage("§aВы получили " + amount + " круток в сундуке-рулетке!");
    }

    public void triggerKillAnimation(Player killer, Player victim) {
        int animId = playerAnimations.getOrDefault(killer.getUniqueId(), -1);
        if (animId == -1) return;
        
        Location loc = victim.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        // Сообщение убийце
        killer.sendMessage("§6§lАнимация убийства: §f" + getAnimationName(animId));
        
        switch (animId) {
            case 1: // Взрыв внутри жертвы
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
                world.spawnParticle(Particle.EXPLOSION, loc, 10, 1, 1, 1, 0);
                break;
                
            case 2: // Визер-скелеты ходят кругами
                new BukkitRunnable() {
                    int ticks = 0;
                    List<WitherSkeleton> skeletons = new ArrayList<>();
                    
                    @Override
                    public void run() {
                        if (ticks == 0) {
                            for (int i = 0; i < 4; i++) {
                                Location spawnLoc = loc.clone().add(
                                    Math.cos(i * Math.PI/2) * 3,
                                    0,
                                    Math.sin(i * Math.PI/2) * 3
                                );
                                WitherSkeleton skelly = (WitherSkeleton) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
                                skelly.setAI(false);
                                skelly.setInvulnerable(true);
                                skelly.setSilent(true);
                                skeletons.add(skelly);
                            }
                        }
                        
                        if (ticks >= 100) {
                            for (WitherSkeleton skelly : skeletons) {
                                skelly.remove();
                            }
                            this.cancel();
                            return;
                        }
                        
                        for (int i = 0; i < skeletons.size(); i++) {
                            WitherSkeleton skelly = skeletons.get(i);
                            double angle = (ticks * 0.05) + (i * Math.PI/2);
                            Location newLoc = loc.clone().add(
                                Math.cos(angle) * 3,
                                0,
                                Math.sin(angle) * 3
                            );
                            skelly.teleport(newLoc);
                        }
                        
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 3: // Наковальня сверху
                Location anvilLoc = loc.clone().add(0, 10, 0);
                FallingBlock anvil = world.spawnFallingBlock(anvilLoc, Material.ANVIL.createBlockData());
                anvil.setDropItem(false);
                anvil.setHurtEntities(false);
                world.playSound(loc, Sound.ITEM_MACE_SMASH_GROUND, 2.0f, 1.0f);
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        anvil.remove();
                    }
                }.runTaskLater(plugin, 40L);
                break;
                
            case 4: // Эффекты вардена
                world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
                world.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
                break;
                
            case 5: // Курицы и тортик
                for (int i = 0; i < 5; i++) {
                    Chicken chicken = (Chicken) world.spawnEntity(loc.clone().add(random.nextInt(3)-1, 0, random.nextInt(3)-1), EntityType.CHICKEN);
                    chicken.setInvulnerable(true);
                    chicken.setAI(false);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            chicken.remove();
                        }
                    }.runTaskLater(plugin, 100L);
                }
                
                Location cakeLoc = loc.clone().add(0, 1, 0);
                cakeLoc.getBlock().setType(Material.CAKE);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (cakeLoc.getBlock().getType() == Material.CAKE) {
                            cakeLoc.getBlock().setType(Material.AIR);
                        }
                    }
                }.runTaskLater(plugin, 100L);
                break;
                
            case 6: // Фейерверки
                new BukkitRunnable() {
                    int count = 0;
                    @Override
                    public void run() {
                        if (count >= 5) {
                            this.cancel();
                            return;
                        }
                        
                        Firework firework = world.spawn(loc.clone().add(random.nextInt(3)-1, 1, random.nextInt(3)-1), Firework.class);
                        FireworkMeta meta = firework.getFireworkMeta();
                        meta.addEffect(FireworkEffect.builder()
                            .withColor(Color.RED, Color.BLUE, Color.GREEN)
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .build());
                        firework.setFireworkMeta(meta);
                        firework.detonate();
                        
                        count++;
                    }
                }.runTaskTimer(plugin, 0L, 5L);
                break;
                
            case 7: // Молния
                world.strikeLightningEffect(loc);
                break;
                
            case 8: // Дождь (визуально)
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 100) {
                            this.cancel();
                            return;
                        }
                        world.spawnParticle(Particle.RAIN, loc.clone().add(random.nextInt(5)-2, 2, random.nextInt(5)-2), 5, 0.5, 0.5, 0.5, 0);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
                
            case 9: // Крест из замшелого булыжника
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (Math.abs(x) == 1 && Math.abs(z) == 1) continue;
                        Location blockLoc = loc.clone().add(x, 0, z);
                        blockLoc.getBlock().setType(Material.MOSSY_COBBLESTONE);
                        
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (blockLoc.getBlock().getType() == Material.MOSSY_COBBLESTONE) {
                                    blockLoc.getBlock().setType(Material.AIR);
                                }
                            }
                        }.runTaskLater(plugin, 100L);
                    }
                }
                break;
                
            case 10: // Варден вылезает и залезает
                Location wardenLoc = loc.clone().add(0, -1, 0);
                Warden warden = (Warden) world.spawnEntity(wardenLoc, EntityType.WARDEN);
                warden.setAI(false);
                warden.setInvulnerable(true);
                warden.setSilent(true);
                
                new BukkitRunnable() {
                    int y = 0;
                    @Override
                    public void run() {
                        if (y >= 3) {
                            new BukkitRunnable() {
                                int downY = 2;
                                @Override
                                public void run() {
                                    if (downY < 0) {
                                        warden.remove();
                                        this.cancel();
                                        return;
                                    }
                                    warden.teleport(loc.clone().add(0, downY, 0));
                                    downY--;
                                }
                            }.runTaskTimer(plugin, 0L, 2L);
                            this.cancel();
                            return;
                        }
                        warden.teleport(loc.clone().add(0, y, 0));
                        y++;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
                break;
                
            case 11: // Метеорит
                Location fireballLoc = loc.clone().add(0, 20, 0);
                Fireball fireball = world.spawn(fireballLoc, Fireball.class);
                fireball.setVelocity(new Vector(0, -1, 0));
                fireball.setYield(0);
                fireball.setIsIncendiary(false);
                
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 40) {
                            fireball.remove();
                            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
                            world.spawnParticle(Particle.EXPLOSION, loc, 20, 2, 2, 2, 0);
                            this.cancel();
                            return;
                        }
                        
                        for (int i = 0; i < 5; i++) {
                            double angle = (ticks * 0.5) + (i * Math.PI/2.5);
                            double radius = 2;
                            world.spawnParticle(Particle.FLAME, 
                                fireballLoc.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius),
                                1, 0, 0, 0, 0);
                        }
                        
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;
        }
    }

    private String getAnimationName(int id) {
        switch (id) {
            case 1: return "§cВзрыв внутри";
            case 2: return "§8Визер-скелеты";
            case 3: return "§7Наковальня";
            case 4: return "§3Варден-выстрел";
            case 5: return "§eКурицы и тортик";
            case 6: return "§aФейерверки";
            case 7: return "§bМолния";
            case 8: return "§9Дождь";
            case 9: return "§8Крест из камня";
            case 10: return "§2Варден";
            case 11: return "§6Метеорит";
            default: return "§7Неизвестно";
        }
    }

    public int getPlayerAnimation(Player player) {
        return playerAnimations.getOrDefault(player.getUniqueId(), -1);
    }
}