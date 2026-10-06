package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableFloat;

/**
 * 车万女仆保护类饰品（溺水/爆炸/摔落/火焰/魔法/弹射物保护）移植基类。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code DrownProtectBauble} 等保护类：被对应伤害类型命中时消耗 1 点物品耐久
 * （原版物品为可损耗饰品）并完全抵挡该次伤害；女仆侧经 {@code onInjured} 由车万女仆
 * 调用，人是狐玩家侧经 {@link PlayerMaidBaubleEvents#onLivingAttack} 等价调用。</p>
 */
public abstract class PlayerMaidProtectionBauble implements PlayerMaidBauble {

    /** 该饰品抵挡的伤害类型标签。 */
    protected abstract TagKey<DamageType> damageTag();

    /** 抵挡成功后的附加行为（默认无）。 */
    protected void onBlocked(LivingEntity entity, ItemStack baubleItem) {
    }

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：伤害抵挡由 onInjured / 玩家侧事件实现
    }

    /** 女仆侧：车万女仆受伤回调，命中类型则扣耐久并抵挡。 */
    @Override
    public boolean onInjured(EntityMaid maid, ItemStack baubleItem, DamageSource source, MutableFloat damage) {
        if (source.is(damageTag())) {
            baubleItem.hurtAndBreak(1, maid, m -> {
            });
            onBlocked(maid, baubleItem);
            return true;
        }
        return false;
    }

    /** 玩家侧：命中类型则扣耐久并抵挡（由 {@link PlayerMaidBaubleEvents} 调用）。 */
    public boolean tryBlockPlayer(LivingEntity entity, ItemStack stack, DamageSource source) {
        if (!source.is(damageTag())) {
            return false;
        }
        stack.hurtAndBreak(1, entity, m -> {
        });
        onBlocked(entity, stack);
        return true;
    }
}
