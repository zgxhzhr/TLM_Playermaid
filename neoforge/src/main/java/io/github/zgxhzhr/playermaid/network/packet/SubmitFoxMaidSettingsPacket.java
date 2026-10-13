package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidSettingsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

/**
 * C2S：提交「人是狐设置」界面的整份修改。
 *
 * <p>一次性回传全部字段（与调试器实体编辑器提交同风格），服务端先做权限与取值校验
 * 再落库同步，避免逐项包带来的半更新状态。</p>
 *
 * @param entityId     被配置玩家的实体 id
 * @param active       人是狐开关
 * @param renderName   自定义渲染名（null/空 表示清除）
 * @param ownerUuid    主人 UUID 字符串（null/空 表示清除主人）
 * @param favorability 好感度点数（服务端钳制 0-384）
 * @param schedule     日程模式名 DAY/NIGHT/ALL（非法值回退白天）
 * @param invulnerable 无敌展示开关
 */
public record SubmitFoxMaidSettingsPacket(int entityId, boolean active, @Nullable String renderName,
                                          @Nullable String ownerUuid, int favorability,
                                          String schedule, boolean invulnerable)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SubmitFoxMaidSettingsPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "submit_fox_maid_settings"));

    public static final StreamCodec<FriendlyByteBuf, SubmitFoxMaidSettingsPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> packet.write(buf), SubmitFoxMaidSettingsPacket::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(active);
        OpenFoxMaidSettingsPacket.writeNullableUtf(buf, renderName);
        OpenFoxMaidSettingsPacket.writeNullableUtf(buf, ownerUuid);
        buf.writeVarInt(favorability);
        buf.writeUtf(schedule);
        buf.writeBoolean(invulnerable);
    }

    private static SubmitFoxMaidSettingsPacket read(FriendlyByteBuf buf) {
        return new SubmitFoxMaidSettingsPacket(buf.readVarInt(), buf.readBoolean(),
                OpenFoxMaidSettingsPacket.readNullableUtf(buf),
                OpenFoxMaidSettingsPacket.readNullableUtf(buf),
                buf.readVarInt(), buf.readUtf(), buf.readBoolean());
    }

    public static void handle(SubmitFoxMaidSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) {
                FoxMaidSettingsService.apply(sender, packet);
            }
        });
    }
}
