package io.github.zgxhzhr.playermaid.menu;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 人是狐女仆饰品栏容器（仿车万女仆 {@code BaubleContainer}）。
 *
 * <p>槽位顺序：0-35 访问者背包；36-39 目标护甲；40-41 目标主副手；
 * 42-71 女仆饰品 30 格（5 列 × 6 排，全部解锁）。仅接受 TLM 女仆饰品，每格 1 个。</p>
 */
public class FoxMaidBaubleMenu extends FoxMaidBaseMenu {

    private static final int BAUBLE_START_X = 152;
    private static final int BAUBLE_START_Y = 45;
    private static final int COLUMNS = 5;
    private static final int ROWS = 6;

    /** 装备区（护甲 + 主副手）结束位置，也是饰品区起始索引。 */
    private static final int EQUIPMENT_END = VISITOR_INVENTORY_SIZE + 6;

    /** 客户端构造。 */
    public FoxMaidBaubleMenu(int id, Inventory visitorInventory, FriendlyByteBuf buf) {
        this(id, visitorInventory, resolveTarget(visitorInventory, buf.readInt()));
    }

    /** 服务端构造。 */
    public FoxMaidBaubleMenu(int id, Inventory visitorInventory, Player target) {
        super(FoxMaidMenus.BAUBLE.get(), id, visitorInventory, target);
        // 客户端使用独立的虚拟槽位（同主菜单：避免单人集成服下点击预测双重执行），
        // 校验规则与真实饰品栏一致，内容由容器同步覆盖。
        ItemStackHandler baubles = target.level().isClientSide
                ? FoxMaidData.createBaubleHandler(null)
                : FoxMaidManager.getOrCreate(target).getBaubles();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = col + row * COLUMNS;
                addSlot(new SlotItemHandler(baubles, index,
                        BAUBLE_START_X + 18 * col, BAUBLE_START_Y + 18 * row));
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < VISITOR_INVENTORY_SIZE) {
            // 先尝试饰品区，失败再尝试装备区
            if (!moveItemStackTo(original, EQUIPMENT_END, this.slots.size(), false)) {
                if (!moveItemStackTo(original, VISITOR_INVENTORY_SIZE, EQUIPMENT_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!moveItemStackTo(original, 0, VISITOR_INVENTORY_SIZE, true)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (original.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, original);
        return copy;
    }
}
