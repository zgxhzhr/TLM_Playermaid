package io.github.zgxhzhr.playermaid.data;

import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 「人是狐」状态判定工具。
 * <p>
 * 优先服务端权威缓存；未命中时回退玩家 persistentData（客户端/未缓存场景）。
 * 供床交互事件与相关 mixin 共用，避免在 mixin 类中暴露非 private 方法
 * （mixin 规范要求混入类新增方法必须为 private）。
 */
public final class FoxMaidStatus {

    private FoxMaidStatus() {
    }

    /** 玩家当前是否处于人是狐状态（与 {@code level().isClientSide} 无关的纯状态判断）。 */
    public static boolean isActive(Player player) {
        FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
        if (data != null) {
            return data.isActive();
        }
        CompoundTag root = player.getPersistentData();
        return root.contains(Constants.ROOT_TAG)
                && root.getCompound(Constants.ROOT_TAG).getBoolean("Active");
    }
}
