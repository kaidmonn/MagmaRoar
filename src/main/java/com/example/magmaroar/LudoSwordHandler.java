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
    private final Map<UUID, Long> globalCooldowns = new HashMap<>();
    private final Random random = new Random();
    
    private static final long GLOBAL_COOLDOWN = 2000; // 2 секунды на рулетку
    
    private enum LudoMode {
        NONE,
        FROST, SHADOW, SPIDER, MJOLNIR, DEATH_SCYTHE,
        STORM, REAPER, DRAGON, EXCALIBUR, LIGHT_MACE, JACKPOT
    }
    
    private static class LudoStats {
        LudoMode currentMode = LudoMode.NONE;
        long modeEndTime = 0;
        int hitsLeft = 0;
        Map<LudoMode, Long> abilityCooldowns = new HashMap<>();
        Map<UUID, Integer> frostHits = new HashMap<>();
        boolean isInvulnerable = false;
    }
    
    private static final Map<LudoMode, Integer> DURATIONS = new HashMap<>();
    private static final Map<LudoMode, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<LudoMode, Sound> MODE_SOUNDS = new HashMap<>();
    private static final Map<LudoMode, Float> SOUND_PITCHES = new HashMap<>();
    
    static {
        // Длительности
        DURATIONS.put(LudoMode.FROST, 20);
        DURATIONS.put(LudoMode.SHADOW, 20);
        DURATIONS.put(LudoMode.SPIDER, 15);
        DURATIONS.put(LudoMode.MJOLNIR, 15);
        DURATIONS.put(LudoMode.JACKPOT, 40);
        
        // Кулдауны
        COOLDOWNS.put(LudoMode.FROST, 45);
        COOLDOWNS.put(LudoMode.SHADOW, 40);
        COOLDOWNS.put(LudoMode.SPIDER, 30);
        COOLDOWNS.put(LudoMode.MJOLNIR, 40);
        COOLDOWNS.put(LudoMode.DEATH_SCYTHE, 40);
        COOLDOWNS.put(LudoMode.STORM, 45);
        COOLDOWNS.put(LudoMode.REAPER, 40);
        COOLDOWNS.put(LudoMode.DRAGON, 20);
        COOLDOWNS.put(LudoMode.EXCALIBUR, 50);
        COOLDOWNS.put(LudoMode.LIGHT_MACE, 45);
        COOLDOWNS.put(LudoMode.JACKPOT, 60);
        
        // Звуки для каждой способности
        MODE_SOUNDS.put(LudoMode.FROST, Sound.BLOCK_GLASS_BREAK);
        SOUND_PITCHES.put(LudoMode.FROST, 1.5f);
        
        MODE_SOUNDS.put(LudoMode.SHADOW, Sound.ENTITY_ENDERMAN_TELEPORT);
        SOUND_PITCHES.put(LudoMode.SHADOW, 1.2f);
        
        MODE_SOUNDS.put(LudoMode.SPIDER, Sound.ENTITY_SPIDER_DEATH);
        SOUND_PITCHES.put(LudoMode.SPIDER, 1.0f);
        
        MODE_SOUNDS.put(LudoMode.MJOLNIR, Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
        SOUND_PITCHES.put(LudoMode.MJOLNIR, 1.0f);
        
        MODE_SOUNDS.put(LudoMode.DEATH_SCYTHE, Sound.ENTITY_WITHER_SPAWN);
        SOUND_PITCHES.put(LudoMode.DEATH_SCYTHE, 0.8f);
        
        MODE_SOUNDS.put(LudoMode.STORM, Sound.ENTITY_LIGHTNING_BOLT_IMPACT);
        SOUND_PITCHES.put(LudoMode.STORM, 1.2f);
        
        MODE_SOUNDS.put(LudoMode.REAPER, Sound.ENTITY_SPLASH_POTION_BREAK);
        SOUND_PITCHES.put(LudoMode.REAPER, 0.5f);
        
        MODE_SOUNDS.put(LudoMode.DRAGON, Sound.ENTITY_ENDER_DRAGON_GROWL);
        SOUND_PITCHES.put(LudoMode.DRAGON, 1.5f);
        
        MODE_SOUNDS.put(LudoMode.EXCALIBUR, Sound.BLOCK_ANVIL_PLACE);
        SOUND_PITCHES.put(LudoMode.EXCALIBUR, 1.0f);
        
        MODE_SOUNDS.put(LudoMode.LIGHT_MACE, Sound.ENTITY_HORSE_JUMP);
        SOUND_PITCHES.put(LudoMode.LIGHT_MACE, 1.5f);
        
        MODE_SOUNDS.put(LudoMode.JACKPOT, Sound.UI_TOAST_CHALLENGE_COMPLETE);
        SOUND_PITCHES.put(LudoMode.JACKPOT, 1.0f);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isLudoSword(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            long now = System.currentTimeMillis();
            LudoStats playerStats = stats.computeIfAbsent(player.getUniqueId(), k -> new LudoStats());
            
            if (playerStats.currentMode != LudoMode.NONE) {
                player.sendMessage("§cЛудо-меч уже активен!");
                event.setCancelled(true);
                return;
            }
            
            Long lastGlobal = globalCooldowns.get(player.getUniqueId());
            if (lastGlobal != null && now - lastGlobal < GLOBAL_COOLDOWN) {
                event.setCancelled(true);
                return;
            }

            startRoulette(player);
            event.setCancelled(true);
        }
    }

    private void startRoulette(Player player) {
        player.sendMessage("§6§l🔄 ЛУДО-МЕЧ: КРУТИТСЯ РУЛЕТКА...");
        
        // Звук рулетки
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int ticks = 0;
            String lastMessage = "";
            
            @Override
            public void run() {
                if (ticks >= 40) { // 2 секунды
                    
                    LudoMode selected = selectRandomMode();
                    
                    // Финальный звук
                    playModeSound(player, selected, true);
                    
                    String modeName = getModeName(selected);
                    player.sendMessage("§6§l═══════════════════════");
                    player.sendMessage("§6§l  ВЫПАЛО: " + modeName);
                    player.sendMessage("§6§l═══════════════════════");
                    
                    if (selected == LudoMode.JACKPOT) {
                        player.sendMessage("§d§l░█████╗░░█████╗░███████╗██╗░░██╗██████╗░░█████╗░████████╗");
                        player.sendMessage("§d§l██╔══██╗██╔══██╗██╔════╝██║░██╔╝██╔══██╗██╔══██╗╚══██╔══╝");
                        player.sendMessage("§d§l██║░░╚═╝██║░░██║█████╗░░█████═╝░██████╔╝██║░░██║░░░██║░░░");
                        player.sendMessage("§d§l██║░░██╗██║░░██║██╔══╝░░██╔═██╗░██╔═══╝░██║░░██║░░░██║░░░");
                        player.sendMessage("§d§l╚█████╔╝╚█████╔╝██║░░░░░██║░╚██╗██║░░░░░╚█████╔╝░░░██║░░░");
                        player.sendMessage("§d§l░╚════╝░░╚════╝░╚═╝░░░░░╚═╝░░╚═╝╚═╝░░░░░░╚════╝░░░░╚═╝░░░");
                    }
                    
                    activateMode(player, selected);
                    this.cancel();
                    return;
                }
                
                // Каждые 2 тика меняем название
                if (ticks % 2 == 0) {
                    String randomName = getRandomModeName();
                    if (!randomName.equals(lastMessage)) {
                        player.sendMessage("§8> " + randomName);
                        lastMessage = randomName;
                        
                        // Звук рулетки
                        if (ticks % 8 == 0) {
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void playModeSound(Player player, LudoMode mode, boolean final_) {
        Sound sound = MODE_SOUNDS.get(mode);
        float pitch = SOUND_PITCHES.get(mode);
        
        if (sound != null) {
            if (mode == LudoMode.JACKPOT) {
                // Джекпот - каскад звуков
                player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2.0f, 1.2f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.5f);
            } else {
                player.getWorld().playSound(player.getLocation(), sound, 1.0f, pitch);
            }
        }
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

    private String getModeName(LudoMode mode) {
        switch (mode) {
            case FROST: return "§b❄️ Морозный меч";
            case SHADOW: return "§8🌑 Теневой меч";
            case SPIDER: return "§2🕷️ Паучий клинок";
            case MJOLNIR: return "§e⚡ Мьёльнир";
            case DEATH_SCYTHE: return "§c💀 Коса смерти";
            case STORM: return "§9🌪️ Клинок бури";
            case REAPER: return "§5💀 Коса жнеца";
            case DRAGON: return "§d🐉 Катана дракона";
            case EXCALIBUR: return "§6⚔️ Экскалибур";
            case LIGHT_MACE: return "§f🏏 Легкая булава";
            case JACKPOT: return "§d§l💰 ДЖЕКПОТ";
            default: return "Неизвестно";
        }
    }

    private String getRandomModeName() {
        LudoMode[] modes = LudoMode.values();
        return getModeName(modes[random.nextInt(modes.length)]);
    }

    private void activateMode(Player player, LudoMode mode) {
        LudoStats playerStats = stats.get(player.getUniqueId());
        playerStats.currentMode = mode;
        
        long now = System.currentTimeMillis();
        
        switch (mode) {
            case FROST:
                playerStats.modeEndTime = now + DURATIONS.get(mode) * 1000;
                playerStats.frostHits.put(player.getUniqueId(), 0);
                player.sendMessage("§bМорозный меч: замедление + заморозка (20 сек)");
                break;
                
            case SHADOW:
                playerStats.modeEndTime = now + DURATIONS.get(mode) * 1000;
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, DURATIONS.get(mode) * 20, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, DURATIONS.get(mode) * 20, 2, false, false));
                player.sendMessage("§8Теневой меч: невидимость + скорость 3 (20 сек)");
                break;
                
            case SPIDER:
                playerStats.modeEndTime = now + DURATIONS.get(mode) * 1000;
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAVING, DURATIONS.get(mode) * 20, 0, false, false));
                spawnSpiderWeb(player.getLocation());
                player.sendMessage("§2Паучий клинок: следующий удар отравляет (15 сек)");
                break;
                
            case MJOLNIR:
                playerStats.modeEndTime = now + DURATIONS.get(mode) * 1000;
                player.sendMessage("§eМьёльнир: броски молота (15 сек)");
                break;
                
            case DEATH_SCYTHE:
                playerStats.hitsLeft = 1;
                player.sendMessage("§cКоса смерти: следующий удар вампиризм 5♥");
                break;
                
            case STORM:
                playerStats.hitsLeft = 3;
                player.sendMessage("§9Клинок бури: следующие 3 удара подбрасывают + молния");
                break;
                
            case REAPER:
                playerStats.hitsLeft = 1;
                player.sendMessage("§5Коса жнеца: следующий удар крадёт все эффекты");
                break;
                
            case DRAGON:
                Location targetLoc = player.getTargetBlock(null, 15).getLocation().add(0.5, 1, 0.5);
                player.teleport(targetLoc);
                player.sendMessage("§dКатана дракона: телепортация!");
                playerStats.currentMode = LudoMode.NONE;
                startCooldown(player, mode);
                return;
                
            case EXCALIBUR:
                playerStats.hitsLeft = 15;
                playerStats.isInvulnerable = true;
                player.sendMessage("§6Экскалибур: неуязвимость на 15 ударов!");
                startExcaliburEffects(player);
                break;
                
            case LIGHT_MACE:
                playerStats.hitsLeft = 3;
                player.setVelocity(player.getVelocity().add(new Vector(0, 1.5, 0)));
                player.sendMessage("§fЛегкая булава: следующие 3 удара с подбросом");
                break;
                
            case JACKPOT:
                playerStats.modeEndTime = now + DURATIONS.get(mode) * 1000;
                playerStats.isInvulnerable = true;
                player.sendMessage("§d§l💰 ДЖЕКПОТ! ПОЛНАЯ НЕУЯЗВИМОСТЬ 40 СЕКУНД!");
                startJackpotEffects(player);
                startJackpotSoundLoop(player);
                break;
        }
        
        if (DURATIONS.getOrDefault(mode, 0) > 0 && mode != LudoMode.DRAGON) {
            startModeTimer(player, mode);
        }
    }

    private void startJackpotSoundLoop(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                LudoStats playerStats = stats.get(player.getUniqueId());
                if (playerStats == null || playerStats.currentMode != LudoMode.JACKPOT) {
                    this.cancel();
                    return;
                }
                
                // Каждые 2 секунды проигрываем звук джекпота
                if (ticks % 40 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.5f, 1.2f);
                }
                
                ticks++;
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 1L);
    }

    private void startModeTimer(Player player, LudoMode mode) {
        new BukkitRunnable() {
            @Override
            public void run() {
                LudoStats playerStats = stats.get(player.getUniqueId());
                if (playerStats != null && playerStats.currentMode == mode) {
                    endMode(player);
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), DURATIONS.get(mode) * 20L);
    }

    private void startCooldown(Player player, LudoMode mode) {
        int cooldown = COOLDOWNS.get(mode);
        LudoStats playerStats = stats.get(player.getUniqueId());
        playerStats.abilityCooldowns.put(mode, System.currentTimeMillis() + cooldown * 1000);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                playerStats.abilityCooldowns.remove(mode);
                player.sendMessage("§a" + getModeName(mode) + " снова доступен!");
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), cooldown * 20L);
    }

    private void endMode(Player player) {
        LudoStats playerStats = stats.get(player.getUniqueId());
        if (playerStats == null || playerStats.currentMode == LudoMode.NONE) return;
        
        LudoMode mode = playerStats.currentMode;
        playerStats.currentMode = LudoMode.NONE;
        playerStats.hitsLeft = 0;
        playerStats.isInvulnerable = false;
        
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.WEAVING);
        
        player.sendMessage("§cЭффект " + getModeName(mode) + " закончился.");
        
        startCooldown(player, mode);
    }

    private void spawnSpiderWeb(Location center) {
        World world = center.getWorld();
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

    private void startExcaliburEffects(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                LudoStats playerStats = stats.get(player.getUniqueId());
                if (playerStats == null || playerStats.currentMode != LudoMode.EXCALIBUR) {
                    this.cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L);
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
                        player.getLocation().add((Math.random()-0.5)*2, Math.random()*2, (Math.random()-0.5)*2), 
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
                int hits = playerStats.frostHits.getOrDefault(player.getUniqueId(), 0) + 1;
                playerStats.frostHits.put(player.getUniqueId(), hits);
                
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                
                if (hits >= 15) {
                    freezeTarget(target);
                    playerStats.frostHits.remove(player.getUniqueId());
                    player.sendMessage("§bЦель заморожена!");
                }
                break;
                
            case SPIDER:
                if (playerStats.hitsLeft > 0) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 140, 1));
                    playerStats.hitsLeft = 0;
                    player.sendMessage("§2Яд сработал!");
                }
                break;
                
            case MJOLNIR:
                if (random.nextDouble() < 0.3) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(5.0, player);
                }
                break;
                
            case DEATH_SCYTHE:
                if (playerStats.hitsLeft > 0) {
                    double newHealth = Math.min(player.getHealth() + 10, player.getAttribute(Attribute.MAX_HEALTH).getValue());
                    player.setHealth(newHealth);
                    target.damage(10.0, player);
                    playerStats.hitsLeft = 0;
                    player.sendMessage("§cКоса смерти вытянула жизнь!");
                    endMode(player);
                }
                break;
                
            case STORM:
                if (playerStats.hitsLeft > 0) {
                    target.setVelocity(target.getVelocity().add(new Vector(0, 1.5, 0)));
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(5.0, player);
                    
                    playerStats.hitsLeft--;
                    player.sendMessage("§9Удар бури! Осталось: " + playerStats.hitsLeft);
                    
                    if (playerStats.hitsLeft <= 0) {
                        endMode(player);
                    }
                }
                break;
                
            case REAPER:
                if (playerStats.hitsLeft > 0) {
                    // Кража эффектов (упрощенная версия)
                    for (PotionEffect effect : target.getActivePotionEffects()) {
                        if (isBeneficial(effect.getType())) {
                            player.addPotionEffect(new PotionEffect(effect));
                            target.removePotionEffect(effect.getType());
                        }
                    }
                    playerStats.hitsLeft = 0;
                    player.sendMessage("§5Эффекты украдены!");
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