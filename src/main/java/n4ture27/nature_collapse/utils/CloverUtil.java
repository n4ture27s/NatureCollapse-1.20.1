package n4ture27.nature_collapse.utils;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CloverUtil {
    private CloverUtil() {}

    /**
     * プレイヤーのインベントリ内に四つ葉のクローバーが存在するかチェック
     */
    public static boolean hasFourLeafClover(Player player) {
        if (player == null) return false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == NatureCollapse.FourLeafClover.get()) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == NatureCollapse.FourLeafClover.get()) {
                return true;
            }
        }
        return false;
    }

    /**
     * プレイヤーのインベントリ内に通常のクローバーが存在するかチェック
     */
    public static boolean hasClover(Player player) {
        if (player == null) return false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == NatureCollapse.Clover.get()) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == NatureCollapse.Clover.get()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Luck加算値: 四つ葉なら +3、通常クローバーなら +1
     */
    public static float getLuckBonus(Player player) {
        if (hasFourLeafClover(player)) return 3.0f;
        if (hasClover(player)) return 1.0f;
        return 0.0f;
    }

    /**
     * Loot Quality加算値: 四つ葉なら +3、通常クローバーなら +1
     */
    public static float getQualityBonus(Player player) {
        if (hasFourLeafClover(player)) return 3.0f;
        if (hasClover(player)) return 1.0f;
        return 0.0f;
    }

    /**
     * レア抽選確率倍率: 四つ葉なら ×3.0、通常クローバーなら ×1.5
     */
    public static float getRarityMultiplier(Player player) {
        if (hasFourLeafClover(player)) return 3.0f;
        if (hasClover(player)) return 1.5f;
        return 1.0f;
    }

    /**
     * Loot数量倍率: 四つ葉なら ×2、それ以外は ×1
     */
    public static int getCountMultiplier(Player player) {
        if (hasFourLeafClover(player)) return 2;
        return 1;
    }
}
