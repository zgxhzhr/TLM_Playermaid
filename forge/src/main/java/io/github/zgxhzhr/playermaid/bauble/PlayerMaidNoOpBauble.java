package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 无效果占位实现（降级件共用）。
 *
 * <p>用于无法在人是狐玩家侧生效的饰品（依赖万法皆通施法/容器/渲染管线的件）。
 * 仅登记在「仅玩家集」中让饰品栏识别这些物品并保留统一的驱动/佩戴差异框架；
 * 具体降级原因在 {@link PlayerMaidBaubleRegistry} 的注册处逐条注释说明。
 * 不覆盖车万女仆绑定，女仆佩戴仍走万法皆通原版逻辑。</p>
 */
public final class PlayerMaidNoOpBauble implements PlayerMaidBauble {

    public static final PlayerMaidNoOpBauble INSTANCE = new PlayerMaidNoOpBauble();

    private PlayerMaidNoOpBauble() {
    }

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        // 无效果
    }
}
