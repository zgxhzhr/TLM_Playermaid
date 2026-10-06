package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 发簪（hairpin）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code HairpinBauble}：</p>
 * <ul>
 *     <li>每 10 tick 清除佩戴者身上的全部非正面效果；</li>
 *     <li>好感度 ≥2 级时正面效果持续时间延长（取 duration×1.15 与 duration+300 的较大者，
 *     镜像 Config.hairpinBeneficialEffectExtension/hairpinMinExtensionTicks 默认值）；
 *     好感度 ≥3 级时免疫一切非正面效果（玩家侧等价物见
 *     {@link PlayerMaidBaubleEvents#onEffectApplicable}）。</li>
 * </ul>
 *
 * <p>未移植项（依赖万法皆通内部管线，注释说明）：把女仆受到的伤害重定向给主人
 * （InfoDamageSource 重定向管线）；人是狐玩家的"主人"仅为显示名（无实体引用），
 * 无法映射，降级跳过。</p>
 */
public class PlayerMaidHairpinBauble implements PlayerMaidBauble {

    /** 正面效果延长倍率（镜像 Config.hairpinBeneficialEffectExtension 默认值 1.15）。 */
    public static final double BENEFICIAL_EXTENSION = 1.15D;
    /** 正面效果最小延长 tick（镜像 Config.hairpinMinExtensionTicks 默认值 300）。 */
    public static final int MIN_EXTENSION_TICKS = 300;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (tick % 10 == 0) {
            // 收集后再移除，避免迭代时 ConcurrentModification
            List<MobEffect> toRemove = entity.getActiveEffects().stream()
                    .map(MobEffectInstance::getEffect)
                    .filter(effect -> !effect.isBeneficial())
                    .toList();
            toRemove.forEach(entity::removeEffect);
        }
    }

    /**
     * 好感度 ≥2 级时延长正面效果（原样移植）。由 {@link PlayerMaidBaubleEvents} 调用，
     * 返回延长后的效果实例；不满足条件时返回原实例。
     */
    public static MobEffectInstance extendBeneficial(MobEffectInstance effectInstance, int favorabilityLevel) {
        if (favorabilityLevel < 2 || !effectInstance.getEffect().isBeneficial()) {
            return effectInstance;
        }
        int duration = effectInstance.getDuration();
        duration = Math.max((int) (duration * BENEFICIAL_EXTENSION), duration + MIN_EXTENSION_TICKS);
        MobEffectInstance extended = new MobEffectInstance(effectInstance.getEffect(), duration,
                effectInstance.getAmplifier(), effectInstance.isAmbient(), effectInstance.isVisible());
        effectInstance.update(extended);
        return effectInstance;
    }
}
