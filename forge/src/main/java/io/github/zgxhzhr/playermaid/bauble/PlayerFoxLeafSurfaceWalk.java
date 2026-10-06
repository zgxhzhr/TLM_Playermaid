package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;

/**
 * 狐狸叶类饰品的流体表面行走物理（移植自万法皆通 FoxLeafSurfaceHelper 的核心）。
 *
 * <p>本文件代码移植自万法皆通（Touhou-Little-Maid-Spell，作者 yimeng261，MIT 协议开源）
 * 的 {@code FoxLeafSurfaceHelper#keepEntityAtFluidSurface}：把实体托举在流体表面并阻止下沉。
 * 方块轨迹部分（tryPlaceTrail，依赖万法皆通私有方块注册表）未移植，行走效果不受影响。</p>
 */
public final class PlayerFoxLeafSurfaceWalk {

    private PlayerFoxLeafSurfaceWalk() {
    }

    /**
     * 把实体稳定保持在指定流体（水/岩浆）表面：贴近表面时吸附到表面高度，
     * 距表面较远时保持不下沉并缓慢上浮；参数与原版逐项一致。
     */
    public static void keepAtSurface(LivingEntity entity, BlockPos blockPos, FluidState feetFluid,
                                     FluidState belowFluid, Vec3 deltaMovement, TagKey<Fluid> fluidTag,
                                     double maxSinkSpeed, double surfaceFloatSpeed, double surfaceYOffset) {
        Level level = entity.level();
        BlockPos fluidPos = resolveSurfaceFluidPos(blockPos, feetFluid, belowFluid, fluidTag);
        if (fluidPos == null) {
            if (deltaMovement.y < maxSinkSpeed) {
                entity.setDeltaMovement(deltaMovement.x, maxSinkSpeed, deltaMovement.z);
            }
            return;
        }

        // 向上搜索真正的流体表面（流体上方为空气的位置）
        BlockPos trueSurface = fluidPos;
        while (level.getFluidState(trueSurface.above()).is(fluidTag)) {
            trueSurface = trueSurface.above();
        }

        CollisionContext collisionContext = CollisionContext.of(entity);
        if (collisionContext.isAbove(LiquidBlock.STABLE_SHAPE, trueSurface, true)) {
            entity.setOnGround(true);
        }

        double surfaceY = trueSurface.getY() + surfaceYOffset;
        if (Math.abs(entity.getY() - surfaceY) > 0.02D) {
            entity.setPos(entity.getX(), surfaceY, entity.getZ());
            entity.setOnGround(true);
            entity.setDeltaMovement(deltaMovement.x, Math.max(deltaMovement.y, 0.0D), deltaMovement.z);
            return;
        }

        if (deltaMovement.y < 0.0D) {
            entity.setDeltaMovement(deltaMovement.x, Math.max(deltaMovement.y, surfaceFloatSpeed), deltaMovement.z);
        }
    }

    @Nullable
    private static BlockPos resolveSurfaceFluidPos(BlockPos blockPos, FluidState feetFluid, FluidState belowFluid,
                                                   TagKey<Fluid> fluidTag) {
        if (feetFluid.is(fluidTag)) {
            return blockPos;
        }
        if (belowFluid.is(fluidTag)) {
            return blockPos.below();
        }
        return null;
    }
}
