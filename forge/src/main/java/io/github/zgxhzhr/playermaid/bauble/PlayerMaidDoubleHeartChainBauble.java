package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 双心之链（double_heart_chain）饰品逻辑——人是狐玩家版（降级）。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code DoubleHeartChainBauble}：原版把女仆受到伤害的 50%
 * （镜像 Config.doubleHeartChainShareRatio 默认值 0.5）转移给主人实体承担
 * （经其 InfoDamageSource 重定向管线；组合梦云水晶时女仆同样只受 50%）。</p>
 *
 * <p><b>降级说明</b>：人是狐玩家的「主人」仅为显示名（FoxMaidData.ownerName，无实体引用），
 * 无法承担伤害，故被转移的 50% 直接消失，等效「佩戴者受伤减免 50%」。
 * 与万法皆通原版差异：原版主人承受另一半，本移植另一半无人承担；
 * 梦云水晶组合（50%）与原版一致。因此本件不覆盖车万女仆绑定，
 * 女仆佩戴仍走万法皆通原版逻辑（见 {@link PlayerMaidBaubleRegistry} 覆盖策略）。</p>
 */
public class PlayerMaidDoubleHeartChainBauble implements PlayerMaidBauble {

    /** 佩戴者承受的伤害比例（镜像 Config.doubleHeartChainShareRatio 默认值 0.5）。 */
    public static final float SELF_DAMAGE_RATIO = 0.5F;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：伤害分担由玩家侧事件等价物实现
    }
}
