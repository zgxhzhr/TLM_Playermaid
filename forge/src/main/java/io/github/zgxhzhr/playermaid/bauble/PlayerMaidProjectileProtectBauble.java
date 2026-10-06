package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * 弹射物保护饰品（projectile_protect_bauble）——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code ProjectileProtectBauble}：弹射物伤害命中时消耗 1 点耐久完全抵挡
 * （原版另有爆炸粒子与女仆事件进度，玩家侧降级跳过）。</p>
 */
public class PlayerMaidProjectileProtectBauble extends PlayerMaidProtectionBauble {

    @Override
    protected TagKey<DamageType> damageTag() {
        return DamageTypeTags.IS_PROJECTILE;
    }
}
