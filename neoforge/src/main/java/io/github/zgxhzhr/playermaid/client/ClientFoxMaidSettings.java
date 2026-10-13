package io.github.zgxhzhr.playermaid.client;

import io.github.zgxhzhr.playermaid.client.screen.FoxMaidSettingsScreen;
import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidSettingsPacket;
import net.minecraft.client.Minecraft;

/**
 * 客户端设置界面入口（服务端 {@code OpenFoxMaidSettingsPacket} 到达后调用）。
 *
 * <p>单独成类是为了把客户端类引用挡在 {@code DistExecutor} 之后，避免服务端加载客户端屏幕类。</p>
 */
public final class ClientFoxMaidSettings {

    private ClientFoxMaidSettings() {
    }

    /** 打开设置界面（已在客户端主线程执行）。 */
    public static void open(OpenFoxMaidSettingsPacket packet) {
        Minecraft.getInstance().setScreen(new FoxMaidSettingsScreen(packet));
    }
}
