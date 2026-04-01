package me.kaidmonn.magmaroar.handlers;

import me.kaidmonn.magmaroar.MagmaRoar;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class VillagerStaff108Handler implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() != Material.BLAZE_ROD || !item.hasItemMeta() || 
            item.getItemMeta().getCustomModelData() != 108) return;

        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (p.getCooldown(Material.BLAZE_ROD) > 0) return;

        Block target = p.getTargetBlockExact(50); // Увеличил дистанцию до 50 блоков
        if (target == null) return;

        Location strikeLoc = target.getLocation().add(0.5, 0.1, 0.5); // Центр блока, чуть выше
        p.setCooldown(Material.BLAZE_ROD, 1900); // 95 сек (95*20)

        // Визуал синих линий (как при F3+B) в течение 2 секунд
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) { // 2 секунды
                    spawnBeamAndExplode(strikeLoc);
                    this.cancel();
                    return;
                }
                drawBlueHitboxCross(strikeLoc);
                ticks += 2;
            }
        }.runTaskTimer(MagmaRoar.getInstance(), 0, 2); // Отрисовка каждые 2 тика для четкости
    }

    private void drawBlueHitboxCross(Location loc) {
        // Цвет пыли (Ярко-синий, как линия взгляда в хитбоксах)
        Particle.DustOptions dust = new Particle.DustOptions(Color.BLUE, 1.2f);
        double size = 1.8; // Длина палочек креста

        for (double i = -size; i <= size; i += 0.25) {
            // Линия по оси X
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(i, 0, 0), 1, 0, 0, 0, 0, dust);
            // Линия по оси Z
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0, i), 1, 0, 0, 0, 0, dust);
        }
        // Вертикальная палочка (как ось Y)
        for (double y = 0; y <= 3; y += 0.25) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 1, 0, 0, 0, 0, dust);
        }
    }

    private void spawnBeamAndExplode(Location loc) {
        int startY = loc.getBlockY();
        int maxY = loc.getWorld().getMaxHeight();

        // Спавн луча из желтого стекла
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = startY; y < maxY; y += 3) { // Через 3 для оптимизации
                    Block b = loc.clone().add(x, y - startY, z).getBlock();
                    if (b.getType() == Material.AIR) {
                        b.setType(Material.YELLOW_STAINED_GLASS);
                        // Убираем стекло через 1.5 секунды
                        Bukkit.getScheduler().runTaskLater(MagmaRoar.getInstance(), () -> b.setType(Material.AIR), 30);
                    }
                }
            }
        }

        // Взрыв уровня 12 (наносит урон всем)
        loc.getWorld().createExplosion(loc, 12.0f, true, true);
    }
}