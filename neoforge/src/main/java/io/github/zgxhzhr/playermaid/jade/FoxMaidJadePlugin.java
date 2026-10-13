package io.github.zgxhzhr.playermaid.jade;

import net.minecraft.world.entity.player.Player;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * 人是狐 Jade 插件入口：由 Jade 通过 {@link WailaPlugin} 注解自动发现。
 * Jade 缺席时本类不会被加载，其余功能不受影响。
 */
@WailaPlugin
public class FoxMaidJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new FoxMaidJadeProvider(), Player.class);
    }
}
