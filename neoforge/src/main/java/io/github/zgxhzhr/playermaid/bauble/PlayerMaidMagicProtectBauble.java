package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 魔法保护饰品（magic_protect_bauble）——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code MagicProtectBauble}：女巫系魔法伤害（{@code WITCH_RESISTANT_TO} 标签）
 * 命中时消耗 1 点耐久完全抵挡并清除全部状态效果
 * （原版另有爆炸粒子与女仆事件进度，玩家侧降级跳过）。</p>
 */
public class PlayerMaidMagicProtectBauble extends PlayerMaidProtectionBauble {

    @Override
    protected TagKey<DamageType> damageTag() {
        return DamageTypeTags.WITCH_RESISTANT_TO;
    }

    @Override
    protected void onBlocked(LivingEntity entity, ItemStack baubleItem) {
        entity.removeAllEffects();
    }
}
