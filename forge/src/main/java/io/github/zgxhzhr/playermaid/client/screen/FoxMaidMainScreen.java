package io.github.zgxhzhr.playermaid.client.screen;

import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.BaubleButton;
import com.github.tartaricacid.touhoulittlemaid.compat.curios.CuriosCompat;
import com.github.tartaricacid.touhoulittlemaid.compat.curios.client.CuriosButton;
import io.github.zgxhzhr.playermaid.menu.FoxMaidMainMenu;
import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidMenuPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 人是狐主界面：车万女仆 {@code maid_gui_main.png} 基底叠加
 * {@code maid_gui_backpack.png}（与 TLM 大背包界面一致）；顶部 36 格直接映射
 * 目标玩家的真实背包，与底部访问者背包为同一份数据。
 * 其中与主手槽指向同一下标的那一格，以及自己查看自己时的底部访问者背包，
 * 均为只读锁定样式（内容照常显示，但不能取出或放入）。
 */
public class FoxMaidMainScreen extends FoxMaidAbstractScreen<FoxMaidMainMenu> {

    public FoxMaidMainScreen(FoxMaidMainMenu menu, Inventory inventory, Component title) {
        super(menu, inventory);
    }

    @Override
    protected void initAdditionWidgets() {
        // “打开女仆饰品栏”按钮
        this.addRenderableWidget(new BaubleButton(leftPos, topPos, false, btn ->
                NetworkHandler.CHANNEL.sendToServer(
                        new OpenFoxMaidMenuPacket(target.getId(), true))));
        // Curios 按钮：仅显示与提示（女仆饰品不支持 Curios 装备）
        if (CuriosCompat.isLoadedOrEnable()) {
            this.addRenderableWidget(new CuriosButton(leftPos, topPos, false, btn -> {
            }));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        // 大背包贴图覆盖模型区域，装备槽与 36 格背包槽均落在贴图上
        graphics.blit(BACKPACK_BG, leftPos + 85, topPos + 36, 0, 0, 165, 128);
    }
}
