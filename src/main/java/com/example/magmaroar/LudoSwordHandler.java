package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class LudoSwordHandler implements Listener {

    private final Map<UUID, LudoStats> stats = new HashMap<>();
    private final Random random = new Random();
    
    private enum LudoMode {
        NONE,
        FROST, SHADOW, SPIDER, MJOLNIR, DEATH_SCYTHE,
        STORM, REAPER, DRAGON, EXCALIBUR, LIGHT_MACE, JACKPOT
    }
    
    private static class LudoStats {
        LudoMode currentMode = LudoMode.NONE;
        long modeEndTime = 0;
        int hitsLeft = 0;
        int frostHits = 0;
        boolean isInvulnerable = false;
        boolean isRolling = false;
        int slot = -1;
        ItemStack originalItem = null;
    }
    
    private static final Map<LudoMode, Integer> DURATIONS = new HashMap<>();
    private static final Map<LudoMode, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<LudoMode, String> MODE_NAMES = new HashMap<>();
    
    static {
        DURATIONS.put(LudoMode.FROST, 30);
        DURATIONS.put(LudoMode.SHADOW, 30);
        DURATIONS.put(LudoMode.SPIDER, 30);
        DURATIONS.put(LudoMode.MJOLNIR, 30);
        DURATIONS.put(LudoMode.DEATH_SCYTHE, 30);
        DURATIONS.put(LudoMode.STORM, 30);
        DURATIONS.put(LudoMode.REAPER, 30);
        DURATIONS.put(LudoMode.DRAGON, 30);
        DURATIONS.put(LudoMode.EXCALIBUR, 30);
        DURATIONS.put(LudoMode.LIGHT_MACE, 30);
        DURATIONS.put(LudoMode.JACKPOT, 40);
        
        MODE_NAMES.put(LudoMode.FROST, "§b❄️ Морозный меч");
        MODE_NAMES.put(LudoMode.SHADOW, "§8🌑 Теневой меч");
        MODE_NAMES.put(LudoMode.SPIDER, "§2🕷️ Паучий клинок");
        MODE_NAMES.put(LudoMode.MJOLNIR, "§e⚡ Мьёльнир");
        MODE_NAMES.put(LudoMode.DEATH_SCYTHE, "§c💀 Коса смерти");
        MODE_NAMES.put(LudoMode.STORM, "§9🌪️ Клинок бури");
        MODE_NAMES.put(LudoMode.REAPER, "§5💀 Коса жнеца");
        MODE_NAMES.put(LudoMode.DRAGON, "§d🐉 Катана дракона");
        MODE_NAMES.put(LudoMode.EXCALIBUR, "§6⚔️ Экскалибур");
        MODE_NAMES.put(LudoMode.LIGHT_MACE, "§f🏏 Легкая булава");
        MODE_NAMES.put(LudoMode.JACKPOT, "§d§l💰 ДЖЕКПОТ");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isLudoSword(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            LudoStats playerStats = stats.computeIfAbsent(player.getUniqueId(), k -> new LudoStats());
            
            if (playerStats.isRolling) {
                player.sendMessage("§cРулетка уже крутится!");
                event.setCancelled(true);
                return;
            }
            
            if (playerStats.currentMode != LudoMode.NONE) {
                player.sendMessage("§cЛудо-меч уже активен!");
                event.setCancelled(true);
                return;
            }
            
            playerStats.slot = player.getInventory().getHeldItemSlot();
            playerStats.originalItem = item.clone();
            playerStats.isRolling = true;
            
            startRoulette(player);
            event.setCancelled(true);
        }
    }

    private void startRoulette(Player player) {
        player.sendMessage("§6§l🔄 ЛУДО-МЕЧ: КРУТИТСЯ РУЛЕТКА...");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40) {
                    
                    LudoStats playerStats = stats.get(player.getUniqueId());
                    if (playerStats != null) {
                        playerStats.isRolling = false;
                    }
                    
                    LudoMode selected = selectRandomMode();
                    player.sendMessage("§6§l═══════════════════════");
                    player.sendMessage("§6§l  ВЫПАЛО: " + MODE_NAMES.get(selected));
                    player.sendMessage("§6§l═══════════════════════");
                    
                    playModeSound(player, selected);
                    activateMode(player, selected);
                    
                    this.cancel();
                    return;
                }
                
                if (ticks % 4 == 0) {
                    LudoMode randomMode = getRandomMode();
                    player.sendMessage("§8> " + MODE_NAMES.get(randomMode));
                }
                
                if (ticks % 8 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                }
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private LudoMode selectRandomMode() {
        double r = random.nextDouble() * 100;
        
        if (r < 5) {
            return LudoMode.JACKPOT;
        } else {
            int index = (int) ((r - 5) / 9.5);
            LudoMode[] modes = {
                LudoMode.FROST, LudoMode.SHADOW, LudoMode.SPIDER, LudoMode.MJOLNIR,
                LudoMode.DEATH_SCYTHE, LudoMode.STORM, LudoMode.REAPER, LudoMode.DRAGON,
                LudoMode.EXCALIBUR, LudoMode.LIGHT_MACE
            };
            return modes[Math.min(index, 9)];
        }
    }

    private LudoMode getRandomMode() {
        LudoMode[] modes = LudoMode.values();
        return modes[random.nextInt(modes.length)];
    }

    private void playModeSound(Player player, LudoMode mode) {
        if (mode == LudoMode.JACKPOT) {
            player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2.0f, 1.2f);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private void activateMode(Player player, LudoMode mode) {
        LudoStats playerStats = stats.get(player.getUniqueId());
        playerStats.currentMode = mode;
        
        // Меняем предмет
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem != null && isLudoSword(handItem)) {
            ItemMeta meta = handItem.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.Component.text(MODE_NAMES.get(mode)));
                handItem.setItemMeta(meta);
            }
        }
        
        long now = System.currentTimeMillis();
        
        switch (mode) {
            case FROST:
                playerStats.modeEndTime = now + 30000;
                playerStats.frostHits = 0;
                player.sendMessage("§bМорозный меч: замедление при ударе, заморозка после 8 ударов (30 сек)");
                break;
                
            case SHADOW:
                playerStats.modeEndTime = now + 30000;
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 600, 0, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 2, true, true, true));
                player.sendMessage("§8Теневой меч: невидимость + скорость 3 (30 сек)");
                break;
                
            case SPIDER:
                playerStats.modeEndTime = now + 30000;
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAVING, 600, 0, true, true, true));
                spawnSpiderWeb(player.getLocation());
                player.sendMessage("§2Паучий клинок: удары отравляют (30 сек)");
                break;
                
            case MJOLNIR:
                playerStats.modeEndTime = now + 30000;
                player.sendMessage("§eМьёльнир: ПКМ для броска молота (30 сек)");
                break;
                
            case DEATH_SCYTHE:
                playerStats.hitsLeft = 1;
                player.sendMessage("§cКоса смерти: следующий удар вампиризм 5♥ (30 сек)");
                break;
                
            case STORM:
                playerStats.modeEndTime = now + 30000;
                playerStats.hitsLeft = 3;
                player.sendMessage("§9Клинок бури: 3 заряда подброса + молния (30 сек)");
                break;
                
            case REAPER:
                playerStats.hitsLeft = 1;
                player.sendMessage("§5Коса жнеца: следующий удар крадёт все положительные эффекты (30 сек)");
                break;
                
            case DRAGON:
                playerStats.hitsLeft = 2;
                player.sendMessage("§dКатана дракона: ПКМ телепортация (2 заряда, 30 сек)");
                break;
                
            case EXCALIBUR:
                playerStats.hitsLeft = 15;
                playerStats.isInvulnerable = true;
                player.sendMessage("§6Экскалибур: неуязвимость на 15 ударов (30 сек)");
                break;
                
            case LIGHT_MACE:
                playerStats.hitsLeft = 3;
                player.setVelocity(player.getVelocity().add(new Vector(0, 1.5, 0)));
                player.sendMessage("§fЛегкая булава: 3 заряда подброса (30 сек)");
                break;
                
            case JACKPOT:
                playerStats.modeEndTime = now + 40000;
                playerStats.isInvulnerable = true;
                player.sendMessage("§d§l💰 ДЖЕКПОТ! ПОЛНАЯ НЕУЯЗВИМОСТЬ 40 СЕКУНД!");
                startJackpotEffects(player);
                break;
        }
        
        if (mode != LudoMode.DRAGON && mode != LudoMode.DEATH_SCYTHE && mode != LudoMode.REAPER) {
            startModeTimer(player, mode);
        }
    }

    private void startModeTimer(Player player, LudoMode mode) {
        int duration = (mode == LudoMode.JACKPOT) ? 40 : 30;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                LudoStats playerStats = stats.get(player.getUniqueId());
                if (playerStats != null && playerStats.currentMode == mode) {
                    endMode(player);
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), duration * 20L);
    }

    private void endMode(Player player) {
        LudoStats playerStats = stats.get(player.getUniqueId());
        if (playerStats == null || playerStats.currentMode == LudoMode.NONE) return;
        
        LudoMode mode = playerStats.currentMode;
        
        // Возвращаем оригинальный предмет
        if (playerStats.originalItem != null && playerStats.slot >= 0) {
            player.getInventory().setItem(playerStats.slot, playerStats.originalItem);
        }
        
        playerStats.currentMode = LudoMode.NONE;
        playerStats.hitsLeft = 0;
        playerStats.isInvulnerable = false;
        playerStats.frostHits = 0;
        
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.WEAVING);
        
        player.sendMessage("§cЭффект " + MODE_NAMES.get(mode) + " закончился.");
    }

    private void spawnSpiderWeb(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.sqrt(x*x + z*z) <= 2.5) {
                    Location webLoc = center.clone().add(x, 0, z);
                    if (webLoc.getBlock().getType() == Material.AIR) {
                        webLoc.getBlock().setType(Material.COBWEB);
                        
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (webLoc.getBlock().getType() == Material.COBWEB) {
                                    webLoc.getBlock().setType(Material.AIR);
                                }
                            }
                        }.runTaskLater(MagmaRoarPlugin.getInstance(), 300L);
                    }
                }
            }
        }
    }

    private void startJackpotEffects(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                LudoStats playerStats = stats.get(player.getUniqueId());
                if (playerStats == null || playerStats.currentMode != LudoMode.JACKPOT) {
                    this.cancel();
                    return;
                }
                
                for (int i = 0; i < 10; i++) {
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
                        player.getLocation().add(random.nextDouble()*2-1, random.nextDouble()*2, random.nextDouble()*2-1), 
                        1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 2L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        LudoStats playerStats = stats.get(player.getUniqueId());
        
        if (playerStats == null || playerStats.currentMode == LudoMode.NONE) return;
        
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity target = (LivingEntity) event.getEntity();
        
        switch (playerStats.currentMode) {
            case FROST:
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                
                playerStats.frostHits++;
                if (playerStats.frostHits >= 8) {
                    freezeTarget(target);
                    playerStats.frostHits = 0;
                    player.sendMessage("§bЦель заморожена!");
                }
                break;
                
            case SPIDER:
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 140, 1));
                break;
                
            case MJOLNIR:
                if (random.nextDouble() < 0.3) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(5.0, player);
                }
                break;
                
            case DEATH_SCYTHE:
                if (playerStats.hitsLeft > 0) {
                    double playerHealth = player.getHealth();
                    double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                    double newHealth = Math.min(playerHealth + 10, maxHealth);
                    player.setHealth(newHealth);
                    
                    target.damage(10.0, player);
                    
                    playerStats.hitsLeft = 0;
                    player.sendMessage("§cКоса смерти вытянула жизнь! +5♥");
                    endMode(player);
                }
                break;
                
            case STORM:
                if (playerStats.hitsLeft > 0) {
                    target.setVelocity(target.getVelocity().add(new Vector(0, 1.5, 0)));
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(5.0, player);
                    
                    playerStats.hitsLeft--;
                    if (playerStats.hitsLeft <= 0) {
                        endMode(player);
                    }
                }
                break;
                
            case REAPER:
                if (playerStats.hitsLeft > 0) {
                    List<PotionEffect> stolen = new ArrayList<>();
                    
                    for (PotionEffect effect : target.getActivePotionEffects()) {
                        if (isBeneficial(effect.getType())) {
                            stolen.add(new PotionEffect(effect.getType(), 
                                effect.getDuration(), 
                                effect.getAmplifier(), 
                                effect.isAmbient(), 
                                effect.hasParticles(), 
                                effect.hasIcon()));
                            target.removePotionEffect(effect.getType());
                        }
                    }
                    
                    for (PotionEffect effect : stolen) {
                        player.addPotionEffect(effect);
                    }
                    
                    playerStats.hitsLeft = 0;
                    player.sendMessage("§5§l" + stolen.size() + " эффектов украдено!");
                    endMode(player);
                }
                break;
                
            case LIGHT_MACE:
                if (playerStats.hitsLeft > 0) {
                    target.setVelocity(target.getVelocity().add(new Vector(0, 1.5, 0)));
                    playerStats.hitsLeft--;
                    
                    if (playerStats.hitsLeft <= 0) {
                        endMode(player);
                    }
                }
                break;
        }
    }

    @EventHandler
    public void onEntityDamagePlayer(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        LudoStats playerStats = stats.get(player.getUniqueId());
        
        if (playerStats != null && playerStats.isInvulnerable) {
            event.setCancelled(true);
            
            if (playerStats.currentMode == LudoMode.EXCALIBUR) {
                playerStats.hitsLeft--;
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.0f);
                
                if (playerStats.hitsLeft <= 0) {
                    endMode(player);
                }
            }
        }
    }

    private void freezeTarget(LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 254));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 80, 128));
        target.setFreezeTicks(80);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                target.removePotionEffect(PotionEffectType.SLOWNESS);
                target.removePotionEffect(PotionEffectType.JUMP_BOOST);
                target.setFreezeTicks(0);
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), 80L);
    }

    private boolean isBeneficial(PotionEffectType type) {
        return type == PotionEffectType.SPEED ||
               type == PotionEffectType.HASTE ||
               type == PotionEffectType.STRENGTH ||
               type == PotionEffectType.JUMP_BOOST ||
               type == PotionEffectType.REGENERATION ||
               type == PotionEffectType.RESISTANCE ||
               type == PotionEffectType.FIRE_RESISTANCE ||
               type == PotionEffectType.WATER_BREATHING ||
               type == PotionEffectType.INVISIBILITY ||
               type == PotionEffectType.NIGHT_VISION ||
               type == PotionEffectType.HEALTH_BOOST ||
               type == PotionEffectType.ABSORPTION ||
               type == PotionEffectType.SATURATION ||
               type == PotionEffectType.LUCK ||
               type == PotionEffectType.SLOW_FALLING ||
               type == PotionEffectType.CONDUIT_POWER ||
               type == PotionEffectType.DOLPHINS_GRACE ||
               type == PotionEffectType.HERO_OF_THE_VILLAGE;
    }

    private boolean isLudoSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Лудо-меч");
    }
}