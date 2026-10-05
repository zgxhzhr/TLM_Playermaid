package io.github.zgxhzhr.playermaid.client.screen;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.BaubleButton;
import com.github.tartaricacid.touhoulittlemaid.compat.curios.CuriosCompat;
import com.github.tartaricacid.touhoulittlemaid.compat.curios.client.CuriosButton;
import io.github.zgxhzhr.playermaid.menu.FoxMaidBaubleMenu;
import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidMenuPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 人是狐女仆饰品栏界面：{@code maid_gui_bauble.png} 叠加，30 格饰品槽默认全部解锁。
 */
public class FoxMaidBaubleScreen extends FoxMaidAbstractScreen<FoxMaidBaubleMenu> {

    private static final ResourceLocation BAUBLE_BG =
            new ResourceLocation(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_bauble.png");

    public FoxMaidBaubleScreen(FoxMaidBaubleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory);
    }

    @Override
    protected void initAdditionWidgets() {
        // 饰品栏打开态按钮：点击返回主界面
        this.addRenderableWidget(new BaubleButton(leftPos, topPos, true, btn ->
                NetworkHandler.CHANNEL.sendToServer(
                        new OpenFoxMaidMenuPacket(target.getId(), false))));
        // Curios 按钮：仅显示与提示（女仆饰品不支持 Curios 装备）
        if (CuriosCompat.isLoadedOrEnable()) {
            this.addRenderableWidget(new CuriosButton(leftPos, topPos, false, btn -> {
            }));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        graphics.blit(BAUBLE_BG, leftPos + 85, topPos + 36, 0, 0, 165, 128);
    }
}
