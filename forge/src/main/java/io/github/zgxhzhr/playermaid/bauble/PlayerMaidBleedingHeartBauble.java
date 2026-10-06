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
 * <p>映射说明：原版"女仆及其主人回血"中"主人"为车万女仆女仆的绑定主人实体；
 * 人是狐玩家即女仆本身，主人仅存显示名（FoxMaidData.ownerName，无实体引用），
 * 故主人回血部分降级为仅玩家自身回血，此处注释说明。</p>
 */
public class PlayerMaidBleedingHeartBauble implements PlayerMaidBauble {

    /** 回血比例（镜像 Config.bleedingHeartHealRatio 默认值 0.1）。 */
    public static final float HEAL_RATIO = 0.1F;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：原实现仅挂 Global 伤害管线，玩家侧由事件等价物实现
    }
}
