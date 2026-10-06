package io.github.zgxhzhr.playermaid.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import io.github.zgxhzhr.playermaid.client.model.PlayerHaloModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 人是狐玩家梦云水晶光环渲染（头后光环）。
 *
 * <p>几何、贴图与动画参数移植自万法皆通 {@code GeckoLayerDreamCatCrystalHalo}
 * （Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）。该层挂在女仆
 * 渲染器的头骨定位器上，在原版中的变换链为：头骨矩阵 → 头骨局部平移
 * {@code (0, 0.44, 0.6)} → 三张同心平面（基座 + 顺/逆时针旋转的两层），
 * 缩放 2.32、基座旋转 180°、外层 2.4°/tick、内层 -1.9°/tick，全亮光照
 * （eyes 材质，单面）。贴图直接引用万法皆通资源
 * {@code touhou_little_maid_spell:textures/entity/maid/halo/dream_halo_*.png}。</p>
 *
 * <p><b>矩阵链</b>：入口由 {@code PlayerHaloRenderMixin} 注入
 * {@code LevelRenderer#renderEntity} 方法尾部，此时位姿栈为相机视图旋转已烘焙、
 * 不含平移的空间（局部轴与世界轴平行）。先按与原版一致的插值公式平移到实体
 * 脚底，再复刻头骨坐标系：平移到头骨基点 → 绕 Y 旋转 {@code 180° - 头部偏航}
 * → 绕 X 旋转 {@code -头部俯仰} → 头骨局部平移 {@code (0, 0.44, 0.6)}。
 * 该旋转恰好把模型空间 {@code -Z}（模型正面）映射到头部视线方向，
 * 故光环平面的法线沿头部视线反方向：<b>光环固定贴在头后、不随相机转动，
 * 侧视时呈一条线</b>，与原版女仆一致（不可改为面向相机的 billboard）。</p>
 *
 * <p><b>缓冲与遮挡</b>：直接使用 {@code renderEntity} 传入的多重缓冲源，
 * 不自建即时缓冲、不主动冲刷，使光环几何排在实体模型之后统一绘制，
 * 从而被深度测试正确地遮挡在头部之后（原版同样如此）。本地玩家朝向取
 * yRot/xRot 插值（YSM 下 yHeadRot 不随视角），远程玩家取 yHeadRot/xRot 插值
 * （服务端同步）。第一人称不渲染自身光环。</p>
 *
 * <p>原版按女仆当前任务选择三套贴图（普通/近战/远战），人是狐玩家无万法皆通
 * 战斗任务，固定使用第一套（普通）贴图，此处注释说明。</p>
 */
public final class PlayerDreamCrystalHaloRenderer {

    /** 全亮光照值（方块/天空光均 15），与万法皆通原版一致。 */
    private static final int LIGHT_LEVEL = 0xFF00F0;
    private static final float HALO_SCALE = 2.32F;
    private static final float BASE_ROTATION_DEGREES = 180.0F;

    /** 头骨基点相对实体脚底的高度（原版头骨定位器所在高度）。 */
    private static final double HEAD_BASE_HEIGHT = 1.52D;
    /** 头骨局部平移的竖直分量（原版 translate 的 0.44）。 */
    private static final double HEAD_UP_OFFSET = 0.44D;
    /** 头骨局部平移的后方分量（原版 translate 的 0.6；模型空间 +Z 为实体背面）。 */
    private static final double BEHIND_HEAD_OFFSET = 0.6D;

    private static final ResourceLocation[] HALO_BASE = {
            texture("dream_halo_1.png"),
            texture("dream_halo_2.png"),
            texture("dream_halo_3.png")
    };

    private static final ResourceLocation[] HALO_PART_1 = {
            texture("dream_halo_1_part_1.png"),
            texture("dream_halo_2_part_1.png"),
            texture("dream_halo_3_part_1.png")
    };

    private static final ResourceLocation[] HALO_PART_2 = {
            texture("dream_halo_1_part_2.png"),
            texture("dream_halo_2_part_2.png"),
            texture("dream_halo_3_part_2.png")
    };

    private static PlayerHaloModel model;

    private PlayerDreamCrystalHaloRenderer() {
    }

    /**
     * 世界渲染入口（{@code LevelRenderer#renderEntity} 尾部，mixin 调用）：
     * 为人是狐且佩戴梦云水晶的玩家绘制头后光环。调用方已判定人是狐状态；
     * 此处再做可见性条件过滤并补齐实体局部平移。该方法必须仅客户端调用。
     */
    public static void render(Player player, PoseStack poseStack, MultiBufferSource buffer,
                              double camX, double camY, double camZ, float partialTick) {
        if (player.isInvisible() || player.isSleeping() || !player.isAlive()) {
            return;
        }
        if (!ClientFoxState.hasDreamCrystal(player)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        // 第一人称不渲染自己的光环（正常情况下相机实体不经过 renderEntity，此处为兜底）
        if (player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        // 与原版 renderEntity 一致的插值平移：实体局部空间原点在脚底
        double x = Mth.lerp(partialTick, player.xOld, player.getX()) - camX;
        double y = Mth.lerp(partialTick, player.yOld, player.getY()) - camY;
        double z = Mth.lerp(partialTick, player.zOld, player.getZ()) - camZ;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        renderHalo(player, poseStack, buffer, partialTick);
        poseStack.popPose();
    }

    private static void renderHalo(Player player, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (model == null) {
            model = new PlayerHaloModel(
                    mc.getEntityModels().bakeLayer(PlayerHaloModel.LAYER_LOCATION));
        }

        float ageInTicks = player.tickCount + partialTick;
        float clockwiseRotation = ageInTicks * 2.4F;
        float counterClockwiseRotation = -ageInTicks * 1.9F;

        // 头部朝向：本地玩家取视角值（YSM 下 yHeadRot 不随视角），远程玩家取服务端同步的头部朝向
        float headYaw = player == mc.player
                ? Mth.rotLerp(partialTick, player.yRotO, player.getYRot())
                : Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());

        poseStack.pushPose();
        // 复刻原版头骨坐标系：基点到头骨基点，再按头部朝向旋转。
        // 该旋转把模型空间 -Z（模型正面）映射到头部视线方向，因此模型空间 +Z
        // 即头部后方，光环平面法线随之沿视线反方向（固定贴头后，不面向相机）
        poseStack.translate(0.0D, HEAD_BASE_HEIGHT, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - headYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-headPitch));
        // 头骨局部偏移：上 0.44、后 0.6（与原版 translate(0, 0.44, 0.6) 一致）
        poseStack.translate(0.0D, HEAD_UP_OFFSET, BEHIND_HEAD_OFFSET);

        // 原版按女仆任务选择贴图下标，人是狐玩家固定第 1 套（普通）
        renderLayer(poseStack, buffer, HALO_BASE[0], HALO_SCALE, 0.0F, BASE_ROTATION_DEGREES);
        renderLayer(poseStack, buffer, HALO_PART_1[0], HALO_SCALE, 0.0006F,
                BASE_ROTATION_DEGREES + clockwiseRotation);
        renderLayer(poseStack, buffer, HALO_PART_2[0], HALO_SCALE, -0.0006F,
                BASE_ROTATION_DEGREES + counterClockwiseRotation);

        poseStack.popPose();
    }

    private static void renderLayer(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
                                    float scale, float zOffset, float rotationDegrees) {
        VertexConsumer glowConsumer = buffer.getBuffer(RenderType.eyes(texture));
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, 0.0F, zOffset);
        if (rotationDegrees != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
        }
        model.renderToBuffer(poseStack, glowConsumer, LIGHT_LEVEL, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        poseStack.popPose();
    }

    private static ResourceLocation texture(String path) {
        return new ResourceLocation("touhou_little_maid_spell", "textures/entity/maid/halo/" + path);
    }
}
