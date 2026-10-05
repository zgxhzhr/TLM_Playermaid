package io.github.zgxhzhr.playermaid.mixin;

import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Jade 面板标题：处于人是狐状态且设置了自定义渲染名的玩家，
 * Jade 准心面板标题显示渲染名。Jade 缺席时该 Mixin 自动跳过。
 */
@Mixin(snownee.jade.addon.core.ObjectNameProvider.class)
public abstract class JadeNameProviderMixin {

    @Inject(method = "getEntityName", at = @At("HEAD"), cancellable = true, remap = false)
    private static void playermaid$replaceJadeTitle(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof Player player) {
            String renderName = ClientFoxState.getRenderName(player);
            if (renderName != null) {
                cir.setReturnValue(Component.literal(renderName));
            }
        }
    }
}
