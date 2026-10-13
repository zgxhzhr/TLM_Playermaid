package io.github.zgxhzhr.playermaid.network;

import net.neoforged.neoforge.network.PacketDistributor;

import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidMenuPacket;
import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidSettingsPacket;
import io.github.zgxhzhr.playermaid.network.packet.SubmitFoxMaidSettingsPacket;
import io.github.zgxhzhr.playermaid.network.packet.SyncFoxStatePacket;
import io.github.zgxhzhr.playermaid.network.packet.UpdateFoxSchedulePacket;
import io.github.zgxhzhr.playermaid.network.packet.UpdateFoxTaskPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 模组网络通道注册中心（NeoForge 1.21.1 载荷式 API）。
 *
 * <p>1.20.1 的 {@code SimpleChannel} 在 1.21 已被 {@code CustomPacketPayload} +
 * {@link RegisterPayloadHandlersEvent} 取代：每个包自带 {@code TYPE} 与
 * {@code STREAM_CODEC}，注册时按方向声明，发送改用
 * {@code PacketDistributor.sendToServer / sendToPlayer / sendToPlayersTrackingEntityAndSelf}。</p>
 *
 * <p>协议版本由 {@code registrar(version)} 声明，两端不一致时 NeoForge 拒绝连接，
 * 与 1.20.1 的 {@code PROTOCOL_VERSION} 校验等效。</p>
 */
public final class NetworkHandler {

    /**
     * 协议版本。3 = 1.21.1 移植版起始版本；1.20.1 末版为 2
     * （状态包含主人 UUID 字段），本版在此基础上另含设置界面相关载荷。
     */
    public static final String PROTOCOL_VERSION = "3";

    private NetworkHandler() {
    }

    /** 注册全部网络载荷。由 {@link NetworkInit} 在 mod 事件总线上调用。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // S2C：人是狐标量状态同步
        registrar.playToClient(SyncFoxStatePacket.TYPE, SyncFoxStatePacket.STREAM_CODEC,
                SyncFoxStatePacket::handle);
        // S2C：打开设置界面（携带整份设置快照）
        registrar.playToClient(OpenFoxMaidSettingsPacket.TYPE, OpenFoxMaidSettingsPacket.STREAM_CODEC,
                OpenFoxMaidSettingsPacket::handle);

        // C2S：请求打开主界面/饰品界面
        registrar.playToServer(OpenFoxMaidMenuPacket.TYPE, OpenFoxMaidMenuPacket.STREAM_CODEC,
                OpenFoxMaidMenuPacket::handle);
        // C2S：切换日程
        registrar.playToServer(UpdateFoxSchedulePacket.TYPE, UpdateFoxSchedulePacket.STREAM_CODEC,
                UpdateFoxSchedulePacket::handle);
        // C2S：切换工作模式（任务）
        registrar.playToServer(UpdateFoxTaskPacket.TYPE, UpdateFoxTaskPacket.STREAM_CODEC,
                UpdateFoxTaskPacket::handle);
        // C2S：提交设置界面
        registrar.playToServer(SubmitFoxMaidSettingsPacket.TYPE, SubmitFoxMaidSettingsPacket.STREAM_CODEC,
                SubmitFoxMaidSettingsPacket::handle);
    }
}
