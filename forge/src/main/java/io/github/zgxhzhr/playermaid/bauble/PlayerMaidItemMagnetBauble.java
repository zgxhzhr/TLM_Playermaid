package io.github.zgxhzhr.playermaid.bauble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;

import java.util.List;

/**
 * 物品磁铁饰品（item_magnet_bauble）——人是狐玩家版。
 *
 * <p>本文件代码移植自车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）
 * 的 {@code ItemMagnetBauble}：每 60 tick 拾取周围 6 格内的物品与经验球
 * （直接入袋并播拾取动画，与原版女仆一致）。</p>
 *
 * <p>降级说明：原版还拾取 P 点（EntityPowerPoint，车万女仆实体）与箭
 * （女仆专属拾取逻辑），玩家侧降级跳过，注释说明。</p>
 */
public class PlayerMaidItemMagnetBauble implements PlayerMaidBauble {

    private static final int DELAY = 3 * 20;
    private static final int RANGE = 6;

    @Override
    public void tickLivingEntity(LivingEntity entity, ItemStack baubleItem, int tick) {
        if (entity.level().isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        if (tick % DELAY != 0) {
            return;
        }
        handlePickup(player);
    }

    private static void handlePickup(Player player) {
        // 物品：直接插入玩家背包（含快捷栏与副手），数量变化则播拾取动画并同步实体
        List<ItemEntity> items = player.level().getEntitiesOfClass(
                ItemEntity.class, player.getBoundingBox().inflate(RANGE),
                e -> e.isAlive() && !e.hasPickUpDelay());
        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            int count = stack.getCount();
            ItemStack rest = ItemHandlerHelper.insertItemStacked(
                    new PlayerInvWrapper(player.getInventory()), stack, false);
            if (count == rest.getCount()) {
                continue;
            }
            player.take(itemEntity, count - rest.getCount());
            if (rest.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(rest);
            }
        }

        // 经验球：直接入经验（含经验修补语义由玩家自身处理）
        List<ExperienceOrb> orbs = player.level().getEntitiesOfClass(
                ExperienceOrb.class, player.getBoundingBox().inflate(RANGE),
                e -> e.isAlive() && e.tickCount > 2);
        for (ExperienceOrb orb : orbs) {
            player.take(orb, 1);
            player.giveExperiencePoints(orb.value);
            orb.discard();
        }
    }
}
