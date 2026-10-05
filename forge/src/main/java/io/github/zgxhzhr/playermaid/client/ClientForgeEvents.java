package io.github.zgxhzhr.playermaid.client;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidMenuPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端 Forge 事件：
 * <ul>
 *     <li>人是狐状态下拦截背包键（默认 E），改为打开自己的人是狐主界面（含创造模式）；</li>
 *     <li>断开连接时清空客户端状态缓存。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        KeyMapping inventoryKey = mc.options.keyInventory;
        if (!inventoryKey.matches(event.getKey(), event.getScanCode())) {
            return;
        }
        // 人是狐状态：吞掉原版背包键点击计数，改发打开人是狐界面的请求
        if (ClientFoxState.isActive(mc.player)) {
            inventoryKey.consumeClick();
            NetworkHandler.CHANNEL.sendToServer(new OpenFoxMaidMenuPacket(-1, false));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFoxState.clear();
    }
}
