package com.example.magmaroar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class VillagerStaffItem {

    // Ключ для связи плагина с предметом (невидимый для игрока)
    public static final NamespacedKey STAFF_KEY = new NamespacedKey("magmaroar", "villager_staff");

    public static ItemStack createStaff() {
        ItemStack staff = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = staff.getItemMeta();

        if (meta != null) {
            // Название предмета (используем Component для 1.21.4)
            meta.displayName(Component.text("Посох жителя")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            // Установка CustomModelData (строковое значение "1016" для твоего ресурспака)
            // В 1.21.4 через API это всё еще число или список строк. 
            // Если твой ресурспак настроен на строку "1016", используем:
            meta.setCustomModelData(1016); 

            // ГЛАВНОЕ: Скрытая метка, чтобы способности не ломались
            meta.getPersistentDataContainer().set(STAFF_KEY, PersistentDataType.BYTE, (byte) 1);

            // Описание (Lore)
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("ПКМ: Создает мощный взрыв через 1.5 секунды").color(NamedTextColor.GRAY));
            lore.add(Component.text("Уровень взрыва: 20").color(NamedTextColor.RED));
            lore.add(Component.text("Кулдаун: 2 минуты").color(NamedTextColor.DARK_AQUA));
            meta.lore(lore);

            staff.setItemMeta(meta);
        }
        return staff;
    }

    public static void giveStaff(Player player) {
        player.getInventory().addItem(createStaff());
        player.sendMessage(Component.text("§aВы получили Посох жителя!"));
    }
}