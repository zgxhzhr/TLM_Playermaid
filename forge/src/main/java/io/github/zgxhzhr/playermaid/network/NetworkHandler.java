package io.github.zgxhzhr.playermaid.network;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidMenuPacket;
import io.github.zgxhzhr.playermaid.network.packet.SyncFoxStatePacket;
import io.github.zgxhzhr.playermaid.network.packet.UpdateFoxSchedulePacket;
import io.github.zgxhzhr.playermaid.network.packet.UpdateFoxTaskPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组网络通道注册中心。
 */
public final class NetworkHandler {

    /** 协议版本，服务端与客户端不一致时拒绝连接。 */
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Constants.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private NetworkHandler() {
    }

    /**
     * 注册所有网络包。在模组入口调用一次。
     */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SyncFoxStatePacket.class,
                SyncFoxStatePacket::encode, SyncFoxStatePacket::decode, SyncFoxStatePacket::handle);
        CHANNEL.registerMessage(id++, OpenFoxMaidMenuPacket.class,
                OpenFoxMaidMenuPacket::encode, OpenFoxMaidMenuPacket::decode, OpenFoxMaidMenuPacket::handle);
        CHANNEL.registerMessage(id++, UpdateFoxSchedulePacket.class,
                UpdateFoxSchedulePacket::encode, UpdateFoxSchedulePacket::decode, UpdateFoxSchedulePacket::handle);
        CHANNEL.registerMessage(id++, UpdateFoxTaskPacket.class,
                UpdateFoxTaskPacket::encode, UpdateFoxTaskPacket::decode, UpdateFoxTaskPacket::handle);
    }
}
