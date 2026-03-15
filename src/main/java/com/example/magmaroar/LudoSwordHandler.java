package com.example.magmaroar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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

import net.kyori.adventure.text.Component;

import java.util.*;

public class LudoSwordHandler implements Listener {

    private final Map<UUID, LudoStats> stats = new HashMap<>();
    private final Random random = new Random();
    
    private enum LudoMode {
        NONE, FROST, SHADOW, SPIDER, MJOLNIR, DEATH_SCYTHE,
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
    private static final Map<LudoMode, Material> MODE_MATERIALS = new HashMap<>();
    private static final Map<LudoMode, String> MODE_NAMES = new HashMap<>();
    
    static {
        DURATIONS.put(LudoMode.FROST, 20);
        DURATIONS.put(LudoMode.SHADOW, 20);
        DURATIONS.put(LudoMode.SPIDER, 15);
        DURATIONS.put(LudoMode.MJOLNIR, 15);
        DURATIONS.put(LudoMode.STORM, 15);
        DURATIONS.put(LudoMode.JACKPOT, 40);
        
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
        
        MODE_NAMES.put(LudoMode.FROST, "§bМорозный меч");
        MODE_NAMES.put(LudoMode.SHADOW, "§8Теневой меч");
        MODE_NAMES.put(LudoMode.SPIDER, "§2Паучий клинок");
        MODE_NAMES.put(LudoMode.MJOLNIR, "§eМьёльнир");
        MODE_NAMES.put(LudoMode.DEATH_SCYTHE, "§cКоса смерти");
        MODE_NAMES.put(LudoMode.STORM, "§9Клинок бури");
        MODE_NAMES.put(LudoMode.REAPER, "§5Коса жнеца");
        MODE_NAMES.put(LudoMode.DRAGON, "§dКатана дракона");
        MODE_NAMES.put(LudoMode.EXCALIBUR, "§6Экскалибур");
        MODE_NAMES.put(LudoMode.LIGHT_MACE, "§fЛегкая булава");
        MODE_NAMES.put(LudoMode.JACKPOT, "§d§lДЖЕКПОТ");
        
        MODE_MATERIALS.put(LudoMode.FROST, Material.NETHERITE_SWORD);
        MODE_MATERIALS.put(LudoMode.SHADOW, Material.NETHERITE_SWORD);
        MODE_MATERIALS.put(LudoMode.SPIDER, Material.NETHERITE_SWORD);
        MODE_MATERIALS.put(LudoMode.MJOLNIR, Material.IRON_AXE);
        MODE_MATERIALS.put(LudoMode.DEATH_SCYTHE, Material.NETHERITE_HOE);
        MODE_MATERIALS.put(LudoMode.STORM, Material.NETHERITE_SWORD);
        MODE_MATERIALS.put(LudoMode.REAPER, Material.NETHERITE_HOE);
        MODE_MATERIALS.put(LudoMode.DRAGON, Material.NETHERITE_SWORD);
        MODE_MATERIALS.put(LudoMode.EXCALIBUR, Material.NETHERITE_SWORD);
        MODE_MATERIALS.put(LudoMode.LIGHT_MACE, Material.MACE);
        MODE_MATERIALS.put(LudoMode.JACKPOT, Material.NETHERITE_SWORD);
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
            
            // Даём зелья при активации
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3600, 1, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 3600, 1, true, true, true));
            
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
        
        if (r < 5) return LudoMode.JACKPOT;
        
        int index = (int) ((r - 5) / 9.5);
        LudoMode[] modes = {
            LudoMode.FROST, LudoMode.SHADOW, LudoMode.SPIDER, LudoMode.MJOLNIR,
            LudoMode.DEATH_SCYTHE, LudoMode.STORM, LudoMode.REAPER, LudoMode.DRAGON,
            LudoMode.EXCALIBUR, LudoMode.LIGHT_MACE
        };
        return modes[Math.min(index, 9)];
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
        if (handItem != null) {
            Material newMaterial = MODE_MATERIALS.get(mode);
            if (newMaterial != null) {
                handItem.setType(newMaterial);
                
                ItemMeta meta = handItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(MODE_NAMES.get(mode)));
                    handItem.setItemMeta(meta);
                }
            }
        }
        
        long now = System.currentTimeMillis();
        
        switch (mode) {
            case FROST:
                playerStats.modeEndTime = now + 20000;
                playerStats.frostHits = 0;
                player.sendMessage("§bМорозный меч: замедление при ударе, заморозка после 8 ударов (20 сек)");
                break;
                
            case SHADOW:
                playerStats.modeEndTime = now + 20000;
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 400, 0, true, true, true));
                player.sendMessage("§8Теневой меч: невидимость (20 сек)");
                break;
                
            case SPIDER:
                playerStats.modeEndTime = now + 15000;
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAVING, 300, 0, true, true, true));
                spawnSpiderWeb(player.getLocation());
                player.sendMessage("§2Паучий клинок: следующий удар отравляет (15 сек)");
                break;
                
            case MJOLNIR:
                playerStats.modeEndTime = now + 15000;
                player.sendMessage("§eМьёльнир: броски молота (15 сек)");
                break;
                
            case DEATH_SCYTHE:
                playerStats.hitsLeft = 1;
                player.sendMessage("§cКоса смерти: следующий удар вампиризм 5♥");
                break;
                
            case STORM:
                playerStats.modeEndTime = now + 15000;
                playerStats.hitsLeft = 3;
                player.sendMessage("§9Клинок бури: следующие 3 удара подбрасывают + молния");
                break;
                
            case REAPER:
                playerStats.hitsLeft = 1;
                player.sendMessage("§5Коса жнеца: следующий удар крадёт ВСЕ эффекты");
                break;
                
            case DRAGON:
                Location targetLoc = player.getTargetBlock(null, 15).getLocation().add(0.5, 1, 0.5);
                player.teleport(targetLoc);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage("§dКатана дракона: телепортация!");
                endMode(player);
                return;
                
            case EXCALIBUR:
                playerStats.hitsLeft = 15;
                playerStats.isInvulnerable = true;
                player.sendMessage("§6Экскалибур: неуязвимость на 15 ударов!");
                break;
                
            case LIGHT_MACE:
                playerStats.hitsLeft = 3;
                player.setVelocity(player.getVelocity().add(new Vector(0, 1.5, 0)));
                player.sendMessage("§fЛегкая булава: следующие 3 удара с подбросом");
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
        int duration = DURATIONS.getOrDefault(mode, 20) * 20;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                LudoStats playerStats = stats.get(player.getUniqueId());
                if (playerStats != null && playerStats.currentMode == mode) {
                    endMode(player);
                }
            }
        }.runTaskLater(MagmaRoarPlugin.getInstance(), duration);
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
                
                for (int i = 0; i < 5; i++) {
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
                        player.getLocation().add(random.nextDouble()*2-1, random.nextDouble()*2, random.nextDouble()*2-1), 
                        1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(MagmaRoarPlugin.getInstance(), 0L, 5L);
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
                        stolen.add(new PotionEffect(effect.getType(), 
                            effect.getDuration(), 
                            effect.getAmplifier(), 
                            effect.isAmbient(), 
                            effect.hasParticles(), 
                            effect.hasIcon()));
                        target.removePotionEffect(effect.getType());
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

    private boolean isLudoSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.displayName() != null &&
               meta.displayName().toString().contains("Лудо-меч");
    }
}