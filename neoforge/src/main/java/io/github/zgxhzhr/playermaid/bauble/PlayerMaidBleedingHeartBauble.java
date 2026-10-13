package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 泣血之心（bleeding_heart）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code BleedingHeartBauble}：佩戴者直接攻击造成伤害时，按伤害的 10%
 * （镜像 Config.bleedingHeartHealRatio 默认值 0.1）回复生命。
 * 原实现挂在万法皆通 Global 伤害管线（女仆攻击→女仆与其主人回血）；玩家侧等价物见
 * {@link PlayerMaidBaubleEvents#onLivingHurt}。</p>
 *
 * <p><b>本次补齐</b>：人是狐玩家认主后（真实主人实体），「主人回血」部分已可如实生效——
 * 佩戴者造成伤害时，佩戴者与其主人各回复伤害的 {@link #HEAL_RATIO}，
 * 与万法皆通原版「女仆攻击→女仆与其主人回血」一致；
 * 未认主或主人不在场时仅佩戴者自身回血（保留此前的降级行为）。
 * 结算见 {@link PlayerMaidBaubleEvents#onLivingHurt}。</p>
 */
public class PlayerMaidBleedingHeartBauble implements PlayerMaidBauble {

    /** 回血比例（镜像 Config.bleedingHeartHealRatio 默认值 0.1）。 */
    public static final float HEAL_RATIO = 0.1F;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：原实现仅挂 Global 伤害管线，玩家侧由事件等价物实现
    }
}
