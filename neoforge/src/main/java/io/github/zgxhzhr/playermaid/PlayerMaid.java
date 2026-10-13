package io.github.zgxhzhr.playermaid;

import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import io.github.zgxhzhr.playermaid.event.FoxMaidInteractHandler;
import io.github.zgxhzhr.playermaid.menu.FoxMaidMenus;
import io.github.zgxhzhr.playermaid.network.NetworkInit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 「人是狐」主类（NeoForge 1.21.1）。
 *
 * <p>勾选后玩家将获得车万女仆风格的界面与展示：自己按背包键打开女仆主界面，
 * 其他玩家非潜行右键可查看并操作其装备、背包与女仆饰品。</p>
 */
@Mod(Constants.MOD_ID)
public class PlayerMaid {

    public PlayerMaid(IEventBus modEventBus) {
        // 容器类型注册（mod 事件总线）
        FoxMaidMenus.MENUS.register(modEventBus);

        // 自定义数据组件（梦云水晶无敌倒计时等）
        io.github.zgxhzhr.playermaid.data.FoxMaidDataComponents.COMPONENTS.register(modEventBus);
        // 网络载荷注册（1.21 走 RegisterPayloadHandlersEvent）
        modEventBus.addListener(NetworkInit::registerPayloads);

        // 玩家数据事件（登录/复活/登出/克隆/追踪同步）
        NeoForge.EVENT_BUS.register(FoxMaidManager.class);
        // 非潜行右键玩家打开界面
        NeoForge.EVENT_BUS.register(FoxMaidInteractHandler.class);
    }
}
