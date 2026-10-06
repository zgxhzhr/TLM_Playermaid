package io.github.zgxhzhr.playermaid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IChestType;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidWirelessIOEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.chest.ChestManager;
import com.github.tartaricacid.touhoulittlemaid.item.ItemWirelessIO;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Arrays;
import java.util.Objects;

import static com.github.tartaricacid.touhoulittlemaid.util.BytesBooleansConvert.bytes2Booleans;

/**
 * 无线存取饰品（wireless_io）——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code WirelessIOBauble}：每 100 tick 在饰品绑定的箱子（物品 NBT 记录坐标）与
 * 佩戴者库存之间按方向/黑名单/槽位配置/过滤槽双向搬运物品。
 * 女仆侧使用女仆可用库存（与原版一致，含 {@code MaidWirelessIOEvent} 拦截点）；
 * 人是狐玩家侧使用独立女仆背包（FoxMaidData，36 格，对应原版女仆背包语义）。</p>
 *
 * <p>降级说明：原版距离取女仆活动半径 {@code getRestrictRadius}，玩家侧等效取固定
 * 32 格；玩家侧不触发 {@code MaidWirelessIOEvent}（事件构造需要女仆实体，注释说明）。</p>
 */
public class PlayerMaidWirelessIOBauble implements PlayerMaidBauble {

    private static final int SLOT_NUM = 38;
    private static final int CHECK_INTERVAL = 100;
    /** 玩家侧等效传输距离（原版取女仆活动半径）。 */
    private static final double PLAYER_MAX_DISTANCE = 32.0D;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide() || tick % CHECK_INTERVAL != 0) {
            return;
        }
        BlockPos bindingPos = ItemWirelessIO.getBindingPos(baubleItem);
        if (bindingPos == null) {
            return;
        }
        double maxDistance;
        if (entity instanceof EntityMaid maid) {
            if (maid.guiOpening) {
                return;
            }
            maxDistance = maid.getRestrictRadius();
        } else if (entity instanceof Player player) {
            FoxMaidData data = FoxMaidManager.peek(player).orElse(null);
            if (data == null || !data.isActive()) {
                return;
            }
            // 对应女仆侧 guiOpening：玩家打开容器时不搬运
            if (player.containerMenu != player.inventoryMenu) {
                return;
            }
            maxDistance = PLAYER_MAX_DISTANCE;
        } else {
            return;
        }

        if (entity.distanceToSqr(bindingPos.getX(), bindingPos.getY(), bindingPos.getZ())
                > (maxDistance * maxDistance)) {
            return;
        }
        BlockEntity te = entity.level().getBlockEntity(bindingPos);
        if (te == null) {
            return;
        }
        for (IChestType type : ChestManager.getAllChestTypes()) {
            if (!type.isChest(te)) {
                continue;
            }
            int openCount = type.getOpenCount(entity.level(), bindingPos, te);
            if (openCount > 0) {
                return;
            }
            te.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(chestInv -> {
                IItemHandler wearerInv;
                boolean postEvent = false;
                EntityMaid eventMaid = null;
                if (entity instanceof EntityMaid maid) {
                    wearerInv = maid.getAvailableInv(false);
                    postEvent = true;
                    eventMaid = maid;
                } else {
                    wearerInv = FoxMaidManager.getOrCreate((Player) entity).getBackpack();
                }
                boolean isMaidToChest = ItemWirelessIO.isMaidToChest(baubleItem);
                boolean isBlacklist = ItemWirelessIO.isBlacklist(baubleItem);
                byte[] slotConfig = ItemWirelessIO.getSlotConfig(baubleItem);
                byte[] slotConfigTmp = null;
                if (slotConfig != null) {
                    slotConfigTmp = Arrays.copyOf(slotConfig, slotConfig.length);
                    slotConfigTmp[wearerInv.getSlots() - 2] = slotConfigTmp[SLOT_NUM - 2];
                    slotConfigTmp[wearerInv.getSlots() - 1] = slotConfigTmp[SLOT_NUM - 1];
                }
                boolean[] slotConfigData = bytes2Booleans(slotConfigTmp, SLOT_NUM);
                IItemHandler filterList = ItemWirelessIO.getFilterList(baubleItem);

                if (isMaidToChest) {
                    if (!postEvent || !MinecraftForge.EVENT_BUS.post(
                            new MaidWirelessIOEvent.MaidToChest(eventMaid, wearerInv, chestInv,
                                    filterList, isBlacklist, slotConfigData))) {
                        maidToChest(wearerInv, chestInv, isBlacklist, filterList, slotConfigData);
                    }
                } else {
                    if (!postEvent || !MinecraftForge.EVENT_BUS.post(
                            new MaidWirelessIOEvent.ChestToMaid(eventMaid, wearerInv, chestInv,
                                    filterList, isBlacklist, slotConfigData))) {
                        chestToMaid(chestInv, wearerInv, isBlacklist, filterList, slotConfigData);
                    }
                }
            });
            return;
        }
    }

    private static void maidToChest(IItemHandler wearer, IItemHandler chest, boolean isBlacklist,
                                    IItemHandler filterList, boolean[] slotConfig) {
        for (int i = 0; i < wearer.getSlots(); i++) {
            if (i < slotConfig.length && slotConfig[i]) {
                continue;
            }
            ItemStack wearerInvItem = wearer.getStackInSlot(i);
            boolean allowMove = isBlacklist;
            for (int j = 0; j < filterList.getSlots(); j++) {
                ItemStack filterItem = filterList.getStackInSlot(j);
                boolean isEqual = ItemStack.isSameItem(wearerInvItem, filterItem);
                if (isEqual) {
                    allowMove = !isBlacklist;
                    break;
                }
            }
            if (allowMove) {
                int beforeCount = wearerInvItem.getCount();
                ItemStack after = ItemHandlerHelper.insertItemStacked(chest, wearerInvItem.copy(), false);
                int afterCount = after.getCount();
                if (beforeCount != afterCount) {
                    wearer.extractItem(i, beforeCount - afterCount, false);
                }
            }
        }
    }

    private static void chestToMaid(IItemHandler chest, IItemHandler wearer, boolean isBlacklist,
                                    IItemHandler filterList, boolean[] slotConfig) {
        for (int i = 0; i < chest.getSlots(); i++) {
            ItemStack chestInvStack = chest.getStackInSlot(i);
            boolean allowMove = isBlacklist;
            for (int j = 0; j < filterList.getSlots(); j++) {
                ItemStack filterItem = filterList.getStackInSlot(j);
                boolean isEqual = ItemStack.isSameItem(chestInvStack, filterItem);
                if (isEqual) {
                    allowMove = !isBlacklist;
                    break;
                }
            }
            if (allowMove) {
                int beforeCount = chestInvStack.getCount();
                ItemStack after = insertItemStacked(wearer, chestInvStack.copy(), false, slotConfig);
                int afterCount = after.getCount();
                if (beforeCount != afterCount) {
                    chest.extractItem(i, beforeCount - afterCount, false);
                }
            }
        }
    }

    private static ItemStack insertItemStacked(IItemHandler inventory, ItemStack stack, boolean simulate,
                                               boolean[] slotConfig) {
        if (stack.isEmpty()) {
            return stack;
        }
        if (!stack.isStackable()) {
            return insertItem(inventory, stack, simulate, slotConfig);
        }
        int sizeInventory = inventory.getSlots();
        for (int i = 0; i < sizeInventory; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slotConfig != null && i < slotConfig.length && slotConfig[i]) {
                continue;
            }
            if (canItemStacksStackRelaxed(slot, stack)) {
                stack = inventory.insertItem(i, stack, simulate);
                if (stack.isEmpty()) {
                    break;
                }
            }
        }
        if (!stack.isEmpty()) {
            for (int i = 0; i < sizeInventory; i++) {
                if (slotConfig != null && i < slotConfig.length && slotConfig[i]) {
                    continue;
                }
                if (inventory.getStackInSlot(i).isEmpty()) {
                    stack = inventory.insertItem(i, stack, simulate);
                    if (stack.isEmpty()) {
                        break;
                    }
                }
            }
        }
        return stack;
    }

    private static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate, boolean[] slotConfig) {
        if (stack.isEmpty()) {
            return stack;
        }
        for (int i = 0; i < dest.getSlots(); i++) {
            if (slotConfig != null && i < slotConfig.length && slotConfig[i]) {
                continue;
            }
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    private static boolean canItemStacksStackRelaxed(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) {
            return false;
        }
        if (!a.isStackable()) {
            return false;
        }
        if (a.hasTag() != b.hasTag()) {
            return false;
        }
        return (!a.hasTag() || Objects.equals(a.getTag(), Objects.requireNonNull(b.getTag())))
                && a.areCapsCompatible(b);
    }
}
