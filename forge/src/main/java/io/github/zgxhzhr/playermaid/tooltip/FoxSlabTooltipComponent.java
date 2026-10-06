package io.github.zgxhzhr.playermaid.tooltip;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * 装有「人是狐」玩家的魂符悬停预览：显示被收容玩家名字（灰色），
 * 并渲染一个使用魂符记录的模型 id 的缓慢旋转车万女仆模型示意图（仿车万女仆收容女仆的魂符预览样式）。
 *
 * <p>渲染逻辑移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）的
 * {@code ClientMaidTooltip}：高度 70；名字灰色画在 (pX, pY+2)；旋转姿势
 * {@code rotateZ(π) * rotateY(currentTimeMillis / 25.0 % 360)}；用
 * {@link InventoryScreen#renderEntityInInventory} 渲染缓存的 {@link EntityMaid}。</p>
 *
 * <p>每次渲染前按魂符上的模型 id 调用 {@code maid.setModelId(...)}；模型 id 为空时不渲染模型，
 * 只画名字。渲染走车万女仆的实体渲染调度器（EntityMaidRenderer），与收容维度/同屏无关，可靠。</p>
 */
public class FoxSlabTooltipComponent implements ClientTooltipComponent {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 用于预览渲染的缓存女仆实体（跨渲染复用，当前关卡变化时重建）。 */
    private static EntityMaid cachedMaid;

    private final String playerName;
    private final String slabModelId;

    public FoxSlabTooltipComponent(FoxSlabTooltip tooltip) {
        this.playerName = tooltip.playerName();
        this.slabModelId = tooltip.slabModelId();
    }

    @Override
    public int getHeight() {
        return 70;
    }

    @Override
    public int getWidth(Font font) {
        String name = playerName == null ? "" : playerName;
        return Math.max(font.width(Component.literal(name)), 50);
    }

    @Override
    public void renderImage(Font font, int pX, int pY, GuiGraphics guiGraphics) {
        LOGGER.info("[playermaid] FoxSlabTooltipComponent.renderImage: playerName={} slabModelId={}", playerName, slabModelId);
        // 名字：显示被收容玩家名字（灰色），不是女仆模型名
        if (playerName != null) {
            guiGraphics.drawString(font, Component.literal(playerName).withStyle(ChatFormatting.GRAY),
                    pX, pY + 2, 0xFFFFFF);
        }
        // 未配置魂符展示模型 id：不渲染模型，只画名字
        if (slabModelId == null || slabModelId.isBlank()) {
            LOGGER.info("[playermaid] FoxSlabTooltipComponent.renderImage: slabModelId 为空，跳过模型渲染");
            return;
        }
        LOGGER.info("[playermaid] FoxSlabTooltipComponent.renderImage: 进入模型渲染分支 slabModelId={}", slabModelId);
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        EntityMaid maid = getCachedMaid(world);
        if (maid == null) {
            return;
        }
        // 每次渲染前先清除女仆的模型/动画渲染残留（仿车万女仆 ClientMaidTooltip），
        // 否则缓存的同一女仆实体在切换模型后仍渲染旧模型
        EntityCacheUtil.clearMaidDataResidue(maid, false);
        maid.setModelId(slabModelId);

        int width = this.getWidth(font);
        int posX = pX + width / 2;
        int posY = pY + 64;
        double rot = ((System.currentTimeMillis() / 25.0) % 360);
        Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
        Quaternionf rotation = (new Quaternionf()).rotateY((float) Math.toRadians(rot));
        pose.mul(rotation);

        guiGraphics.enableScissor(pX, posY - 50, pX + width, posY);
        InventoryScreen.renderEntityInInventory(guiGraphics, posX, posY, 25, pose, null, maid);
        guiGraphics.disableScissor();
    }

    /** 取（必要时创建）用于预览的缓存女仆实体；当前关卡变化时重建。 */
    @Nullable
    private static EntityMaid getCachedMaid(Level world) {
        if (cachedMaid != null && cachedMaid.level() == world) {
            return cachedMaid;
        }
        EntityMaid maid = EntityMaid.TYPE.create(world);
        if (maid == null) {
            maid = new EntityMaid(world);
        }
        cachedMaid = maid;
        return cachedMaid;
    }
}
