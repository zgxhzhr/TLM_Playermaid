package io.github.zgxhzhr.playermaid.data;

import com.mojang.serialization.Codec;
import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 「人是狐」自定义物品数据组件。
 *
 * <p>MC 1.21 已移除 {@code ItemStack.getTag()} / {@code getOrCreateTag()} / {@code setTag(...)}，
 * 原先直接挂在饰品 ItemStack 上的自定义数据一律改为 DataComponentType。</p>
 *
 * <p><b>注意</b>：{@link #COMPONENTS} 必须由主类在 mod 事件总线上注册
 * （与 {@code FoxMaidMenus.MENUS} 同样写法：{@code FoxMaidDataComponents.COMPONENTS.register(modEventBus);}），
 * 否则读取组件时会因未注册而抛异常。</p>
 */
public final class FoxMaidDataComponents {

    /** 数据组件注册器（以 {@link Registries#DATA_COMPONENT_TYPE} 为准，避免使用已弃用的单参重载）。 */
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);

    /**
     * 梦云水晶「无敌剩余 tick」计数。
     *
     * <p>对应 1.20.1 版存在饰品 ItemStack 上的 NBT 键 {@code dream_crystal_invulnerable_time}，
     * 语义不变：复活后写 300，每 tick 自减 1，减到 0 时移除。</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            DREAM_CRYSTAL_INVULNERABLE_TICKS = COMPONENTS.registerComponentType(
            "dream_crystal_invulnerable_ticks",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private FoxMaidDataComponents() {
    }
}
