package io.github.zgxhzhr.playermaid.menu;

import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 人是狐容器类型注册。
 */
public final class FoxMaidMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Constants.MOD_ID);

    /** 女仆主界面（含装备槽、女仆背包）。 */
    public static final RegistryObject<MenuType<FoxMaidMainMenu>> MAIN =
            MENUS.register("foxmaid_main", () -> IForgeMenuType.create(FoxMaidMainMenu::new));

    /** 女仆饰品栏（30 格，仅女仆饰品）。 */
    public static final RegistryObject<MenuType<FoxMaidBaubleMenu>> BAUBLE =
            MENUS.register("foxmaid_bauble", () -> IForgeMenuType.create(FoxMaidBaubleMenu::new));

    private FoxMaidMenus() {
    }
}
