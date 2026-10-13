package io.github.zgxhzhr.playermaid.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 网络初始化入口：1.21 的载荷注册发生在 mod 事件总线的
 * {@link RegisterPayloadHandlersEvent}，不再需要 {@code FMLCommonSetupEvent}。
 */
public final class NetworkInit {

    private NetworkInit() {
    }

    /** 由主类以 {@code modEventBus.addListener(NetworkInit::registerPayloads)} 挂载。 */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        NetworkHandler.register(event);
    }
}
