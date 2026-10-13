package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魂之书（soul_book）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code SoulBookBauble}（配合其 MaidDamageProcessor 管线）：
 * 佩戴者受伤时，若两次受伤间隔 ≤10 tick（镜像 Config.soulBookDamageIntervalThreshold 默认值）
 * 则取消本次伤害；否则伤害钳制为 min(原始伤害, 最大生命×20%)
 * （镜像 Config.soulBookDamageThresholdPercent 默认值 0.2）。
 * 原版通过拦截 setHealth 实现；玩家侧等价物见 {@link PlayerMaidBaubleEvents#onLivingHurt}。</p>
 */
public class PlayerMaidSoulBookBauble implements PlayerMaidBauble {

    /** 伤害钳制比例（镜像 Config.soulBookDamageThresholdPercent 默认值 0.2）。 */
    public static final float DAMAGE_THRESHOLD_PERCENT = 0.2F;
    /** 伤害间隔阈值 tick（镜像 Config.soulBookDamageIntervalThreshold 默认值 10）。 */
    public static final int DAMAGE_INTERVAL_THRESHOLD = 10;

    /** 佩戴者最近一次受伤 tick。 */
    private static final Map<UUID, Integer> LAST_HURT_TIME = new ConcurrentHashMap<>();

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：原实现经其伤害管线处理，玩家侧由事件等价物实现
    }

    @Override
    public void onUnequipLivingEntity(LivingEntity entity, ItemStack baubleItem) {
        LAST_HURT_TIME.remove(entity.getUUID());
    }

    /**
     * 玩家侧伤害处理：返回是否允许本次伤害（false 表示间隔内取消），
     * 并给出钳制后的伤害值。由 {@link PlayerMaidBaubleEvents#onLivingHurt} 调用。
     */
    public static float handleHurt(LivingEntity entity, float amount, int tick) {
        UUID uuid = entity.getUUID();
        int lastHurt = LAST_HURT_TIME.computeIfAbsent(uuid, ignored -> tick);
        int timeDiff = tick - lastHurt;
        if (timeDiff <= DAMAGE_INTERVAL_THRESHOLD) {
            // 间隔内二次受击：取消本次伤害（原版 dataItem.setCanceled）
            LAST_HURT_TIME.put(uuid, tick);
            return 0.0F;
        }
        LAST_HURT_TIME.put(uuid, tick);
        return Math.min(amount, entity.getMaxHealth() * DAMAGE_THRESHOLD_PERCENT);
    }
}
