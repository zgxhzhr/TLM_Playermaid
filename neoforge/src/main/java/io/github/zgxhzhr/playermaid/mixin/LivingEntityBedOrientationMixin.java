package io.github.zgxhzhr.playermaid.mixin;

import com.github.tartaricacid.touhoulittlemaid.block.BlockMaidBed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修正「人是狐」玩家睡在 TLM 女仆床上时模型相对床水平偏移的问题。
 *
 * <p>睡姿渲染由 {@code LivingEntityRenderer#setupRotations} 的
 * {@code Pose.SLEEPING} 分支完成，它先取 {@code LivingEntity#getBedOrientation()}
 * 决定躺姿朝向，再据床头朝向摆放模型。Forge 版的 {@code getBedOrientation} 实现为
 * 「{@code isBed} 放行则取 {@code getBedDirection}」，而 TLM 女仆床是
 * {@code BlockMaidBed}（继承 {@code HorizontalDirectionalBlock}，只覆写了
 * {@code isBed} 而<b>没有</b>覆写 {@code getBedDirection}），于是朝向一律取该默认方法的
 * NORTH，与床自身 {@code FACING} 无关。床不是南北朝向时躺姿便与床错位，表现为
 * 「模型平行于地面、相对床偏移」。</p>
 *
 * <p>此处直接按女仆床自身的 {@code FACING} 覆盖返回值。女仆床的 {@code FACING}
 * 与原版床同为 {@code HorizontalDirectionalBlock.FACING}（{@code BedBlock.FACING}
 * 即该属性），语义一致，故与原版床渲染完全对齐。</p>
 *
 * <p>仅对 Player 生效：能睡上女仆床的必是本工程放行睡眠的「人是狐」玩家，
 * 原版床与其它实体的行为不受影响。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBedOrientationMixin {

    @Inject(method = "getBedOrientation", at = @At("RETURN"), cancellable = true)
    private void playermaid$maidBedOrientation(CallbackInfoReturnable<Direction> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player)) {
            return;
        }
        BlockPos sleepingPos = self.getSleepingPos().orElse(null);
        if (sleepingPos == null) {
            return;
        }
        BlockState state = self.level().getBlockState(sleepingPos);
        if (state.getBlock() instanceof BlockMaidBed) {
            cir.setReturnValue(state.getValue(HorizontalDirectionalBlock.FACING));
        }
    }
}
