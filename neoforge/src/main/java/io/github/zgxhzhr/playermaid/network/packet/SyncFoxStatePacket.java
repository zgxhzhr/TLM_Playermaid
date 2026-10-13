package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.client.FoxMaidStateView;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.ScheduleMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：同步单个玩家的人是狐标量状态给客户端（不含背包/饰品物品本体，
 * 仅带「饰品栏是否含梦云水晶」标志，供玩家光环渲染）。
 *
 * @param entityId        玩家实体 id
 * @param active          是否处于人是狐状态
 * @param renderName      自定义渲染名（null 表示无）
 * @param ownerName       主人显示名（null 表示未设置）
 * @param ownerUuid       主人 UUID 字符串（真实实体引用，null 表示未认主）
 * @param favorability    好感度点数 0-384
 * @param schedule        日程模式
 * @param invulnerable    无敌展示开关
 * @param taskUid         当前工作模式（车万女仆任务 uid）
 * @param hasDreamCrystal 饰品栏是否含有梦云水晶
 */
public record SyncFoxStatePacket(int entityId, boolean active, String renderName, String ownerName,
                                 String ownerUuid, int favorability, ScheduleMode schedule,
                                 boolean invulnerable, String taskUid, boolean hasDreamCrystal)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncFoxStatePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_fox_state"));

    public static final StreamCodec<FriendlyByteBuf, SyncFoxStatePacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> packet.write(buf), SyncFoxStatePacket::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 由服务端数据构造。 */
    public static SyncFoxStatePacket of(int entityId, FoxMaidData data) {
        return new SyncFoxStatePacket(entityId, data.isActive(), data.getRenderName(), data.getOwnerName(),
                data.getOwnerUuid() == null ? null : data.getOwnerUuid().toString(),
                data.getFavorability(), data.getSchedule(), data.isInvulnerable(),
                data.getTaskUid(), io.github.zgxhzhr.playermaid.bauble.PlayerMaidDreamCatBauble
                        .containsDreamCrystal(data.getBaubles()));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(active);
        OpenFoxMaidSettingsPacket.writeNullableUtf(buf, renderName);
        OpenFoxMaidSettingsPacket.writeNullableUtf(buf, ownerName);
        OpenFoxMaidSettingsPacket.writeNullableUtf(buf, ownerUuid);
        buf.writeVarInt(favorability);
        buf.writeEnum(schedule);
        buf.writeBoolean(invulnerable);
        buf.writeUtf(taskUid);
        buf.writeBoolean(hasDreamCrystal);
    }

    private static SyncFoxStatePacket read(FriendlyByteBuf buf) {
        return new SyncFoxStatePacket(buf.readVarInt(), buf.readBoolean(),
                OpenFoxMaidSettingsPacket.readNullableUtf(buf),
                OpenFoxMaidSettingsPacket.readNullableUtf(buf),
                OpenFoxMaidSettingsPacket.readNullableUtf(buf),
                buf.readVarInt(), buf.readEnum(ScheduleMode.class), buf.readBoolean(),
                buf.readUtf(), buf.readBoolean());
    }

    public static void handle(SyncFoxStatePacket packet, IPayloadContext context) {
        // playToClient 载荷只在物理客户端执行；这里再按 Dist 兜一层，
        // 保证专用服务器上永远不会去解析客户端类（NeoForge 1.21 已移除 DistExecutor）。
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            ClientFoxState.update(new FoxMaidStateView(packet.entityId, packet.active, packet.renderName,
                    packet.ownerName, packet.ownerUuid, packet.favorability, packet.schedule,
                    packet.invulnerable, packet.taskUid, packet.hasDreamCrystal));
        });
    }
}
