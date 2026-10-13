package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 溺水保护饰品（drown_protect_bauble）——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code DrownProtectBauble}：溺水伤害命中时消耗 1 点耐久完全抵挡，并把空气值补满
 * （原版另有气泡粒子与女仆事件进度，玩家侧降级跳过）。</p>
 */
public class PlayerMaidDrownProtectBauble extends PlayerMaidProtectionBauble {

    @Override
    protected TagKey<DamageType> damageTag() {
        return DamageTypeTags.IS_DROWNING;
    }

    @Override
    protected void onBlocked(LivingEntity entity, ItemStack baubleItem) {
        entity.setAirSupply(entity.getMaxAirSupply());
    }
}
