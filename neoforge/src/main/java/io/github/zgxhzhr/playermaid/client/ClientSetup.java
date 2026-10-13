package io.github.zgxhzhr.playermaid.client;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.client.screen.FoxMaidBaubleScreen;
import io.github.zgxhzhr.playermaid.client.screen.FoxMaidMainScreen;
import io.github.zgxhzhr.playermaid.menu.FoxMaidMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端专属注册：容器与屏幕绑定。
 *
 * <p>1.21 起 {@code MenuScreens.register} 已私有化，改为监听
 * {@link RegisterMenuScreensEvent}；该事件挂在 mod 事件总线。</p>
 */
@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    /** 容器 → 屏幕绑定。 */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(FoxMaidMenus.MAIN.get(), FoxMaidMainScreen::new);
        event.register(FoxMaidMenus.BAUBLE.get(), FoxMaidBaubleScreen::new);
    }

    /** 注册梦云水晶玩家光环的模型层（供光环渲染器烘焙）。 */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(
            net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                io.github.zgxhzhr.playermaid.client.model.PlayerHaloModel.LAYER_LOCATION,
                io.github.zgxhzhr.playermaid.client.model.PlayerHaloModel::createBodyLayer);
    }
}
