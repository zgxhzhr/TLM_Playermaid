package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 群青灵珠（ultramarine_orb_elixir，额外生命）饰品——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code ExtraLifeBauble}：佩戴者死亡（非无视无敌类伤害）时消耗 1 点耐久
 * 并恢复满生命（原版另有爱心粒子与玻璃碎裂音效、女仆事件进度，玩家侧降级跳过）。
 * 女仆侧经 {@code onDeath} 由车万女仆调用，玩家侧见 {@link PlayerMaidBaubleEvents#onLivingDeath}。</p>
 */
public class PlayerMaidExtraLifeBauble implements PlayerMaidBauble {

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：死亡复活由 onDeath / 玩家侧事件实现
    }

    /** 女仆侧：死亡回调。 */
    @Override
    public boolean onDeath(EntityMaid maid, ItemStack baubleItem, DamageSource source) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            baubleItem.hurtAndBreak(1, maid, m -> {
            });
            maid.setHealth(maid.getMaxHealth());
            return true;
        }
        return false;
    }

    /** 玩家侧：死亡时消耗耐久并回满生命（由 {@link PlayerMaidBaubleEvents} 调用）。 */
    public boolean tryRevivePlayer(LivingEntity player, ItemStack stack, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        stack.hurtAndBreak(1, player, m -> {
        });
        player.setHealth(player.getMaxHealth());
        return true;
    }
}
