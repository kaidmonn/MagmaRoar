package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShadowBlade107Handler implements Listener {

    private final Map<UUID, UUID> activeDecoys = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 107) return;

        if (!e.getAction().name().contains("RIGHT")) return;
        if (p.getCooldown(Material.NETHERITE_SWORD) > 0 || activeDecoys.containsKey(p.getUniqueId())) return;

        activateShadowStep(p);
    }

    private void activateShadowStep(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 2)); // Скорость 3 (индекс 2)
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0));

        // Скрываем игрока для всех
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != p) online.hidePlayer(MagmaRoar.getInstance(), p);
        }

        // Создаем невидимый хитбокс (стенд)
        ArmorStand decoy = p.getWorld().spawn(p.getLocation(), ArmorStand.class, s -> {
            s.setInvisible(true);
            s.setBasePlate(false);
            s.setArms(true);
            s.setMetadata("shadow_decoy", new FixedMetadataValue(MagmaRoar.getInstance(), p.getUniqueId().toString()));
        });

        activeDecoys.put(p.getUniqueId(), decoy.getUniqueId());

        // Таймер на 15 секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                revealPlayer(p);
            }
        }.runTaskLater(MagmaRoar.getInstance(), 300);
    }

    @EventHandler
    public void onDecoyHit(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        if (!victim.hasMetadata("shadow_decoy")) return;

        if (e.getDamager() instanceof Player attacker) {
            Material tool = attacker.getInventory().getItemInMainHand().getType();
            // Если удар мечом или топором
            if (tool.name().contains("SWORD") || tool.name().contains("_AXE")) {
                UUID ownerUUID = UUID.fromString(victim.getMetadata("shadow_decoy").get(0).asString());
                Player owner = Bukkit.getPlayer(ownerUUID);
                if (owner != null) revealPlayer(owner);
            }
        }
    }

    private void revealPlayer(Player p) {
        if (!activeDecoys.containsKey(p.getUniqueId())) return;

        // Удаляем стенд
        Entity decoy = Bukkit.getEntity(activeDecoys.get(p.getUniqueId()));
        if (decoy != null) decoy.remove();
        activeDecoys.remove(p.getUniqueId());

        // Показываем игрока всем обратно
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(MagmaRoar.getInstance(), p);
        }
        
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.setCooldown(Material.NETHERITE_SWORD, 1300); // 65 секунд
    }
}