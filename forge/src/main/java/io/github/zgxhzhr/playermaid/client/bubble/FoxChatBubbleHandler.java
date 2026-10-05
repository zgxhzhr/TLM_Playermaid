package io.github.zgxhzhr.playermaid.client.bubble;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import io.github.zgxhzhr.playermaid.client.FoxMaidStateView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 人是狐玩家的随机聊天气泡。
 *
 * <p>节奏由玩家 UUID 与 {@code tickCount} 推导，属于纯客户端确定性计算：不额外发包，
 * 但所有客户端会在同一时刻看到同一句话。文案取自 {@link FoxKaomojiPool}。</p>
 *
 * <p>渲染挂在玩家实体渲染之后（{@link RenderPlayerEvent.Post}），此时位姿栈正处于
 * 玩家实体的局部空间，因此可以直接「抬高到头顶、再转向镜头」，
 * 与车万女仆渲染女仆气泡的做法完全一致，不存在世界空间手算偏移带来的错位。
 * 气泡贴图与九宫格布局移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，
 * MIT 协议开源），特此声明。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FoxChatBubbleHandler {

    /** 每个冒泡周期（tick），10 秒。 */
    private static final int CYCLE_TICKS = 200;
    /** 每个周期内的显示时长（tick），5 秒。 */
    private static final int SHOW_TICKS = 100;
    /** 气泡贴图（车万女仆 type2），尺寸 256×256。 */
    private static final ResourceLocation BUBBLE_TEXTURE =
            new ResourceLocation("touhou_little_maid", "textures/entity/chat_bubble/type2.png");
    /** 气泡渲染类型：与车万女仆一致走文本类渲染（透明混合、不剔除背面）。 */
    private static final RenderType BUBBLE_RENDER_TYPE = RenderType.text(BUBBLE_TEXTURE);
    /** 九宫格切片边长。 */
    private static final int SLICE = 8;
    /** 气泡本体在贴图中的区域。 */
    private static final int REGION_WIDTH = 48;
    private static final int REGION_HEIGHT = 24;
    private static final int TEXTURE_SIZE = 256;
    /** 气泡内边距（与车万女仆一致）。 */
    private static final int PADDING = 5;
    /** 车万女仆渲染气泡时 1 世界单位等于 40 像素（贴图缩放 0.025）。 */
    private static final float BUBBLE_SCALE = 0.025F;
    /** 全亮光照值，使气泡不受环境亮度影响。 */
    private static final int FULL_LIGHT = 0xF000F0;

    private FoxChatBubbleHandler() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        FoxMaidStateView view = ClientFoxState.get(player.getId()).orElse(null);
        if (view == null || !view.active() || !player.isAlive() || player.isInvisible()) {
            return;
        }
        if (!isBubbleVisibleNow(player)) {
            return;
        }
        String text = pickText(player, view);
        if (text == null || text.isEmpty()) {
            return;
        }
        renderBubble(event, player, text);
    }

    /** 当前是否处于该玩家的冒泡窗口内。 */
    private static boolean isBubbleVisibleNow(Player player) {
        long offset = Math.floorMod(player.getUUID().getLeastSignificantBits(), CYCLE_TICKS);
        long phase = Math.floorMod((long) player.tickCount + offset, CYCLE_TICKS);
        return phase < SHOW_TICKS;
    }

    /** 按日程状态与工作模式从对应文案池里挑一句。 */
    private static String pickText(Player player, FoxMaidStateView view) {
        String category;
        if (player.isSleeping()) {
            category = "sleep";
        } else if (view.taskUid() != null && view.taskUid().endsWith(":idle")) {
            category = "idle";
        } else {
            category = "work";
        }
        List<String> pool = FoxKaomojiPool.pool(category);
        if (pool.isEmpty()) {
            pool = FoxKaomojiPool.pool("idle");
        }
        if (pool.isEmpty()) {
            return null;
        }
        long offset = Math.floorMod(player.getUUID().getLeastSignificantBits(), CYCLE_TICKS);
        long cycleIndex = ((long) player.tickCount + offset) / CYCLE_TICKS;
        long seed = player.getUUID().getMostSignificantBits()
                ^ (player.getUUID().getLeastSignificantBits() * 31L)
                ^ (cycleIndex * 0x9E3779B97F4A7C15L);
        return pool.get((int) Math.floorMod(seed, pool.size()));
    }

    private static void renderBubble(RenderPlayerEvent.Post event, Player player, String text) {
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();

        poseStack.pushPose();
        // 与车万女仆女仆气泡完全一致的锚点与朝向：抬到名牌高度，再转向镜头
        poseStack.translate(0.0D, player.getNameTagOffsetY(), 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        // 车万女仆的缩放；X、Y 同取负使局部坐标与界面坐标一致（向下为正）
        poseStack.scale(-BUBBLE_SCALE, -BUBBLE_SCALE, BUBBLE_SCALE);

        Font font = Minecraft.getInstance().font;
        int bgWidth = font.width(text) + 2 * PADDING;
        int bgHeight = font.lineHeight + 2 * PADDING;
        int left = -bgWidth / 2;
        int top = -bgHeight;

        Matrix4f matrix = poseStack.last().pose();
        drawBubble(buffer.getBuffer(BUBBLE_RENDER_TYPE), matrix, left, top, bgWidth, bgHeight);
        font.drawInBatch(text, left + PADDING, top + PADDING, 0xFF000000, false,
                matrix, buffer, Font.DisplayMode.NORMAL, 0, FULL_LIGHT);
        poseStack.popPose();
    }

    /** 九宫格拉伸气泡本体，再叠上指向头顶的小尾巴。 */
    private static void drawBubble(VertexConsumer consumer, Matrix4f matrix, int x, int y, int width, int height) {
        int right = x + width - SLICE;
        int bottom = y + height - SLICE;
        int centerX = x + SLICE;
        int centerY = y + SLICE;
        int centerWidth = width - 2 * SLICE;
        int centerHeight = height - 2 * SLICE;
        int sourceWidth = REGION_WIDTH - 2 * SLICE;
        int sourceHeight = REGION_HEIGHT - 2 * SLICE;

        // 四角不拉伸
        quad(consumer, matrix, x, y, SLICE, SLICE, 0, 0, SLICE, SLICE);
        quad(consumer, matrix, right, y, SLICE, SLICE, REGION_WIDTH - SLICE, 0, SLICE, SLICE);
        quad(consumer, matrix, x, bottom, SLICE, SLICE, 0, REGION_HEIGHT - SLICE, SLICE, SLICE);
        quad(consumer, matrix, right, bottom, SLICE, SLICE,
                REGION_WIDTH - SLICE, REGION_HEIGHT - SLICE, SLICE, SLICE);
        // 四边与中心拉伸
        quad(consumer, matrix, centerX, y, centerWidth, SLICE, SLICE, 0, sourceWidth, SLICE);
        quad(consumer, matrix, centerX, bottom, centerWidth, SLICE,
                SLICE, REGION_HEIGHT - SLICE, sourceWidth, SLICE);
        quad(consumer, matrix, x, centerY, SLICE, centerHeight, 0, SLICE, SLICE, sourceHeight);
        quad(consumer, matrix, right, centerY, SLICE, centerHeight,
                REGION_WIDTH - SLICE, SLICE, SLICE, sourceHeight);
        quad(consumer, matrix, centerX, centerY, centerWidth, centerHeight,
                SLICE, SLICE, sourceWidth, sourceHeight);
        // 小尾巴：位于气泡底边中点（局部原点即头顶锚点）
        quad(consumer, matrix, -SLICE, -SLICE, 16, 16, 16, 24, 16, 16);
    }

    /**
     * 以局部坐标画一个贴图矩形。
     *
     * @param x      左上角局部 X
     * @param y      左上角局部 Y（局部坐标向下为正，与界面一致）
     * @param width  宽（贴图像素）
     * @param height 高（贴图像素）
     * @param u      贴图内起始 X（贴图像素）
     * @param v      贴图内起始 Y（贴图像素）
     * @param uWidth 取样宽（贴图像素）
     * @param vHeight 取样高（贴图像素）
     */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, int x, int y, int width, int height,
                             int u, int v, int uWidth, int vHeight) {
        float x0 = x;
        float y0 = y;
        float x1 = x + width;
        float y1 = y + height;
        float u0 = (float) u / TEXTURE_SIZE;
        float v0 = (float) v / TEXTURE_SIZE;
        float u1 = (float) (u + uWidth) / TEXTURE_SIZE;
        float v1 = (float) (v + vHeight) / TEXTURE_SIZE;
        vertex(consumer, matrix, x0, y0, u0, v0);
        vertex(consumer, matrix, x0, y1, u0, v1);
        vertex(consumer, matrix, x1, y1, u1, v1);
        vertex(consumer, matrix, x1, y0, u1, v0);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float u, float v) {
        consumer.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .uv2(FULL_LIGHT)
                .endVertex();
    }
}
