package io.github.zgxhzhr.playermaid.mixin;

import com.github.tartaricacid.touhoulittlemaid.block.BlockMaidBed;
import io.github.zgxhzhr.playermaid.data.FoxMaidStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * 让「人是狐」玩家能睡 TLM 女仆床。
 *
 * <p>TLM 的 {@code BlockMaidBed.isBed} 只对 {@code EntityMaid} 放行，这里在 RETURN 处
 * 对人是狐玩家额外返回 true；原版 {@code Player.startSleepInBed} 会先调 {@code isBed}，
 * 放行后即可进入正常睡眠流程（起床沿用原版机制）。</p>
 *
 * <p>TLM 为外部模组：release jar 中 {@code isBed} 方法名未混淆（已用 javap 验证），
 * 故按 {@code remap = false} 直接以运行时方法名注入，不参与 minecraft 混淆映射。</p>
 */
@Mixin(value = BlockMaidBed.class, remap = false)
public abstract class BlockMaidBedMixin {

    @Inject(method = "isBed", at = @At("RETURN"), cancellable = true)
    private void playermaid$allowFoxMaidSleep(BlockState state, BlockGetter world, BlockPos pos,
                                              @Nullable Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player && FoxMaidStatus.isActive(player)) {
            cir.setReturnValue(true);
        }
    }
}
