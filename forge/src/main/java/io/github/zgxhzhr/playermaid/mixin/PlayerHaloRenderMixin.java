package io.github.zgxhzhr.playermaid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import io.github.zgxhzhr.playermaid.client.render.PlayerDreamCrystalHaloRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 梦云水晶光环的世界渲染入口。
 *
 * <p>光环挂在 {@code LevelRenderer#renderEntity} 方法<b>尾部</b>。选择该点的原因：
 * 第三方模型模组（YSM）接管玩家渲染后，Forge 实体渲染事件与玩家渲染器事件
 * 均不触发；名牌渲染入口（{@code EntityRenderer#renderNameTag}）虽然可达，
 * 但原版与 YSM 的 {@code shouldShowName} 都排除相机实体自身——第三人称下
 * 自己的名牌方法根本不会被调用，挂在名牌上的光环随之对自己失效。
 * {@code renderEntity} 是世界渲染中每个实体的必经入口（含第三人称的相机实体，
 * 第一人称下相机实体不经过此方法，天然满足「第一人称不显示自己光环」），
 * 且不被背包界面等界面肖像渲染复用，光环与名牌显隐彻底解耦。</p>
 *
 * <p>注入尾部的目的：此处的位姿栈与入口相同（方法内部的进栈出栈已配平），
 * 但实体模型几何已经进入多重缓冲源队列，光环几何随之排在其后统一绘制，
 * 深度测试即可把光环正确遮挡在头部之后（与原版女仆光环一致）。
 * 若在方法头绘制则会排在模型之前，看起来像浮在身前。</p>
 */
@Mixin(LevelRenderer.class)
public abstract class PlayerHaloRenderMixin {

    @Inject(method = "renderEntity", at = @At("RETURN"))
    private void playermaid$renderDreamCrystalHalo(Entity entity, double camX, double camY, double camZ,
                                                   float partialTick, PoseStack poseStack,
                                                   MultiBufferSource buffer, CallbackInfo ci) {
        if (entity instanceof Player player && ClientFoxState.isActive(player)) {
            PlayerDreamCrystalHaloRenderer.render(player, poseStack, buffer, camX, camY, camZ, partialTick);
        }
    }
}
