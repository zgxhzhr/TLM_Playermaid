package io.github.zgxhzhr.playermaid.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 人是狐主界面容器（仿车万女仆 {@code MaidMainContainer} + 大背包布局）。
 *
 * <p>槽位顺序：0-35 访问者背包；36-39 目标护甲；40-41 目标主副手；
 * 42-77 顶部网格。顶部网格直接映射目标玩家的真实背包，与底部玩家背包是同一份数据：
 * 上排对应主背包（27 格），下排对应快捷栏（9 格）。因此移动顶部格子里的物品
 * 即等于移动真实背包里的物品，其他人打开该界面也能正常存取人是狐玩家的物品。</p>
 */
public class FoxMaidMainMenu extends FoxMaidBaseMenu {

    /** 默认背包格数量（顶部一排 6 格）。 */
    private static final int DEFAULT_BACKPACK_COUNT = 6;
    /** 扩展区域排数。 */
    private static final int EXTENSION_ROWS = 5;
    /** 每排格数。 */
    private static final int COLUMNS = 6;
    /** 玩家主背包槽数（不含快捷栏），用于把顶部网格上半映射到主背包。 */
    private static final int MAIN_INVENTORY_SIZE = 27;

    private static final int DEFAULT_BACKPACK_Y = 37;
    private static final int BACKPACK_START_X = 143;
    /** 与车万女仆大背包完全一致的各排 y 坐标。 */
    private static final int[] EXTENSION_ROW_Y = {59, 82, 100, 123, 141};

    /** 客户端构造（MenuType 反序列化，读目标实体 id）。 */
    public FoxMaidMainMenu(int id, Inventory visitorInventory, FriendlyByteBuf buf) {
        this(id, visitorInventory, resolveTarget(visitorInventory, buf.readInt()));
    }

    /** 服务端构造。 */
    public FoxMaidMainMenu(int id, Inventory visitorInventory, Player target) {
        super(FoxMaidMenus.MAIN.get(), id, visitorInventory, target);

        // 默认背包 6 格（y=37，x=143 起）
        for (int i = 0; i < DEFAULT_BACKPACK_COUNT; i++) {
            addSlot(createInventorySlot(i, BACKPACK_START_X + 18 * i, DEFAULT_BACKPACK_Y));
        }
        // 大背包扩展 30 格：5 排 × 6 列
        for (int row = 0; row < EXTENSION_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int gridIndex = DEFAULT_BACKPACK_COUNT + row * COLUMNS + col;
                addSlot(createInventorySlot(gridIndex, BACKPACK_START_X + 18 * col, EXTENSION_ROW_Y[row]));
            }
        }
    }

    /**
     * 把顶部网格第 {@code gridIndex} 格映射到目标玩家的真实背包槽位：
     * 上半（前 27 格）为主背包，下半（后 9 格）为快捷栏。
     */
    private Slot createInventorySlot(int gridIndex, int x, int y) {
        int inventoryIndex = gridIndex < MAIN_INVENTORY_SIZE
                ? gridIndex + 9
                : gridIndex - MAIN_INVENTORY_SIZE;
        return new Slot(target.getInventory(), inventoryIndex, x, y);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveBetween(player, index, VISITOR_INVENTORY_SIZE, this.slots.size(), false);
    }
}
