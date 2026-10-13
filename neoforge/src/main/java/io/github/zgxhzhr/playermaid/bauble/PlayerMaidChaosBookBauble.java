package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 混沌之书（chaos_book）饰品逻辑——人是狐玩家版（降级）。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code ChaosBookBauble}：佩戴者直接攻击时，对目标造成真实伤害
 * （max(5, 目标最大生命×1%)，镜像 Config.chaosBookTrueDamageMin/chaosBookTrueDamagePercent
 * 默认值；组合梦云水晶时翻倍）。</p>
 *
 * <p><b>降级说明</b>：原版另有「把一次攻击拆分为 N=5 次 InfoDamageSource 命中」的伤害转换
 * 管线（依赖其私有 InfoDamageSource/重入保护），无法复刻，降级为「保留原伤害 + 额外真实伤害」；
 * 真实伤害为简化实现（直接削减生命，不经过其 EntityData/NBT 混合写入管线，与梦云水晶
 * 玩家侧同款）。因此本件不覆盖车万女仆绑定，女仆佩戴仍走万法皆通原版逻辑。</p>
 */
public class PlayerMaidChaosBookBauble implements PlayerMaidBauble {

    /** 真实伤害下限（镜像 Config.chaosBookTrueDamageMin 默认值 5.0）。 */
    public static final float TRUE_DAMAGE_MIN = 5.0F;
    /** 真实伤害比例（镜像 Config.chaosBookTrueDamagePercent 默认值 0.01）。 */
    public static final double TRUE_DAMAGE_PERCENT = 0.01D;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：攻击真实伤害由玩家侧事件等价物实现
    }

    /** 真实伤害金额（组合梦云水晶翻倍），由 {@link PlayerMaidBaubleEvents#onLivingHurt} 调用。 */
    public static float trueDamageAmount(LivingEntity target, boolean withDreamCrystal) {
        float multiplier = withDreamCrystal ? 2.0F : 1.0F;
        float base = Math.max(TRUE_DAMAGE_MIN, target.getMaxHealth() * (float) TRUE_DAMAGE_PERCENT);
        return base * multiplier;
    }

    /** 简化真实伤害：直接削减目标生命（目标非玩家/女仆；死亡交由原版死亡流程）。 */
    public static void dealTrueDamage(LivingEntity target, float amount, LivingEntity attacker) {
        if (target == null || target instanceof Player || target instanceof EntityMaid) {
            return;
        }
        if (amount <= 0.0F || target.level().isClientSide() || !target.isAlive()) {
            return;
        }
        float newHealth = Math.max(0.0F, target.getHealth() - amount);
        target.setHealth(newHealth);
        if (newHealth <= 0.0F && target.isAlive()) {
            if (attacker instanceof Player player) {
                target.die(target.damageSources().playerAttack(player));
            } else if (attacker != null) {
                target.die(target.damageSources().mobAttack(attacker));
            } else {
                target.die(target.damageSources().generic());
            }
        }
    }
}
