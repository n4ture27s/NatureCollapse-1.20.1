package n4ture27.nature_collapse.utils;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Set;
import java.util.function.Consumer;

public class InventoryUtil {
    public static boolean hasFlower(Player player) {
        if (player == null || player.getInventory() == null) return false;
        
        if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) return true;

        if (ModList.get().isLoaded("curios")) {
            return CuriosApi.getCuriosInventory(player).map(handler ->
                handler.findFirstCurio(NatureCollapse.NatureFlower.get()).isPresent()
            ).orElse(false);
        }
        
        return false;
    }

    public static void forEachFlower(Player player, Consumer<ItemStack> consumer) {
        if (player == null || player.getInventory() == null) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == NatureCollapse.NatureFlower.get()) consumer.accept(stack);
        }

        if (ModList.get().isLoaded("curios")) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.findCurios(NatureCollapse.NatureFlower.get()).forEach(result -> 
                    consumer.accept(result.stack())
                );
            });
        }
    }
}