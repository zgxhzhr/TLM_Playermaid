package io.github.zgxhzhr.playermaid.data;

import net.neoforged.neoforge.network.PacketDistributor;

import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import io.github.zgxhzhr.playermaid.network.packet.OpenFoxMaidSettingsPacket;
import io.github.zgxhzhr.playermaid.network.packet.SubmitFoxMaidSettingsPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * 「人是狐设置」界面的服务端入口与落库逻辑。
 *
 * <p>把这套设置从调试器（superdbg）手里接回来：指令 {@code /playermaidgui [玩家]}
 * 经本类校验后直接向请求者发送 {@link OpenFoxMaidSettingsPacket} 打开界面，
 * 界面提交再由 {@link #apply} 校验并写回。因此不安装调试器也能完整配置人是狐。</p>
 *
 * <p>权限规则（指令与提交包两处都执行，防止客户端伪造 entityId 绕过指令层校验）：</p>
 * <ul>
 *     <li>配置自己：任何玩家可用；</li>
 *     <li>配置他人：需要 OP 等级 ≥ 2。</li>
 * </ul>
 */
public final class FoxMaidSettingsService {

    /** 自定义渲染名最大长度（与界面输入框一致）。 */
    private static final int MAX_RENDER_NAME_LENGTH = 32;

    private FoxMaidSettingsService() {
    }

    /**
     * 打开设置界面。
     *
     * @param viewer 请求者
     * @param target 被配置的玩家（可与 viewer 相同）
     * @return 是否成功发出界面包
     */
    public static boolean open(ServerPlayer viewer, ServerPlayer target) {
        if (!viewer.getUUID().equals(target.getUUID()) && !viewer.hasPermissions(2)) {
            viewer.displayClientMessage(
                    Component.literal("§c权限不足：配置其他玩家需要 OP 等级 2"), false);
            return false;
        }
        FoxMaidData data = FoxMaidManager.getOrCreate(target);
        PacketDistributor.sendToPlayer(viewer,
                new OpenFoxMaidSettingsPacket(target.getId(), target.getName().getString(),
                        data.isActive(), data.getRenderName(), data.getOwnerName(),
                        data.getOwnerUuid() == null ? null : data.getOwnerUuid().toString(),
                        data.getFavorability(), data.getSchedule().name(), data.isInvulnerable()));
        return true;
    }

    /**
     * 应用界面提交的整份设置。目标玩家不存在时静默忽略。
     */
    public static void apply(ServerPlayer viewer, SubmitFoxMaidSettingsPacket packet) {
        ServerPlayer target = resolveTarget(viewer, packet.entityId());
        if (target == null) {
            return;
        }
        if (!viewer.getUUID().equals(target.getUUID()) && !viewer.hasPermissions(2)) {
            viewer.displayClientMessage(
                    Component.literal("§c权限不足：配置其他玩家需要 OP 等级 2"), false);
            return;
        }

        FoxMaidData data = FoxMaidManager.getOrCreate(target);
        data.setActive(packet.active());
        data.setRenderName(clampRenderName(packet.renderName()));
        data.setFavorability(packet.favorability());
        data.setSchedule(ScheduleMode.fromName(packet.schedule()));
        data.setInvulnerable(packet.invulnerable());
        // 主人：只接受当前在线玩家的 UUID（解析不到即视为清除主人，并明确告知）
        java.util.UUID requestedOwner = parseUuid(packet.ownerUuid());
        FoxMaidManager.applyOwnerByUuid(data, target, requestedOwner);
        // 整份设置一次性落库并广播
        FoxMaidManager.persistAndSync(target);
        if (requestedOwner != null && !data.hasOwner()) {
            viewer.displayClientMessage(
                    Component.literal("§e所选主人已不在线，本次未建立主人关系（其余设置已保存）"), false);
            return;
        }
        viewer.displayClientMessage(Component.literal("§a已保存 " + target.getName().getString()
                + " 的人是狐设置"), true);
    }

    /** 按实体 id 解析目标玩家：-1 或自身 id 表示自己。 */
    @Nullable
    private static ServerPlayer resolveTarget(ServerPlayer viewer, int entityId) {
        if (entityId == -1 || entityId == viewer.getId()) {
            return viewer;
        }
        return viewer.level().getEntity(entityId) instanceof ServerPlayer player ? player : null;
    }

    /** 渲染名：去空白、超长截断、空串归一化为 null（清除语义）。 */
    @Nullable
    private static String clampRenderName(@Nullable String renderName) {
        if (renderName == null) {
            return null;
        }
        String trimmed = renderName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= MAX_RENDER_NAME_LENGTH
                ? trimmed
                : trimmed.substring(0, MAX_RENDER_NAME_LENGTH);
    }

    /** 解析主人 UUID 字符串；null/空/非法一律返回 null（清除主人）。 */
    @Nullable
    private static java.util.UUID parseUuid(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
