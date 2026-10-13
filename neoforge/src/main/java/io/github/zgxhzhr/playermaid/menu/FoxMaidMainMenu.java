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
 * 网格第 {@code i} 格就是玩家背包第 {@code i} 格（0-8 为快捷栏、9-35 为主背包），
 * 也就是从 1 数到 36 的同一套顺序，只是每排 6 格、玩家背包每排 9 格。
 * 因此移动顶部格子里的物品即等于移动真实背包里的物品，其他人打开该界面也能正常存取。</p>
 */
public class FoxMaidMainMenu extends FoxMaidBaseMenu {

    /** 默认背包格数量（顶部一排 6 格）。 */
    private static final int DEFAULT_BACKPACK_COUNT = 6;
    /** 扩展区域排数。 */
    private static final int EXTENSION_ROWS = 5;
    /** 每排格数。 */
    private static final int COLUMNS = 6;

    private static final int DEFAULT_BACKPACK_Y = 37;
    private static final int BACKPACK_START_X = 143;
    /** 与车万女仆大背包完全一致的各排 y 坐标。 */
    private static final int[] EXTENSION_ROW_Y = {59, 82, 100, 123, 141};

    /** 装备区（4 护甲 + 主副手）起始槽索引，紧接访问者背包之后。 */
    private static final int EQUIPMENT_START = VISITOR_INVENTORY_SIZE;
    /** 顶部网格起始槽索引，紧接装备区之后。 */
    private static final int GRID_START = EQUIPMENT_START + 6;

    /** 打开界面瞬间的快捷栏选中下标，也就是主手槽指向的背包下标。 */
    private final int mainHandIndex;

    /** 客户端构造（MenuType 反序列化，读目标实体 id）。 */
    public FoxMaidMainMenu(int id, Inventory visitorInventory, FriendlyByteBuf buf) {
        this(id, visitorInventory, resolveTarget(visitorInventory, buf.readInt()));
    }

    /** 服务端构造。 */
    public FoxMaidMainMenu(int id, Inventory visitorInventory, Player target) {
        super(FoxMaidMenus.MAIN.get(), id, visitorInventory, target);
        this.mainHandIndex = target.getInventory().selected;

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
     * 把顶部网格第 {@code gridIndex} 格映射到目标玩家背包的同下标槽位，两边顺序完全一致。
     *
     * <p>与主手槽指向同一下标的那一格会被主手槽重复展示，因此锁为只读样式；
     * 主手为空时两处都是空的，不存在重复，该格照常可用。</p>
     */
    private Slot createInventorySlot(int gridIndex, int x, int y) {
        Inventory inventory = target.getInventory();
        if (gridIndex != mainHandIndex) {
            return new Slot(inventory, gridIndex, x, y);
        }
        return new LockedSlot(inventory, gridIndex, x, y,
                () -> !inventory.getItem(mainHandIndex).isEmpty());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || isLocked(slot) || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        boolean moved;
        if (index < VISITOR_INVENTORY_SIZE) {
            // 底部访问者背包 → 装备区与顶部网格
            moved = moveItemStack(original, EQUIPMENT_START, slots.size(), false, slot);
        } else if (index < GRID_START) {
            // 装备区 → 玩家背包；自己看自己时底部是只读视图，改投顶部网格
            moved = dropIntoBackpackView(original, slot);
        } else {
            // 顶部网格即玩家背包本身：自己看自己时无处可移，否则投给访问者背包
            moved = !visitorLocked && moveItemStack(original, 0, VISITOR_INVENTORY_SIZE, true, slot);
        }
        return finishQuickMove(player, slot, original, copy, moved);
    }

    /** 把物品放入玩家背包的可写视图：未锁定时投底部访问者背包，自己看自己时改投顶部网格。 */
    private boolean dropIntoBackpackView(ItemStack stack, Slot source) {
        return visitorLocked
                ? moveItemStack(stack, GRID_START, slots.size(), false, source)
                : moveItemStack(stack, 0, VISITOR_INVENTORY_SIZE, true, source);
    }
}
