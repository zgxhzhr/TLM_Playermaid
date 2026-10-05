package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import io.github.zgxhzhr.playermaid.client.FoxMaidStateView;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.ScheduleMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C：同步单个玩家的人是狐标量状态给客户端（不含背包/饰品物品）。
 */
public class SyncFoxStatePacket {

    private final int entityId;
    private final boolean active;
    private final String renderName;
    private final String ownerName;
    private final int favorability;
    private final ScheduleMode schedule;
    private final boolean invulnerable;
    private final String taskUid;
    private final boolean hasHalo;

    public SyncFoxStatePacket(int entityId, boolean active, String renderName, String ownerName,
                              int favorability, ScheduleMode schedule, boolean invulnerable,
                              String taskUid, boolean hasHalo) {
        this.entityId = entityId;
        this.active = active;
        this.renderName = renderName;
        this.ownerName = ownerName;
        this.favorability = favorability;
        this.schedule = schedule;
        this.invulnerable = invulnerable;
        this.taskUid = taskUid;
        this.hasHalo = hasHalo;
    }

    public static SyncFoxStatePacket of(int entityId, FoxMaidData data) {
        return new SyncFoxStatePacket(entityId, data.isActive(), data.getRenderName(), data.getOwnerName(),
                data.getFavorability(), data.getSchedule(), data.isInvulnerable(),
                data.getTaskUid(), data.hasHaloBauble());
    }

    public static void encode(SyncFoxStatePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeBoolean(packet.active);
        buf.writeBoolean(packet.renderName != null);
        if (packet.renderName != null) {
            buf.writeUtf(packet.renderName);
        }
        buf.writeBoolean(packet.ownerName != null);
        if (packet.ownerName != null) {
            buf.writeUtf(packet.ownerName);
        }
        buf.writeInt(packet.favorability);
        buf.writeEnum(packet.schedule);
        buf.writeBoolean(packet.invulnerable);
        buf.writeUtf(packet.taskUid);
        buf.writeBoolean(packet.hasHalo);
    }

    public static SyncFoxStatePacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        boolean active = buf.readBoolean();
        String renderName = buf.readBoolean() ? buf.readUtf() : null;
        String ownerName = buf.readBoolean() ? buf.readUtf() : null;
        int favorability = buf.readInt();
        ScheduleMode schedule = buf.readEnum(ScheduleMode.class);
        boolean invulnerable = buf.readBoolean();
        String taskUid = buf.readUtf();
        boolean hasHalo = buf.readBoolean();
        return new SyncFoxStatePacket(entityId, active, renderName, ownerName, favorability, schedule, invulnerable, taskUid, hasHalo);
    }

    public static void handle(SyncFoxStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                // 仅客户端执行：写入客户端状态缓存
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFoxState.update(
                        new FoxMaidStateView(packet.entityId, packet.active, packet.renderName, packet.ownerName,
                                packet.favorability, packet.schedule, packet.invulnerable, packet.taskUid, packet.hasHalo)))
        );
        context.setPacketHandled(true);
    }
}
