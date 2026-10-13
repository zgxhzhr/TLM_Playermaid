package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 火焰保护饰品（fire_protect_bauble）——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code FireProtectBauble}：火焰伤害命中时消耗 1 点耐久完全抵挡，并获得
 * 15 秒抗火（原版还生成灭火实体 EntityExtinguishingAgent，玩家侧降级跳过）。</p>
 */
public class PlayerMaidFireProtectBauble extends PlayerMaidProtectionBauble {

    @Override
    protected TagKey<DamageType> damageTag() {
        return DamageTypeTags.IS_FIRE;
    }

    @Override
    protected void onBlocked(LivingEntity entity, ItemStack baubleItem) {
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300));
    }
}
