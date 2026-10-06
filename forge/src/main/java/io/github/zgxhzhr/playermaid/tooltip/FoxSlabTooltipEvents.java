package io.github.zgxhzhr.playermaid.tooltip;

import io.github.zgxhzhr.playermaid.Constants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 注册 {@link FoxSlabTooltip} 的客户端渲染组件工厂（走 Forge MOD 总线）。
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FoxSlabTooltipEvents {

    private FoxSlabTooltipEvents() {
    }

    @SubscribeEvent
    public static void onRegisterTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FoxSlabTooltip.class, FoxSlabTooltipComponent::new);
    }
}
