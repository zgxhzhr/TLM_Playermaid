package io.github.zgxhzhr.playermaid.event;

import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import io.github.zgxhzhr.playermaid.menu.MenuAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 人是狐交互入口：非潜行右键处于人是狐状态的玩家，打开女仆主界面。
 *
 * <p>潜行右键保留给其他模组（如调试器实体编辑器），不在此拦截；
 * 手持空魂符时也直接跳过，改由魂符收容逻辑处理。</p>
 */
public final class FoxMaidInteractHandler {

    private FoxMaidInteractHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // 潜行（含调试斧编辑等场景）不拦截
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer visitor)) {
            return;
        }
        // 手持空魂符时不打开背包，改由收容逻辑处理
        if (visitor.getMainHandItem().getItem() == InitItems.SMART_SLAB_EMPTY.get()) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target) || visitor == target) {
            return;
        }
        FoxMaidData data = FoxMaidManager.peek(target).orElse(null);
        if (data == null || !data.isActive()) {
            return;
        }
        // 已认主：只有主人才能右键打开；非主人不拦截事件（原版交互照常），避免"右键没反应"的困惑
        if (!FoxMaidManager.canBeOpenedBy(target, visitor)) {
            return;
        }
        event.setCanceled(true);
        MenuAccess.open(visitor, target, false);
    }
}
