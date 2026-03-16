package com.example.magmaroar;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NPCManager implements Listener {

    private final MagmaRoarPlugin plugin;
    private final Map<UUID, ServerPlayer> npcs = new HashMap<>();
    private final Map<UUID, Location> npcLocations = new HashMap<>();
    private GameProfile npcProfile;
    private final List<UUID> npcUUIDs = new ArrayList<>();

    public NPCManager(MagmaRoarPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        createNPCProfile();
        startChunkLoader();
    }

    private void createNPCProfile() {
        UUID npcUUID = UUID.nameUUIDFromBytes("Митапы".getBytes());
        npcProfile = new GameProfile(npcUUID, "Митапы");
        
        String textureValue = "ewogICJ0aW1lc3RhbXAiIDogMTY5OTk5OTk5OSwKICAicHJvZmlsZUlkIiA6ICJhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJrYWlkbW9ubmdyaWVmIiwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhIgogICAgfQogIH0KfQ==";
        String signatureValue = "aaa";
        
        npcProfile.getProperties().put("textures", new Property("textures", textureValue, signatureValue));
    }

    public void spawnNPC(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel nmsWorld = ((CraftWorld) world).getHandle();

        ServerPlayer npc = new ServerPlayer(nmsServer, nmsWorld, npcProfile);
        
        npc.setPos(location.getX(), location.getY(), location.getZ());
        npc.setYRot(location.getYaw());
        npc.setXRot(location.getPitch());
        
        nmsWorld.addFreshEntity(npc);
        
        npcs.put(npc.getUUID(), npc);
        npcLocations.put(npc.getUUID(), location);
        npcUUIDs.add(npc.getUUID());

        for (Player player : Bukkit.getOnlinePlayers()) {
            showNPCToPlayer(player, npc);
        }
    }

    private void showNPCToPlayer(Player player, ServerPlayer npc) {
        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        ServerGamePacketListenerImpl connection = nmsPlayer.connection;

        connection.send(new ClientboundPlayerInfoUpdatePacket(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, npc
        ));
        
        connection.send(new ClientboundAddEntityPacket(
            npc.getId(),
            npc.getUUID(),
            npc.getX(),
            npc.getY(),
            npc.getZ(),
            npc.getXRot(),
            npc.getYRot(),
            net.minecraft.world.entity.EntityType.PLAYER,
            0,
            npc.getDeltaMovement(),
            0
        ));

        connection.send(new ClientboundRotateHeadPacket(
            npc,
            (byte) ((npc.getYRot() * 256f) / 360f)
        ));
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (ServerPlayer npc : npcs.values()) {
            showNPCToPlayer(player, npc);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (npcUUIDs.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    public boolean isNPC(Player player) {
        return npcUUIDs.contains(player.getUniqueId());
    }

    public void removeAllNPCs() {
        for (ServerPlayer npc : npcs.values()) {
            Location loc = npcLocations.get(npc.getUUID());
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().getChunkAt(loc).removePluginChunkTicket(plugin);
            }
            
            ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(npc.getId());
            for (Player player : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) player).getHandle().connection.send(packet);
            }
            
            npc.discard();
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