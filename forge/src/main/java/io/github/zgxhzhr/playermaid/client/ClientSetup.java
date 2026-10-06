package io.github.zgxhzhr.playermaid.client;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.client.screen.FoxMaidBaubleScreen;
import io.github.zgxhzhr.playermaid.client.screen.FoxMaidMainScreen;
import io.github.zgxhzhr.playermaid.menu.FoxMaidMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端专属注册：容器与屏幕绑定。
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                    FoxMaidMenus.MAIN.get(), FoxMaidMainScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    FoxMaidMenus.BAUBLE.get(), FoxMaidBaubleScreen::new);
        });
    }

    /** 注册梦云水晶玩家光环的模型层（供光环渲染器烘焙）。 */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                io.github.zgxhzhr.playermaid.client.model.PlayerHaloModel.LAYER_LOCATION,
                io.github.zgxhzhr.playermaid.client.model.PlayerHaloModel::createBodyLayer);
    }
}
