package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 迅捷布料（nimble_fabric）饰品——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code NimbleFabricBauble}：佩戴者被弹射物命中时，消耗 1 点耐久完全抵挡
 * 并随机瞬移到附近安全位置（最多尝试 16 次）。</p>
 *
 * <p>降级说明：原版经车万女仆 {@code MaidAttackEvent} 订阅与 {@code TeleportHelper}
 * 实现；本移植在玩家侧用 {@link LivingEntity#randomTeleport} 等价实现随机瞬移
 * （女仆侧原实例订阅在车万女仆初始化时已注册，覆盖绑定后依然生效，女仆不受影响）。</p>
 */
public class PlayerMaidNimbleFabricBauble implements PlayerMaidBauble {

    private static final int MAX_RETRY = 16;
    private static final double TELEPORT_RADIUS = 8.0D;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：弹射物闪避由玩家侧事件实现
    }

    /** 玩家侧：弹射物命中时抵挡并随机瞬移（由 {@link PlayerMaidBaubleEvents#onLivingAttack} 调用）。 */
    public boolean tryDodge(LivingEntity entity, ItemStack stack) {
        stack.hurtAndBreak(1, entity, m -> {
        });
        if (entity instanceof Player player) {
            for (int i = 0; i < MAX_RETRY; i++) {
                double x = player.getX() + (player.getRandom().nextDouble() - 0.5D) * TELEPORT_RADIUS * 2.0D;
                double z = player.getZ() + (player.getRandom().nextDouble() - 0.5D) * TELEPORT_RADIUS * 2.0D;
                double y = player.getY() + 0.5D;
                if (player.randomTeleport(x, y, z, false)) {
                    return true;
                }
            }
        }
        return true;
    }
}
