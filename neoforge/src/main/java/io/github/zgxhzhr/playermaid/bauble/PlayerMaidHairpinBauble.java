package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.core.Holder;

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
 * <p><b>本次补齐</b>：人是狐玩家认主后，「把受到的伤害转移给主人」已可如实生效——
 * 佩戴者受伤时把该次伤害施加给主人实体，并按主人实际损失的生命值扣减佩戴者这一侧
 * （与万法皆通原版 {@code HairpinBauble} 用主人实际情况回填 {@code capturedRedirectedAmount} 同义），
 * 结算见 {@link PlayerMaidBaubleEvents#onLivingHurt}。</p>
 *
 * <p>与原版的两点差异（均有意为之，见 {@link PlayerMaidBaubleEvents}）：
 * 主人不在场、不在同一维度或超出 {@link #MAX_REDIRECT_DISTANCE} 格时不转移；
 * 主人在该次伤害中实际未掉血（创造/旁观或已被其他守卫拦下）时不把佩戴者这一侧清零，
 * 而是保留原伤害，避免「创造模式主人让佩戴者免疫」的漏洞。</p>
 */
public class PlayerMaidHairpinBauble implements PlayerMaidBauble {

    /** 正面效果延长倍率（镜像 Config.hairpinBeneficialEffectExtension 默认值 1.15）。 */
    public static final double BENEFICIAL_EXTENSION = 1.15D;
    /** 正面效果最小延长 tick（镜像 Config.hairpinMinExtensionTicks 默认值 300）。 */
    public static final int MIN_EXTENSION_TICKS = 300;
    /** 伤害转移给主人的最大距离（原版未设距离限制，此处为跨维度/远距离兜底）。 */
    public static final double MAX_REDIRECT_DISTANCE = 32.0D;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (tick % 10 == 0) {
            // 收集后再移除，避免迭代时 ConcurrentModification
            List<Holder<MobEffect>> toRemove = entity.getActiveEffects().stream()
                    .map(MobEffectInstance::getEffect)
                    .filter(effect -> !effect.value().isBeneficial())
                    .toList();
            toRemove.forEach(entity::removeEffect);
        }
    }

    /**
     * 好感度 ≥2 级时延长正面效果（原样移植）。由 {@link PlayerMaidBaubleEvents} 调用，
     * 返回延长后的效果实例；不满足条件时返回原实例。
     */
    public static MobEffectInstance extendBeneficial(MobEffectInstance effectInstance, int favorabilityLevel) {
        if (favorabilityLevel < 2 || !effectInstance.getEffect().value().isBeneficial()) {
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
