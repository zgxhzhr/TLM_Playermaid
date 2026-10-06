package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 馥郁巧思（fragrant_ingenuity）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code FragrantIngenuityBauble}：进食获得额外好感度（每次 +2，镜像
 * Config.fragrantIngenuityFavorabilityGain 默认值；女仆经车万女仆
 * {@code MaidAfterEatEvent}、人是狐玩家经进食完成事件，见
 * {@link PlayerMaidBaubleEvents#onMaidAfterEat} / {@link PlayerMaidBaubleEvents#onUseItemFinish}）。</p>
 *
 * <p>未移植项（注释说明）：原版"女仆喂食主人赋予随机 1 级正面 Buff"
 * 经万法皆通 MaidFeedOwnerTaskMixin 实现，依赖其喂食任务管线且主人需为实体；
 * 人是狐玩家的主人仅为显示名（无实体引用），无法映射，降级跳过。</p>
 */
public class PlayerMaidFragrantIngenuityBauble implements PlayerMaidBauble {

    /** 每次进食好感度增量（镜像 Config.fragrantIngenuityFavorabilityGain 默认值 2）。 */
    public static final int FAVORABILITY_GAIN = 2;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：进食好感度由事件等价物实现（女仆 MaidAfterEatEvent / 玩家进食完成事件）
    }

    /** 女仆侧：直接增加好感度（原版逻辑）。 */
    public static void addMaidFavorability(EntityMaid maid) {
        maid.getFavorabilityManager().add(FAVORABILITY_GAIN);
    }
}
