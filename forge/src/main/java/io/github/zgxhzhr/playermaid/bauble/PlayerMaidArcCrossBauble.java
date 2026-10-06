package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * 弧光十字（arc_cross）饰品逻辑——人是狐玩家版。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code ArcCrossBauble}：每 40 tick 为佩戴者刷新铁魔法（Irons Spellbooks）的
 * 雷暴与神圣守护效果（持续 100 tick、等级 10，参数与原版一致）。
 * 原实现直接引用铁魔法注册表类；本移植改为按物品注册 id
 * （{@code irons_spellbooks:thunderstorm}、{@code irons_spellbooks:fortify}，
 * 已用 javap 核对生产 jar 注册名）通过注册表查找，铁魔法未安装时静默跳过。</p>
 */
public class PlayerMaidArcCrossBauble implements PlayerMaidBauble {

    private static final int REFRESH_INTERVAL = 40;
    private static final int EFFECT_DURATION = 100;
    private static final int EFFECT_AMPLIFIER = 9;

    private static final ResourceLocation EFFECT_THUNDERSTORM =
            new ResourceLocation("irons_spellbooks", "thunderstorm");
    private static final ResourceLocation EFFECT_FORTIFY =
            new ResourceLocation("irons_spellbooks", "fortify");

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide() || !entity.isAlive() || tick % REFRESH_INTERVAL != 0) {
            return;
        }
        if (!ModList.get().isLoaded("irons_spellbooks")) {
            return;
        }
        refreshEffect(entity, EFFECT_THUNDERSTORM);
        refreshEffect(entity, EFFECT_FORTIFY);
    }

    private static void refreshEffect(LivingEntity entity, ResourceLocation effectId) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect == null) {
            return;
        }
        entity.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, EFFECT_AMPLIFIER, false, true, true));
    }
}
