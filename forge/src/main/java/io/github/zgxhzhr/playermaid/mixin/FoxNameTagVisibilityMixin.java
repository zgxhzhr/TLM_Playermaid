package io.github.zgxhzhr.playermaid.mixin;

import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 人是狐玩家的头顶名牌仅在准星对准时显示，与车万女仆女仆名牌的表现一致。
 *
 * <p>原版对玩家的名牌不做准星判定（只要开启名牌显示就始终可见），
 * 因此这里在 {@code LivingEntityRenderer#shouldShowName} 返回后追加一次判定：
 * 目标是人是狐玩家、且不是当前准星选中的实体时，改为不显示。</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class FoxNameTagVisibilityMixin {

    @Inject(
            method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void playermaid$hideUnlessTargeted(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (entity instanceof Player player
                && ClientFoxState.isActive(player)
                && Minecraft.getInstance().crosshairPickEntity != player) {
            cir.setReturnValue(false);
        }
    }
}
