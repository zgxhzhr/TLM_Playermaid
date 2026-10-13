package io.github.zgxhzhr.playermaid.menu;

import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 人是狐容器类型注册。
 *
 * <p>NeoForge 1.21 已移除 {@code RegistryObject}，改用 {@link DeferredHolder}。</p>
 */
public final class FoxMaidMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    /** 女仆主界面（含装备槽、女仆背包）。 */
    public static final DeferredHolder<MenuType<?>, MenuType<FoxMaidMainMenu>> MAIN =
            MENUS.register("foxmaid_main", () -> IMenuTypeExtension.create(FoxMaidMainMenu::new));

    /** 女仆饰品栏（30 格，仅女仆饰品）。 */
    public static final DeferredHolder<MenuType<?>, MenuType<FoxMaidBaubleMenu>> BAUBLE =
            MENUS.register("foxmaid_bauble", () -> IMenuTypeExtension.create(FoxMaidBaubleMenu::new));

    private FoxMaidMenus() {
    }
}
