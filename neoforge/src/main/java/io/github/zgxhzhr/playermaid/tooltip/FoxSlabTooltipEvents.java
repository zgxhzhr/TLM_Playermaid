package io.github.zgxhzhr.playermaid.tooltip;

import net.neoforged.fml.common.EventBusSubscriber;

import io.github.zgxhzhr.playermaid.Constants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * 注册 {@link FoxSlabTooltip} 的客户端渲染组件工厂（走 Forge MOD 总线）。
 */
@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FoxSlabTooltipEvents {

    private FoxSlabTooltipEvents() {
    }

    @SubscribeEvent
    public static void onRegisterTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FoxSlabTooltip.class, FoxSlabTooltipComponent::new);
    }
}
