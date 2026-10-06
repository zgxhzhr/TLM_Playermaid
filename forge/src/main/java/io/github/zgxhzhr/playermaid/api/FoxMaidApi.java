package io.github.zgxhzhr.playermaid.api;

import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import io.github.zgxhzhr.playermaid.data.ScheduleMode;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * 人是狐对外 API（反射门面）。
 *
 * <p>全部为静态方法，参数类型仅使用 JDK / {@link ServerPlayer} / String / int / boolean，
 * 便于其他模组（如超级调试器）在不引入编译依赖的情况下，通过反射安全调用。
 * 所有写操作会立即落盘并向本人与追踪者同步。</p>
 */
public final class FoxMaidApi {

    private static final Logger LOGGER = LogUtils.getLogger();

    private FoxMaidApi() {
    }

    /** 本 API 是否可用（供反射方容错调用）。 */
    public static boolean isPresent() {
        return true;
    }

    /** 玩家是否处于人是狐状态。 */
    public static boolean isActive(ServerPlayer player) {
        return FoxMaidManager.getOrCreate(player).isActive();
    }

    /**
     * 开关人是狐状态。
     *
     * <p>状态本身不影响玩家真实背包：界面顶部网格始终直接映射真实背包。</p>
     */
    public static void setActive(ServerPlayer player, boolean active) {
        FoxMaidData data = FoxMaidManager.getOrCreate(player);
        data.setActive(active);
        FoxMaidManager.persistAndSync(player);
    }

    /** 自定义渲染名（仅头顶名牌与 Jade 标题），未设置返回 null。 */
    @Nullable
    public static String getRenderName(ServerPlayer player) {
        return FoxMaidManager.getOrCreate(player).getRenderName();
    }

    /** 设置自定义渲染名；传 null 或空串清除。 */
    public static void setRenderName(ServerPlayer player, @Nullable String renderName) {
        FoxMaidData data = FoxMaidManager.getOrCreate(player);
        data.setRenderName(renderName);
        FoxMaidManager.persistAndSync(player);
    }

    /** 主人显示名，未设置返回 null。 */
    @Nullable
    public static String getOwnerName(ServerPlayer player) {
        return FoxMaidManager.getOrCreate(player).getOwnerName();
    }

    /** 设置主人显示名；传 null 或空串清除。 */
    public static void setOwnerName(ServerPlayer player, @Nullable String ownerName) {
        FoxMaidData data = FoxMaidManager.getOrCreate(player);
        data.setOwnerName(ownerName);
        FoxMaidManager.persistAndSync(player);
    }

    /** 魂符展示模型 id（车万女仆女仆模型 id），未设置返回 null。 */
    @Nullable
    public static String getSlabModelId(ServerPlayer player) {
        String result = FoxMaidManager.getOrCreate(player).getSlabModelId();
        LOGGER.info("[playermaid] FoxMaidApi.getSlabModelId: player={} result={}",
                player.getName().getString(), result);
        return result;
    }

    /** 设置魂符展示模型 id；传 null 或空串清除。仅服务端持久化，无需客户端同步包。 */
    public static void setSlabModelId(ServerPlayer player, @Nullable String slabModelId) {
        LOGGER.info("[playermaid] FoxMaidApi.setSlabModelId: player={} received={}",
                player.getName().getString(), slabModelId);
        FoxMaidData data = FoxMaidManager.getOrCreate(player);
        data.setSlabModelId(slabModelId);
        FoxMaidManager.persistAndSync(player);
    }

    /** 好感度点数（0-384）。 */
    public static int getFavorability(ServerPlayer player) {
        return FoxMaidManager.getOrCreate(player).getFavorability();
    }

    /** 设置好感度点数（自动钳制到 0-384）。 */
    public static void setFavorability(ServerPlayer player, int favorability) {
        FoxMaidData data = FoxMaidManager.getOrCreate(player);
        data.setFavorability(favorability);
        FoxMaidManager.persistAndSync(player);
    }

    /**
     * 日程模式名：{@code DAY} / {@code NIGHT} / {@code ALL}。
     */
    public static String getSchedule(ServerPlayer player) {
        return FoxMaidManager.getOrCreate(player).getSchedule().name();
    }

    /**
     * 按名称设置日程（{@code DAY} / {@code NIGHT} / {@code ALL}），非法值回退白天。
     */
    public static void setSchedule(ServerPlayer player, String schedule) {
        FoxMaidData data = FoxMaidManager.getOrCreate(player);
        data.setSchedule(ScheduleMode.fromName(schedule));
        FoxMaidManager.persistAndSync(player);
    }

    /** 是否无敌（展示属性）。 */
    public static boolean isInvulnerable(ServerPlayer player) {
        return FoxMaidManager.getOrCreate(player).isInvulnerable();
    }

    /** 设置无敌（展示属性）。 */
    public static void setInvulnerable(ServerPlayer player, boolean invulnerable) {
        FoxMaidData data = FoxMaidManager.getOrCreate(player);
        data.setInvulnerable(invulnerable);
        FoxMaidManager.persistAndSync(player);
    }
}
