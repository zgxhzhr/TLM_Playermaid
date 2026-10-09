package io.github.zgxhzhr.playermaid.menu;

import com.mojang.datafixers.util.Pair;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;

import java.util.function.BooleanSupplier;

import static net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
import static net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;

/**
 * 人是狐容器基类：负责访问者背包与目标玩家装备槽（4 护甲 + 主副手）的布局，
 * 坐标与车万女仆 {@code MaidMainContainer} 完全一致。
 */
public abstract class FoxMaidBaseMenu extends AbstractContainerMenu {

    /** 访问者（打开界面的玩家）背包槽数量。 */
    protected static final int VISITOR_INVENTORY_SIZE = 36;

    protected static final ResourceLocation EMPTY_MAINHAND_SLOT = new ResourceLocation("item/empty_slot_sword");
    protected static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{
            net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
    };

    /** 被查看的目标玩家。 */
    protected final Player target;

    /** 访问者背包是否被锁定为只读（自己查看自己时为真）。 */
    protected final boolean visitorLocked;

    protected FoxMaidBaseMenu(@Nullable MenuType<?> type, int id, Inventory visitorInventory, Player target) {
        super(type, id);
        this.target = target;
        // 自己查看自己时，底部访问者背包与顶部女仆背包指向的是同一份真实背包数据，
        // 两处都能操作会让同一件物品既在这里被取走又在那边被取出，因此底部统一锁为只读展示。
        this.visitorLocked = visitorInventory.player == target;
        addVisitorInventory(visitorInventory, visitorLocked);
        addTargetEquipment();
    }

    /** 访问者背包：三排主背包 + 一排快捷栏（车万女仆坐标）。 */
    private void addVisitorInventory(Inventory visitorInventory, boolean locked) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(createSlot(visitorInventory, col + row * 9 + 9, 88 + col * 18, 174 + row * 18, locked));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(createSlot(visitorInventory, col, 88 + col * 18, 232, locked));
        }
    }

    /** 恒定锁定的条件。 */
    private static final BooleanSupplier ALWAYS_LOCKED = () -> true;

    /** 按需创建普通槽或只读锁定槽。 */
    protected static Slot createSlot(Container container, int index, int x, int y, boolean locked) {
        return locked
                ? new LockedSlot(container, index, x, y, ALWAYS_LOCKED)
                : new Slot(container, index, x, y);
    }

    /**
     * 只读锁定槽：仍然映射真实的背包数据（内容照常显示与同步），
     * 但在 {@code locked} 条件成立时既不能取出也不能放入，界面上会叠加灰色遮罩。
     *
     * <p>条件按当前背包状态实时求值，因此锁定与否可以随数据变化（例如主手清空后自动恢复可用）。</p>
     */
    protected static class LockedSlot extends Slot {

        private final BooleanSupplier locked;

        LockedSlot(Container container, int index, int x, int y, BooleanSupplier locked) {
            super(container, index, x, y);
            this.locked = locked;
        }

        /** 此刻是否处于锁定状态。 */
        boolean isLocked() {
            return locked.getAsBoolean();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isLocked();
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isLocked();
        }
    }

    /** 该槽此刻是否为只读锁定槽（界面据此叠加灰色遮罩）。 */
    public static boolean isLocked(Slot slot) {
        return slot instanceof LockedSlot locked && locked.isLocked();
    }

    /**
     * 目标玩家装备槽：2×2 护甲 + 主手/副手。
     * 槽位索引 36-39 为护甲（头/胸/腿/脚），40 主手，41 副手。
     */
    private void addTargetEquipment() {
        // (94,37) 头、(114,37) 胸、(94,57) 护腿、(114,57) 靴子
        addArmorSlot(EquipmentSlot.HEAD, 39, 94, 37);
        addArmorSlot(EquipmentSlot.CHEST, 38, 114, 37);
        addArmorSlot(EquipmentSlot.LEGS, 37, 94, 57);
        addArmorSlot(EquipmentSlot.FEET, 36, 114, 57);
        // 主手（打开瞬间的手持槽）
        addSlot(new Slot(target.getInventory(), target.getInventory().selected, 87, 77) {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(BLOCK_ATLAS, EMPTY_MAINHAND_SLOT);
            }
        });
        // 副手
        addSlot(new Slot(target.getInventory(), Inventory.SLOT_OFFHAND, 121, 77) {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(BLOCK_ATLAS, EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
    }

    private void addArmorSlot(EquipmentSlot equipmentSlot, int inventoryIndex, int x, int y) {
        addSlot(new Slot(target.getInventory(), inventoryIndex, x, y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.canEquip(equipmentSlot, target) && stack.getItem().canFitInsideContainerItems();
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                ItemStack itemstack = getItem();
                boolean curseEnchant = !itemstack.isEmpty() && !playerIn.isCreative()
                        && EnchantmentHelper.hasBindingCurse(itemstack);
                return !curseEnchant && super.mayPickup(playerIn);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[equipmentSlot.getIndex()]);
            }
        });
    }

    public Player getTarget() {
        return target;
    }

    /**
     * 按实体 id 解析目标玩家；无效（未加载）时回退到访问者本人，避免槽位缺失导致同步崩溃。
     */
    protected static Player resolveTarget(Inventory visitorInventory, int entityId) {
        Player self = visitorInventory.player;
        if (self.level().getEntity(entityId) instanceof Player player) {
            return player;
        }
        return self;
    }

    /**
     * 容器关闭（服务端）：把目标玩家的人是狐数据写回 persistentData，并立即广播状态包，
     * 确保背包/饰品改动既及时落盘、又及时同步到客户端。
     *
     * <p>此处必须用 {@code persistAndSync} 而非 {@code persist}：{@code persist} 只写 NBT
     * 并清脏标记、不发包。饰品栏变动（含放置/取下梦云水晶）会置脏，若关闭界面时用
     * {@code persist}，脏标记被清掉却从未广播，客户端 {@code hasDreamCrystal} 会一直停留在
     * 旧值，导致头顶光环迟迟不出现（要等到登出重进、复活或他人开始追踪才补发）。</p>
     */
    @Override
    public void removed(Player visitor) {
        super.removed(visitor);
        if (!target.level().isClientSide) {
            FoxMaidManager.persistAndSync(target);
        }
    }

    @Override
    public boolean stillValid(Player visitor) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        // 自己打开自己：始终有效
        if (visitor == target) {
            return true;
        }
        FoxMaidData data = FoxMaidManager.peek(target).orElse(null);
        if (data == null || !data.isActive()) {
            return false;
        }
        // 与车万女仆一致的可达距离（放宽到 8 格，方便多人查看）
        return visitor.canReach(target, 8.0);
    }

    /**
     * shift 移动的收尾：清空来源槽、标记变化，返回本次被移动的物品副本。
     *
     * <p>子类实现 {@link #quickMoveStack} 时，把实际搬运结果 {@code moved} 传进来即可
     * 复用这段逻辑，不必各自重复。</p>
     *
     * @return 实际移动了的物品；没有移动时返回空
     */
    protected ItemStack finishQuickMove(Player player, Slot slot, ItemStack original, ItemStack copy,
                                        boolean moved) {
        if (!moved) {
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

    /**
     * 与原版 {@code moveItemStackTo} 相同的合并/填充逻辑。
     *
     * <p>额外跳过与来源槽重叠的目标槽：自己查看自己时顶部映射槽与底部访问者槽
     * 指向同一份真实背包数据，若不跳过，同一格会被识别为「可堆叠目标」而把数量翻倍。</p>
     */
    protected boolean moveItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection,
                                    Slot source) {
        boolean changed = false;
        if (stack.isStackable()) {
            for (int i = reverseDirection ? endIndex - 1 : startIndex;
                 !stack.isEmpty() && (reverseDirection ? i >= startIndex : i < endIndex);
                 i += reverseDirection ? -1 : 1) {
                Slot slot = slots.get(i);
                ItemStack existing = slot.getItem();
                if (!isSameSlot(slot, source) && slot.mayPlace(stack) && !existing.isEmpty()
                        && ItemStack.isSameItemSameTags(stack, existing)) {
                    int total = existing.getCount() + stack.getCount();
                    if (total <= stack.getMaxStackSize()) {
                        stack.setCount(0);
                        existing.setCount(total);
                        slot.setChanged();
                        changed = true;
                    } else if (existing.getCount() < stack.getMaxStackSize()) {
                        stack.shrink(stack.getMaxStackSize() - existing.getCount());
                        existing.setCount(stack.getMaxStackSize());
                        slot.setChanged();
                        changed = true;
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            for (int i = reverseDirection ? endIndex - 1 : startIndex;
                 reverseDirection ? i >= startIndex : i < endIndex;
                 i += reverseDirection ? -1 : 1) {
                Slot slot = slots.get(i);
                if (!isSameSlot(slot, source) && slot.mayPlace(stack) && !slot.hasItem()) {
                    int max = Math.min(slot.getMaxStackSize(), stack.getMaxStackSize());
                    slot.setByPlayer(stack.split(Math.min(max, stack.getCount())));
                    slot.setChanged();
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    /** 两个槽是否指向同一份底层数据（同一容器且同一下标）。 */
    private static boolean isSameSlot(Slot a, Slot b) {
        return a.container == b.container && a.getContainerSlot() == b.getContainerSlot();
    }

    protected static Component title(String key) {
        return Component.translatable(key);
    }
}
