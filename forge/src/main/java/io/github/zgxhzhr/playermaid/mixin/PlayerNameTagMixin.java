package io.github.zgxhzhr.playermaid.mixin;

import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 头顶悬浮名牌：处于人是狐状态且设置了自定义渲染名的玩家，
 * 其头顶名牌替换为渲染名（Tab/聊天/死亡消息等其他位置不受影响）。
 */
@Mixin(EntityRenderer.class)
public abstract class PlayerNameTagMixin {

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getDisplayName()Lnet/minecraft/network/chat/Component;")
    )
    private Component playermaid$replaceFloatingName(Entity entity) {
        if (entity instanceof Player player) {
            String renderName = ClientFoxState.getRenderName(player);
            if (renderName != null) {
                return Component.literal(renderName);
            }
        }
        return entity.getDisplayName();
    }
}
