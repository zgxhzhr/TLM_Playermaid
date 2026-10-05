package io.github.zgxhzhr.playermaid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 人是狐玩家的光环渲染：饰品栏装备第三方法术模组的光环饰品时，
 * 在玩家脑后渲染与该模组女仆光环一致的三层旋转光环
 * （静止底层 + 顺时针中层 + 逆时针外层，均为发光贴图）。
 *
 * <p>贴图路径与动画参数（缩放 2.32、基础旋转 180°、两层转速 2.4°/-1.9°）
 * 与该模组的女仆光环渲染层保持一致；贴图按资源路径运行时加载，无编译依赖。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FoxHaloRenderHandler {

    /** 与该模组光环渲染层相同的发光强度。 */
    private static final int LIGHT_LEVEL = 0xFF00F0;
    /** 光环缩放（该模组为 2.32）；模型平面为 16×16，即缩放后半宽 1.16 格。 */
    private static final float HALO_SCALE = 2.32F;
    private static final float BASE_ROTATION = 180.0F;

    private static final ResourceLocation[] HALO_BASE = {
            haloTexture("dream_halo_1.png"),
            haloTexture("dream_halo_2.png"),
            haloTexture("dream_halo_3.png")
    };
    private static final ResourceLocation[] HALO_PART_1 = {
            haloTexture("dream_halo_1_part_1.png"),
            haloTexture("dream_halo_2_part_1.png"),
            haloTexture("dream_halo_3_part_1.png")
    };
    private static final ResourceLocation[] HALO_PART_2 = {
            haloTexture("dream_halo_1_part_2.png"),
            haloTexture("dream_halo_2_part_2.png"),
            haloTexture("dream_halo_3_part_2.png")
    };

    private FoxHaloRenderHandler() {
    }

    private static ResourceLocation haloTexture(String path) {
        return new ResourceLocation("touhou_little_maid_spell", "textures/entity/maid/halo/" + path);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        for (Player player : mc.level.players()) {
            FoxMaidStateView view = ClientFoxState.get(player).orElse(null);
            if (view == null || !view.active() || !view.hasHalo()) {
                continue;
            }
            // 与该模组女仆光环一致：隐形或睡觉时不渲染
            if (player.isInvisible() || player.isSleeping()) {
                continue;
            }
            renderHalo(event.getPoseStack(), mc.renderBuffers().bufferSource(),
                    player, view, event.getCamera(), event.getPartialTick());
        }
    }

    private static void renderHalo(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                   Player player, FoxMaidStateView view, Camera camera, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX()) - camera.getPosition().x();
        double y = Mth.lerp(partialTick, player.yo, player.getY()) - camera.getPosition().y();
        double z = Mth.lerp(partialTick, player.zo, player.getZ()) - camera.getPosition().z();
        // 光环跟随头部朝向；女仆光环锚点为头骨上方 0.44 格、后方 0.6 格，玩家按眼高换算
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float age = player.tickCount + partialTick;
        int index = haloIndex(view.taskUid());

        poseStack.pushPose();
        poseStack.translate(x, y + player.getEyeHeight() + 0.44F, z);
        // 与实体渲染管线一致：模型空间 +Z 朝向实体背后，光环应在玩家前方故取负偏移
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - headYaw));
        poseStack.translate(0, 0, -0.6F);
        renderLayer(poseStack, buffer, HALO_BASE[index], 0.0F, BASE_ROTATION);
        renderLayer(poseStack, buffer, HALO_PART_1[index], 0.0006F, BASE_ROTATION + age * 2.4F);
        renderLayer(poseStack, buffer, HALO_PART_2[index], -0.0006F, BASE_ROTATION - age * 1.9F);
        poseStack.popPose();
    }

    private static void renderLayer(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                    ResourceLocation texture, float zOffset, float rotationDegrees) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(texture));
        poseStack.pushPose();
        poseStack.scale(HALO_SCALE, HALO_SCALE, HALO_SCALE);
        poseStack.translate(0, 0, zOffset);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
        // 16×16 模型平面（±0.5 格），正反两面都发射顶点，保证任意视角可见
        emitQuad(poseStack, consumer, 0.5F);
        poseStack.popPose();
        // 立即冲刷该贴图的缓冲，保证绘制发生在当前渲染阶段
        buffer.endBatch(RenderType.eyes(texture));
    }

    private static void emitQuad(PoseStack poseStack, VertexConsumer consumer, float half) {
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -half, -half, 0, 0, 1);
        vertex(consumer, pose, half, -half, 0, 1, 1);
        vertex(consumer, pose, half, half, 0, 1, 0);
        vertex(consumer, pose, -half, half, 0, 0, 0);
        vertex(consumer, pose, -half, -half, 0, 0, 1);
        vertex(consumer, pose, -half, half, 0, 0, 0);
        vertex(consumer, pose, half, half, 0, 1, 0);
        vertex(consumer, pose, half, -half, 0, 1, 1);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LIGHT_LEVEL)
                .normal(pose.normal(), 0, 0, 1)
                .endVertex();
    }

    /** 第三方法术模组的远程/近战法术任务对应不同的光环贴图样式。 */
    private static int haloIndex(String taskUid) {
        if ("maidspell:spell_combat_far".equals(taskUid)) {
            return 2;
        }
        if ("maidspell:spell_combat_melee".equals(taskUid)) {
            return 1;
        }
        return 0;
    }
}
