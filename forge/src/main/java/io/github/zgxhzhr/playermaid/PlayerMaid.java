package io.github.zgxhzhr.playermaid;

import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import io.github.zgxhzhr.playermaid.event.FoxMaidInteractHandler;
import io.github.zgxhzhr.playermaid.menu.FoxMaidMenus;
import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 「人是狐」主类（Forge 1.20.1）。
 *
 * <p>勾选后玩家将获得车万女仆风格的界面与展示：自己按 E 打开女仆主界面，
 * 其他玩家非潜行右键可查看并操作其装备、背包与女仆饰品。</p>
 */
@Mod(Constants.MOD_ID)
public class PlayerMaid {

    public PlayerMaid() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 容器类型注册
        FoxMaidMenus.MENUS.register(modEventBus);

        // 网络通道
        modEventBus.addListener(io.github.zgxhzhr.playermaid.network.NetworkInit::commonSetup);

        // 玩家数据事件（登录/复活/登出/克隆/追踪同步）
        MinecraftForge.EVENT_BUS.register(FoxMaidManager.class);
        // 非潜行右键玩家打开界面
        MinecraftForge.EVENT_BUS.register(FoxMaidInteractHandler.class);
    }
}
