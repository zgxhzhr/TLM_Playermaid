package io.github.zgxhzhr.playermaid.event;

import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import io.github.zgxhzhr.playermaid.menu.MenuAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 人是狐交互入口：非潜行右键处于人是狐状态的玩家，打开女仆主界面。
 *
 * <p>潜行右键保留给其他模组（如调试器实体编辑器），不在此拦截。</p>
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
        if (!(event.getTarget() instanceof ServerPlayer target) || visitor == target) {
            return;
        }
        FoxMaidData data = FoxMaidManager.peek(target).orElse(null);
        if (data == null || !data.isActive()) {
            return;
        }
        event.setCanceled(true);
        MenuAccess.open(visitor, target, false);
    }
}
