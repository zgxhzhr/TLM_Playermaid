package io.github.zgxhzhr.playermaid.menu;

import com.mojang.datafixers.util.Pair;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;

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

    protected FoxMaidBaseMenu(@Nullable MenuType<?> type, int id, Inventory visitorInventory, Player target) {
        super(type, id);
        this.target = target;
        addVisitorInventory(visitorInventory);
        addTargetEquipment();
    }

    /** 访问者背包：三排主背包 + 一排快捷栏（车万女仆坐标）。 */
    private void addVisitorInventory(Inventory visitorInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(visitorInventory, col + row * 9 + 9, 88 + col * 18, 174 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(visitorInventory, col, 88 + col * 18, 232));
        }
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
     * 容器关闭（服务端）：把目标玩家的人是狐数据写回 persistentData，
     * 确保背包/饰品改动及时落盘，不必等到玩家登出。
     */
    @Override
    public void removed(Player visitor) {
        super.removed(visitor);
        if (!target.level().isClientSide) {
            FoxMaidManager.persist(target);
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
     * 通用 shift 移动：访问者背包 ↔ 目标区域。
     *
     * <p>自己查看自己时，顶部映射槽与底部访问者槽指向同一份真实背包数据；
     * 若直接调用原版 {@code moveItemStackTo}，同一格会被识别为「可堆叠目标」
     * 而把数量翻倍。因此这里跳过与来源槽指向同一（容器, 下标）的目标槽。</p>
     *
     * @param targetStart 目标区域起始槽索引（含）
     * @param targetEnd   目标区域结束槽索引（不含）
     */
    protected ItemStack quickMoveBetween(Player player, int index, int targetStart, int targetEnd,
                                         boolean preferTargetTail) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < VISITOR_INVENTORY_SIZE) {
            if (!moveItemStack(original, targetStart, targetEnd, preferTargetTail, slot)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStack(original, 0, VISITOR_INVENTORY_SIZE, true, slot)) {
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

    /** 与原版 {@code moveItemStackTo} 相同的合并/填充逻辑，额外跳过与来源槽重叠的目标槽。 */
    private boolean moveItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection,
                                  Slot source) {
        boolean changed = false;
        if (stack.isStackable()) {
            for (int i = reverseDirection ? endIndex - 1 : startIndex;
                 !stack.isEmpty() && (reverseDirection ? i >= startIndex : i < endIndex);
                 i += reverseDirection ? -1 : 1) {
                Slot slot = slots.get(i);
                ItemStack existing = slot.getItem();
                if (!isSameSlot(slot, source) && !existing.isEmpty()
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
