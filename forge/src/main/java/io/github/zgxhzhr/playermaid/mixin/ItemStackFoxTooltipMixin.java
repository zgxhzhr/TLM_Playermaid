package io.github.zgxhzhr.playermaid.mixin;

import io.github.zgxhzhr.playermaid.compat.slab.FoxSlabData;
import io.github.zgxhzhr.playermaid.tooltip.FoxSlabTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 装有「人是狐」玩家的魂符悬停预览：在 {@code ItemStack#getTooltipImage} 处插入
 * 本模组的预览数据（名字 + 被收容玩家 UUID + 魂符展示模型 id）。
 *
 * <p>该方法为原版方法（1.20.1），生产环境会被重映射为混淆名，因此注入必须走默认
 * remap=true，由注解处理器生成正确的映射条目；此处不依赖车万女仆的方法名
 * （其生产 jar 会混淆方法名），任何物品的悬停预览都必经此入口。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackFoxTooltipMixin {

    @Inject(
            method = "getTooltipImage()Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void playermaid$foxSlabTooltip(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (FoxSlabData.isFoxSlab(stack)) {
            cir.setReturnValue(Optional.of(new FoxSlabTooltip(
                    FoxSlabData.getPlayerName(stack),
                    FoxSlabData.getPlayerUuid(stack),
                    FoxSlabData.getSlabModelId(stack))));
        }
    }
}
