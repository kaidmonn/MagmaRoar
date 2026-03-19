public static ItemStack createFrostSword() {
    // Старый код с ItemStack НЕ НУЖЕН
    // Вместо этого используем команду
    
    Player player = // получи игрока
    String command = "give " + player.getName() + " minecraft:netherite_sword[" +
        "custom_model_data={strings:[\"1005\"]}," +
        "item_name='{\"text\":\"Морозный меч\",\"color\":\"aqua\",\"bold\":true}'" +
        "] 1";
    
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    return null; // или просто return, если метод void
}