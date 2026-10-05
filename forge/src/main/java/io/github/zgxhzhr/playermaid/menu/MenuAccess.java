package io.github.zgxhzhr.playermaid.menu;

import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;

/**
 * 人是狐界面打开工具：服务端校验后向玩家打开主界面或饰品界面。
 */
public final class MenuAccess {

    /** 打开者与目标的最大交互距离（格）。 */
    private static final double INTERACT_DISTANCE_SQR = 8.0 * 8.0;

    private MenuAccess() {
    }

    /**
     * 请求打开目标玩家的人是狐界面。
     *
     * @param visitor 打开界面的玩家
     * @param target  被查看的玩家
     * @param bauble  true=饰品界面，false=主界面
     * @return 是否成功打开
     */
    public static boolean open(ServerPlayer visitor, ServerPlayer target, boolean bauble) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        FoxMaidData data = FoxMaidManager.peek(target).orElse(null);
        if (data == null || !data.isActive()) {
            return false;
        }
        // 自己打开自己不做距离/维度限制；他人需要同维度且在距离内
        if (visitor != target) {
            if (visitor.level().dimension() != target.level().dimension()) {
                return false;
            }
            if (visitor.distanceToSqr(target) > INTERACT_DISTANCE_SQR) {
                return false;
            }
        }
        NetworkHooks.openScreen(visitor, new FoxMaidMenuProvider(target, bauble),
                buf -> buf.writeInt(target.getId()));
        return true;
    }

    /**
     * 人是狐界面 MenuProvider：服务端创建菜单实例，客户端经 MenuType 反序列化。
     */
    private record FoxMaidMenuProvider(ServerPlayer target, boolean bauble) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return bauble
                    ? Component.translatable("gui.playermaid.bauble.title")
                    : Component.translatable("gui.playermaid.main.title");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return bauble
                    ? new FoxMaidBaubleMenu(id, inventory, target)
                    : new FoxMaidMainMenu(id, inventory, target);
        }
    }
}
