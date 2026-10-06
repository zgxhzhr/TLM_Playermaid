package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * 爆炸保护饰品（explosion_protect_bauble）——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code ExplosionProtectBauble}：爆炸伤害命中时消耗 1 点耐久完全抵挡
 * （原版另有女仆事件进度，玩家侧降级跳过）。</p>
 */
public class PlayerMaidExplosionProtectBauble extends PlayerMaidProtectionBauble {

    @Override
    protected TagKey<DamageType> damageTag() {
        return DamageTypeTags.IS_EXPLOSION;
    }
}
