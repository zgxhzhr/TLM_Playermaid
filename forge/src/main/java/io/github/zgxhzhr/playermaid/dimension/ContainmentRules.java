package io.github.zgxhzhr.playermaid.dimension;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.compat.slab.FoxSlabData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「魂符收容」维度的世界规则。
 *
 * <ul>
 *   <li>不允许放置方块、不允许使用物品（吃食物除外）；</li>
 *   <li>破坏方块会瞬间破坏并立刻补上一样的方块，且不掉落物品；</li>
 *   <li>连续破坏 10 次提示「魂符似乎发生了未知的变化」；</li>
 *   <li>连续破坏 20 次触发逃脱：破坏者被传送回当前持有者（其次收容者）旁边、
 *       魂符消失、双方收到对应提示。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContainmentRules {

    /** 连续破坏次数记录（按玩家 UUID）。 */
    private static final Map<UUID, Integer> BREAK_COUNTS = new ConcurrentHashMap<>();
    /** 待补回的方块（按坐标，维度内唯一）。 */
    private static final Map<BlockPos, BlockState> PENDING_RESTORE = new ConcurrentHashMap<>();

    /** 逃脱所需的连续破坏次数。 */
    private static final int ESCAPE_BREAKS = 20;
    /** 出现异变提示所需的连续破坏次数。 */
    private static final int WARN_BREAKS = 10;

    /** 记录每个玩家上次所在的维度，用于检测进入收容维度（含登录时已在收容维度的情况）。 */
    private static final Map<UUID, ResourceKey<Level>> LAST_DIMENSIONS = new ConcurrentHashMap<>();

    /** 房间外壳范围（与 ContainmentChunkGenerator 一致：18×18×18）。 */
    private static final int SHELL_MIN_X = -9;
    private static final int SHELL_MAX_X = 8;
    private static final int SHELL_MIN_Y = -1;
    private static final int SHELL_MAX_Y = 16;
    private static final int SHELL_MIN_Z = -9;
    private static final int SHELL_MAX_Z = 8;

    private ContainmentRules() {
    }

    /** 不允许放置方块。 */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (isContainment(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    /** 右键方块一律取消（阻止放方块与对方块使用物品）。 */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isContainment(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    /** 使用物品：除吃食物外一律取消。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!isContainment(event.getLevel())) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.getItem().isEdible()) {
            event.setCanceled(true);
        }
    }

    /** 破坏方块：计数、提示、逃脱；未逃脱时记录待补回方块。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!isContainment(event.getLevel())) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer breaker)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        int count = BREAK_COUNTS.merge(breaker.getUUID(), 1, Integer::sum);
        if (count == WARN_BREAKS) {
            breaker.sendSystemMessage(Component.literal("魂符似乎发生了未知的变化").withStyle(ChatFormatting.ITALIC));
        }
        if (count >= ESCAPE_BREAKS) {
            BREAK_COUNTS.remove(breaker.getUUID());
            escape(breaker);
            return;
        }
        // 方块被破坏后于下一个服务端 tick 原样补回（瞬间破坏、立刻复原）
        PENDING_RESTORE.put(pos, state);
    }

    /** 维度内掉落的物品直接移除（破坏不掉落）。 */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity && isContainment(event.getEntity().level())) {
            event.setCanceled(true);
        }
    }

    /** 每个服务端 tick 末尾补回待复原的方块。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_RESTORE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(ContainmentDimensions.CONTAINMENT);
        if (level == null) {
            PENDING_RESTORE.clear();
            return;
        }
        for (Map.Entry<BlockPos, BlockState> entry : PENDING_RESTORE.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        PENDING_RESTORE.clear();
    }

    /** 离开收容维度时清空破坏计数（“连续”以上一次离开为界）；进入收容维度时重置一次房间外壳。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        ResourceKey<Level> current = player.level().dimension();
        ResourceKey<Level> last = LAST_DIMENSIONS.put(player.getUUID(), current);
        if (!isContainment(player.level())) {
            BREAK_COUNTS.remove(player.getUUID());
            return;
        }
        // 进入/首次处于收容维度（上次不在、现在在；登录时已在也计入）：重置一次房间外壳
        if (!ContainmentDimensions.CONTAINMENT.equals(last)) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                ServerLevel level = server.getLevel(ContainmentDimensions.CONTAINMENT);
                if (level != null) {
                    resetShell(level);
                }
            }
        }
    }

    /** 收容维度世界加载时设置默认出生点，使玩家在该维度下线重上时重生在房间内（而非维度默认出生点）。 */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && isContainment(level)) {
            level.setDefaultSpawnPos(ContainmentDimensions.CONTAINMENT_SPAWN, 0.0F);
        }
    }

    /** 重置收容房间外壳：按生成器规则全量重设基岩/去皮橡木/空气，玩家破坏过的方块复原。 */
    private static void resetShell(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = SHELL_MIN_X; x <= SHELL_MAX_X; x++) {
            for (int y = SHELL_MIN_Y; y <= SHELL_MAX_Y; y++) {
                for (int z = SHELL_MIN_Z; z <= SHELL_MAX_Z; z++) {
                    BlockState target = ContainmentChunkGenerator.blockAt(x, y, z);
                    pos.set(x, y, z);
                    // 房间内近半数是空气，先比对方块状态可省下大量无谓的 setBlock
                    if (level.getBlockState(pos) == target) {
                        continue;
                    }
                    // flag 2 = 仅同步客户端，跳过邻居更新（外壳是固定结构，不需要连锁更新）
                    level.setBlock(pos, target, 2);
                }
            }
        }
    }

    /** 连续破坏 20 次：挣脱魂符。 */
    private static void escape(ServerPlayer breaker) {
        MinecraftServer server = breaker.getServer();
        if (server == null) {
            return;
        }
        // 找到装着该玩家的魂符及当前持有者（魂符允许丢弃/存箱，可能不在任何在线玩家身上）
        FoxSlabHolder holder = findFoxSlabHolder(server, breaker.getUUID());
        ItemStack slab = holder.stack();
        UUID containerUuid = slab.isEmpty() ? null : FoxSlabData.getContainerUuid(slab);
        if (!slab.isEmpty()) {
            removeSlab(server, slab);
        }
        // 传送：优先回到当前持有者身边，其次收容者，最后收容时的位置
        ServerLevel targetLevel = null;
        BlockPos targetPos = null;
        ServerPlayer owner = holder.player();
        if (owner == null && containerUuid != null) {
            owner = server.getPlayerList().getPlayer(containerUuid);
        }
        if (owner != null) {
            targetLevel = (ServerLevel) owner.level();
            BlockPos p = owner.blockPosition();
            targetPos = new BlockPos(p.getX(), p.getY(), p.getZ());
        } else if (!slab.isEmpty()) {
            targetLevel = server.getLevel(FoxSlabData.getCaptureDimension(slab));
            targetPos = FoxSlabData.getCapturePos(slab);
        }
        if (targetLevel != null && targetPos != null) {
            breaker.teleportTo(targetLevel, targetPos.getX() + 0.5, targetPos.getY(),
                    targetPos.getZ() + 0.5, breaker.getYRot(), breaker.getXRot());
        }
        breaker.sendSystemMessage(Component.literal("你凭借『守护』的意志，挣脱了魂符，回到了主人的旁边")
                .withStyle(ChatFormatting.ITALIC));
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("您的女仆因为未知的原因，突破了魂符，与您站在了一起")
                    .withStyle(ChatFormatting.ITALIC));
        }
    }

    /** 当前持有魂符的玩家与其魂符条目（持有者可能为 null：魂符存于箱子等容器中）。 */
    private record FoxSlabHolder(@Nullable ServerPlayer player, ItemStack stack) {
    }

    /** 在所有在线玩家背包里寻找装着指定玩家的魂符；返回持有者（可能为 null）与魂符。 */
    private static FoxSlabHolder findFoxSlabHolder(MinecraftServer server, UUID playerUuid) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : player.getInventory().items) {
                if (FoxSlabData.isFoxSlabFor(stack, playerUuid)) {
                    return new FoxSlabHolder(player, stack);
                }
            }
            for (ItemStack stack : player.getInventory().armor) {
                if (FoxSlabData.isFoxSlabFor(stack, playerUuid)) {
                    return new FoxSlabHolder(player, stack);
                }
            }
            ItemStack offhand = player.getInventory().offhand.get(0);
            if (FoxSlabData.isFoxSlabFor(offhand, playerUuid)) {
                return new FoxSlabHolder(player, offhand);
            }
        }
        return new FoxSlabHolder(null, ItemStack.EMPTY);
    }

    /** 移除装着指定玩家的魂符（遍历所有在线玩家背包）。 */
    private static void removeSlab(MinecraftServer server, ItemStack target) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack == target || (FoxSlabData.isFoxSlab(stack) && stack.is(target.getItem())
                        && FoxSlabData.getPlayerUuid(stack) != null
                        && FoxSlabData.getPlayerUuid(stack).equals(FoxSlabData.getPlayerUuid(target)))) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    return;
                }
            }
        }
    }

    private static boolean isContainment(LevelAccessor level) {
        return ContainmentDimensions.isContainment(level);
    }
}
