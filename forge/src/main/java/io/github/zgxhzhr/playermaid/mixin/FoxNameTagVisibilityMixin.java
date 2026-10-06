package io.github.zgxhzhr.playermaid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 人是狐玩家的头顶名牌仅在准星对准时显示，与车万女仆女仆名牌的表现一致。
 *
 * <p>原版对玩家的名牌不做准星判定（只要开启名牌显示就始终可见）。
 * 这里选择拦截最底层的 {@code EntityRenderer#renderNameTag}：无论玩家实体由原版
 * {@code PlayerRenderer} 渲染，还是被第三方模型模组替换为自有渲染器
 * （其渲染器会重写 {@code shouldShowName} 并绕过父类实现），
 * 最终都必然经由此方法绘制名牌，因此拦截点对渲染方式不敏感。</p>
 *
 * <p>准星选中时放行，从而复用调试器写入的自定义渲染名；未选中时取消绘制。
 * 注意：取消会让方法体内的 Forge 名牌事件不投递，聊天气泡随之隐藏，
 * 这与车万女仆「不瞄准时不显示名牌与气泡」的表现一致。</p>
 */
@Mixin(EntityRenderer.class)
public abstract class FoxNameTagVisibilityMixin {

    @Inject(
            method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void playermaid$hideUnlessTargeted(Entity entity, Component displayName, PoseStack poseStack,
                                              MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof Player player && ClientFoxState.isActive(player)
                && Minecraft.getInstance().crosshairPickEntity != player) {
            ci.cancel();
        }
    }
}
