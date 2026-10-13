package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 不死图腾（原版 {@code totem_of_undying}）饰品——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code UndyingTotemBauble}：饰品栏佩戴原版不死图腾时，玩家死亡（非无视无敌类伤害）
 * 触发不死效果——消耗 1 个图腾、恢复 1 点生命、清除状态并给予
 * 再生 II（45 秒）/伤害吸收 II（5 秒）/抗火（40 秒），播放图腾激活实体事件。
 * 女仆侧经 {@code onDeath} 由车万女仆调用，玩家侧见 {@link PlayerMaidBaubleEvents#onLivingDeath}。
 * 注意：仅饰品栏佩戴生效；玩家主手/副手原版图腾的不死逻辑由原版先于本事件处理。</p>
 */
public class PlayerMaidUndyingTotemBauble implements PlayerMaidBauble {

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无每 tick 效果：死亡保护由 onDeath / 玩家侧事件实现
    }

    /** 女仆侧：死亡回调。 */
    @Override
    public boolean onDeath(EntityMaid maid, ItemStack baubleItem, DamageSource source) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            baubleItem.shrink(1);
            applyTotemEffects(maid);
            return true;
        }
        return false;
    }

    /** 玩家侧：死亡时消耗图腾并施加不死效果（由 {@link PlayerMaidBaubleEvents} 调用）。 */
    public boolean tryTotemPlayer(LivingEntity player, ItemStack stack, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        stack.shrink(1);
        applyTotemEffects(player);
        return true;
    }

    private static void applyTotemEffects(LivingEntity entity) {
        entity.setHealth(1.0F);
        entity.removeAllEffects();
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        entity.level().broadcastEntityEvent(entity, EntityEvent.TALISMAN_ACTIVATE);
    }
}
