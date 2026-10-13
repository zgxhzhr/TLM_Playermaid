package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 流心核心（flow_core）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code FlowCoreBauble}：按好感度等级每 10 tick 回复生命
 * （每级回复最大生命的 2.5%，镜像 Config.flowCoreHealthRegenRate/flowCoreTickInterval 默认值）；
 * 被击时按好感度等级减免伤害（每级 15%，镜像 Config.flowCoreDamageReduction 默认值，
 * 原实现挂在万法皆通 Global 伤害管线，玩家侧等价物见
 * {@link PlayerMaidBaubleEvents#onLivingHurt}）。好感度等级映射到人是狐数据
 * （女仆取车万女仆好感度等级，玩家取 {@link io.github.zgxhzhr.playermaid.data.FoxMaidData}）。</p>
 */
public class PlayerMaidFlowCoreBauble implements PlayerMaidBauble {

    /** 回血间隔 tick（镜像 Config.flowCoreTickInterval 默认值 10）。 */
    private static final int TICK_INTERVAL = 10;
    /** 每好感度等级回血比例（镜像 Config.flowCoreHealthRegenRate 默认值 0.025）。 */
    private static final double HEALTH_REGEN_RATE = 0.025D;
    /** 每好感度等级伤害减免比例（镜像 Config.flowCoreDamageReduction 默认值 0.15）。 */
    public static final double DAMAGE_REDUCTION = 0.15D;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (tick % TICK_INTERVAL == 0) {
            int favorabilityLevel = PlayerMaidBaubleRegistry.favorabilityLevel(entity);
            entity.setHealth(entity.getHealth() + (float) (entity.getMaxHealth() * HEALTH_REGEN_RATE * favorabilityLevel));
        }
    }

    /** 伤害减免（玩家侧等价物）：按好感度等级减免，由 {@link PlayerMaidBaubleEvents} 调用。 */
    public static float reduceDamageByFavorability(int favorabilityLevel, float amount) {
        return amount * (1.0F - (float) (DAMAGE_REDUCTION * favorabilityLevel));
    }
}
