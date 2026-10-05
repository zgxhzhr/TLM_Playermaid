package io.github.zgxhzhr.playermaid.network;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 网络初始化入口：在公共 setup 阶段注册网络包。
 */
public final class NetworkInit {

    private NetworkInit() {
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}
