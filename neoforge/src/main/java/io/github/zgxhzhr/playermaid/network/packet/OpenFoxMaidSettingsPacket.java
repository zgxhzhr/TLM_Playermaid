package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import io.github.zgxhzhr.playermaid.client.ClientFoxMaidSettings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

/**
 * S2C：打开「人是狐设置」界面，并携带打开瞬间的完整设置快照。
 *
 * <p>由 {@code /playermaidgui [玩家]} 在服务端校验通过后发出。界面本身不依赖任何容器
 * 同步，全部设置项随本包一次性送达；修改一律走
 * {@link SubmitFoxMaidSettingsPacket} 回传服务端校验落库，
 * 因此本模组不再需要安装调试器（superdbg）就能完整配置人是狐。</p>
 *
 * @param entityId      被配置玩家的实体 id
 * @param targetName    被配置玩家的显示名（界面标题用）
 * @param active        人是狐开关
 * @param renderName    自定义渲染名（null 表示未设置）
 * @param ownerName     主人显示名（null 表示未认主）
 * @param ownerUuid     主人 UUID 字符串（null 表示未认主）
 * @param favorability  好感度点数 0-384
 * @param schedule      日程模式名 DAY/NIGHT/ALL
 * @param invulnerable  无敌展示开关
 */
public record OpenFoxMaidSettingsPacket(int entityId, String targetName, boolean active,
                                        @Nullable String renderName, @Nullable String ownerName,
                                        @Nullable String ownerUuid, int favorability,
                                        String schedule, boolean invulnerable)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenFoxMaidSettingsPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "open_fox_maid_settings"));

    public static final StreamCodec<FriendlyByteBuf, OpenFoxMaidSettingsPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> packet.write(buf), OpenFoxMaidSettingsPacket::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(targetName);
        buf.writeBoolean(active);
        writeNullableUtf(buf, renderName);
        writeNullableUtf(buf, ownerName);
        writeNullableUtf(buf, ownerUuid);
        buf.writeVarInt(favorability);
        buf.writeUtf(schedule);
        buf.writeBoolean(invulnerable);
    }

    private static OpenFoxMaidSettingsPacket read(FriendlyByteBuf buf) {
        return new OpenFoxMaidSettingsPacket(buf.readVarInt(), buf.readUtf(), buf.readBoolean(),
                readNullableUtf(buf), readNullableUtf(buf), readNullableUtf(buf),
                buf.readVarInt(), buf.readUtf(), buf.readBoolean());
    }

    public static void handle(OpenFoxMaidSettingsPacket packet, IPayloadContext context) {
        // playToClient 载荷只在物理客户端执行；这里再按 Dist 兜一层，
        // 保证专用服务器上永远不会去解析客户端屏幕类（NeoForge 1.21 已移除 DistExecutor）。
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            ClientFoxMaidSettings.open(packet);
        });
    }

    /** 写可为空的字符串（先写存在标志）。 */
    public static void writeNullableUtf(FriendlyByteBuf buf, @Nullable String value) {
        buf.writeBoolean(value != null);
        if (value != null) {
            buf.writeUtf(value);
        }
    }

    /** 读可为空的字符串。 */
    @Nullable
    public static String readNullableUtf(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUtf() : null;
    }
}
