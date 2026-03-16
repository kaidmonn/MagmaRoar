package com.example.magmaroar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NPCManager implements Listener {

    private final MagmaRoarPlugin plugin;
    private final Map<UUID, Zombie> npcs = new HashMap<>();
    private final Map<UUID, Location> npcLocations = new HashMap<>();
    private final List<UUID> npcUUIDs = new ArrayList<>();

    public NPCManager(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startChunkLoader();
    }

    public void spawnNPC(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Спавним зомби (он похож на игрока)
        Zombie npc = (Zombie) world.spawnEntity(location, EntityType.ZOMBIE);
        
        // Настройки зомби
        npc.setCustomName("§6§lМитапы");
        npc.setCustomNameVisible(true);
        npc.setAI(false); // Не двигается
        npc.setInvulnerable(true); // Неуязвим
        npc.setSilent(true);
        npc.setCanPickupItems(false);
        npc.setRemoveWhenFarAway(false);
        
        // Одеваем как игрока
        ItemStack playerHead = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer("kaidmonngrief")); // Скин kaidmonngrief
        playerHead.setItemMeta(skullMeta);
        
        npc.getEquipment().setHelmet(playerHead);
        npc.getEquipment().setChestplate(new ItemStack(org.bukkit.Material.NETHERITE_CHESTPLATE));
        npc.getEquipment().setLeggings(new ItemStack(org.bukkit.Material.NETHERITE_LEGGINGS));
        npc.getEquipment().setBoots(new ItemStack(org.bukkit.Material.NETHERITE_BOOTS));
        
        // Сохраняем
        npcs.put(npc.getUniqueId(), npc);
        npcLocations.put(npc.getUniqueId(), location);
        npcUUIDs.add(npc.getUniqueId());
    }

    private void startChunkLoader() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Location loc : npcLocations.values()) {
                    if (loc.getWorld() != null) {
                        loc.getWorld().getChunkAt(loc).addPluginChunkTicket(plugin);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();
            if (npcUUIDs.contains(zombie.getUniqueId())) {
                event.setCancelled(true); // Неуязвимость
            }
        }
    }

    @EventHandler
    public void onPlayerInteractNPC(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getRightClicked();
            if (npcUUIDs.contains(zombie.getUniqueId())) {
                // Вызываем событие для QueueManager
                plugin.getQueueManager().addToQueue(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    public boolean isNPC(Zombie zombie) {
        return npcUUIDs.contains(zombie.getUniqueId());
    }

    public void removeAllNPCs() {
        for (Zombie npc : npcs.values()) {
            Location loc = npcLocations.get(npc.getUniqueId());
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().getChunkAt(loc).removePluginChunkTicket(plugin);
            }
            npc.remove();
        }
        npcs.clear();
        npcLocations.clear();
        npcUUIDs.clear();
    }

    public Location getNPCLocation() {
        if (npcLocations.isEmpty()) return null;
        return npcLocations.values().iterator().next();
    }
}