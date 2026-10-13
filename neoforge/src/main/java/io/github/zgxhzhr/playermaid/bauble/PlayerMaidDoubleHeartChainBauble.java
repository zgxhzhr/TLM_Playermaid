package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 双心之链（double_heart_chain）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code DoubleHeartChainBauble}：原版把女仆受到伤害的 50%
 * （镜像 Config.doubleHeartChainShareRatio 默认值 0.5）转移给主人实体承担
 * （经其 InfoDamageSource 重定向管线；组合梦云水晶时女仆同样只受 50%）。</p>
 *
 * <p><b>本次补齐</b>：人是狐玩家认主后（真实主人实体，见
 * {@link io.github.zgxhzhr.playermaid.data.FoxMaidData#getOwnerUuid()}）已可如实分担——
 * 佩戴者只受 {@link #SELF_DAMAGE_RATIO}，另一半由主人实体承受，
 * 距离超过 {@link #MAX_SHARE_DISTANCE} 格时不再分担、佩戴者承受全额，
 * 组合梦云水晶时主人不承担、佩戴者只受 50%（三者均与万法皆通原版一致）。
 * 伤害结算见 {@link PlayerMaidBaubleEvents#onLivingHurt}。</p>
 *
 * <p>与万法皆通原版仅存的一处差异：<b>未认主或主人不在场时</b>保留本模组此前的降级行为
 * （直接减免 50%，被分担部分无人承担），以免未认主玩家因这次改动而变脆。</p>
 */
public class PlayerMaidDoubleHeartChainBauble implements PlayerMaidBauble {

    /** 佩戴者承受的伤害比例（镜像 Config.doubleHeartChainShareRatio 默认值 0.5）。 */
    public static final float SELF_DAMAGE_RATIO = 0.5F;

    /** 主人分担伤害的最大距离（镜像 Config.doubleHeartChainMaxDistance 默认值 32.0）。 */
    public static final double MAX_SHARE_DISTANCE = 32.0D;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：伤害分担由玩家侧事件等价物实现
    }
}
