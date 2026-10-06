package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * 熔岩狐叶（molten_fox_leaf）饰品逻辑——人是狐玩家版（降级）。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code MoltenFoxLeafBauble}：佩戴者在岩浆表面上稳定行走（托举防沉）、持续清除燃烧状态，
 * 参数（下沉上限 -0.05、上浮 0.08、表面偏移 1.0）与原版一致。</p>
 *
 * <p><b>降级说明</b>：熔岩轨迹方块（MoltenFoxLeafTrail，万法皆通私有方块注册表）未移植；
 * 原版"让附近主人获得岩浆行走"对本佩戴者即自身，无需额外处理。因此本件不覆盖车万女仆绑定，
 * 女仆佩戴仍走万法皆通原版逻辑（含轨迹效果）。</p>
 */
public class PlayerMaidMoltenFoxLeafBauble implements PlayerMaidBauble {

    private static final double MAX_SINK_SPEED = -0.05D;
    private static final double SURFACE_FLOAT_SPEED = 0.08D;
    private static final double SURFACE_Y_OFFSET = 1.0D;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide() || entity.isPassenger() || entity.isUnderWater()) {
            return;
        }

        if (entity.isOnFire()) {
            entity.clearFire();
        }

        BlockPos blockPos = entity.blockPosition();
        FluidState feetFluid = entity.level().getFluidState(blockPos);
        FluidState belowFluid = entity.level().getFluidState(blockPos.below());
        if (!feetFluid.is(FluidTags.LAVA) && !belowFluid.is(FluidTags.LAVA)) {
            return;
        }

        entity.fallDistance = 0.0F;
        Vec3 deltaMovement = entity.getDeltaMovement();
        PlayerFoxLeafSurfaceWalk.keepAtSurface(
                entity, blockPos, feetFluid, belowFluid, deltaMovement, FluidTags.LAVA,
                MAX_SINK_SPEED, SURFACE_FLOAT_SPEED, SURFACE_Y_OFFSET);
    }
}
