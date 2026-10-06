package io.github.zgxhzhr.playermaid.compat.slab;

import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.api.FoxMaidApi;
import io.github.zgxhzhr.playermaid.dimension.ContainmentDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;

/**
 * 魂符收容「人是狐」玩家。
 *
 * <p>空魂符右键人是狐玩家 → 收容：写入标记数据块、把空魂符换成装人的魂符、
 * 播放与收容女仆一致的音效、把被收容玩家传送到收容维度（提示文字为斜体）；
 * 装有人的魂符右键 → 放出：把玩家传送到准心落点（仅收容者本人可放出）。
 * 装有人的魂符允许正常丢弃/存入箱子（不拦截）。</p>
 *
 * <p>纯服务端逻辑：客户端只负责取消原版交互，传送与数据修改均在服务端完成。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoxSlabHandler {

    private FoxSlabHandler() {
    }

    /** 空魂符右键人是狐玩家 → 收容。 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide() != LogicalSide.SERVER) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer fox)) {
            return;
        }
        if (!FoxMaidApi.isActive(fox)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (held.getItem() != InitItems.SMART_SLAB_EMPTY.get()) {
            return;
        }
        event.setCanceled(true);
        ItemStack slab = FoxSlabData.makeFoxSlab(fox, player);
        player.setItemInHand(event.getHand(), slab);
        player.getCooldowns().addCooldown(InitItems.SMART_SLAB_HAS_MAID.get(), 20);
        // 与收容女仆一致：溅水音效
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.PLAYER_SPLASH,
                SoundSource.PLAYERS, 1.0F, fox.level().random.nextFloat() * 0.1F + 0.9F);
        ContainmentDimensions.teleportInto(fox);
        fox.sendSystemMessage(Component.literal("你被收容到魂符中").withStyle(ChatFormatting.ITALIC));
    }

    /** 右键方块：装有人的魂符 → 放出（收容者本人），并阻止原版魂符的放置行为。 */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !FoxSlabData.isFoxSlab(held)) {
            return;
        }
        event.setCanceled(true);
        // 放出落点为准心命中点（getHitVec 返回 BlockHitResult，取其位置 Vec3）
        tryRelease(event.getEntity(), held, event.getHand(), event.getHitVec().getLocation());
    }

    /** 右键空气：装有人的魂符 → 放出（收容者本人）。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !FoxSlabData.isFoxSlab(held)) {
            return;
        }
        event.setCanceled(true);
        tryRelease(event.getEntity(), held, event.getHand(), event.getEntity().position());
    }

    /**
     * 放出被收容玩家到指定落点。
     *
     * @param targetPos 传出落点（右键方块时为准心命中点，右键空气时为玩家位置）
     */
    private static void tryRelease(Player player, ItemStack stack, InteractionHand hand, Vec3 targetPos) {
        if (player.level().isClientSide) {
            return;
        }
        // 仅收容者本人可放出
        if (!player.getUUID().equals(FoxSlabData.getContainerUuid(stack))) {
            player.sendSystemMessage(Component.literal("这不是您的魂符，无法放出其中的女仆")
                    .withStyle(ChatFormatting.ITALIC));
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer fox = server.getPlayerList().getPlayer(FoxSlabData.getPlayerUuid(stack));
        if (fox == null) {
            player.sendSystemMessage(Component.literal("被收容的玩家不在线，无法放出")
                    .withStyle(ChatFormatting.ITALIC));
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        fox.teleportTo(level, targetPos.x, targetPos.y, targetPos.z, fox.getYRot(), fox.getXRot());
        // 与放出女仆一致：溅水音效
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.PLAYER_SPLASH,
                SoundSource.PLAYERS, 1.0F, fox.level().random.nextFloat() * 0.1F + 0.9F);
        fox.sendSystemMessage(Component.literal("你被放出了魂符").withStyle(ChatFormatting.ITALIC));
        player.setItemInHand(hand, InitItems.SMART_SLAB_EMPTY.get().getDefaultInstance());
        player.getCooldowns().addCooldown(InitItems.SMART_SLAB_EMPTY.get(), 20);
    }
}
