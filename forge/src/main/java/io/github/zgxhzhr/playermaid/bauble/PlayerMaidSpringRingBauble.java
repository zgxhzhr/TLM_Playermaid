package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 春之戒（spring_ring）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code SpringBauble}：佩戴者直接攻击的伤害按已损失生命比例加成
 * （加成 = min(1 - 当前生命/最大生命, 0.5)，镜像 Config.springRingMaxDamageBonus 默认值 0.5）。
 * 原实现挂在万法皆通 Global 伤害管线；玩家侧等价物见
 * {@link PlayerMaidBaubleEvents#onLivingHurt}。</p>
 */
public class PlayerMaidSpringRingBauble implements PlayerMaidBauble {

    /** 伤害加成上限（镜像 Config.springRingMaxDamageBonus 默认值 0.5）。 */
    public static final double MAX_DAMAGE_BONUS = 0.5D;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：原实现仅挂 Global 伤害管线，玩家侧由事件等价物实现
    }

    /** 按已损失生命比例计算伤害倍率，由 {@link PlayerMaidBaubleEvents} 调用。 */
    public static float damageMultiplier(LivingEntity attacker) {
        float missingRatio = 1.0F - attacker.getHealth() / attacker.getMaxHealth();
        missingRatio = Math.min(missingRatio, (float) MAX_DAMAGE_BONUS);
        return 1.0F + missingRatio;
    }
}
