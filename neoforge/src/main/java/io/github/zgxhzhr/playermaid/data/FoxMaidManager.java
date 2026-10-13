package io.github.zgxhzhr.playermaid.data;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.neoforge.network.PacketDistributor;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import io.github.zgxhzhr.playermaid.network.packet.SyncFoxStatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

/**
 * 玩家人是狐数据管理器（服务端权威）。
 *
 * <p>运行期数据缓存在内存，登录时从玩家 persistentData 读取；变更后立即写回
 * persistentData（Forge 会随玩家存档自动落盘）；死亡复活时通过
 * {@link PlayerEvent.Clone} 手动复制。</p>
 */
public final class FoxMaidManager {

    private static final Map<UUID, FoxMaidData> CACHE = new ConcurrentHashMap<>();

    private FoxMaidManager() {
    }

    /**
     * 获取（必要时从存档加载）玩家的人是狐数据。仅服务端调用。
     */
    public static FoxMaidData getOrCreate(Player player) {
        return CACHE.computeIfAbsent(player.getUUID(), uuid -> {
            // 活跃状态一旦实际改变（开→关/关→开）立即广播一次状态包，
            // 不等 20 tick 脏标记周期——覆盖命令、API、调试器反射等一切写入路径，
            // 保证客户端"人是狐"与饰品栏标志（含梦云水晶光环判定）即时刷新。
            FoxMaidData data = new FoxMaidData(() -> persistAndSync(player));
            CompoundTag root = player.getPersistentData();
            if (root.contains(Constants.ROOT_TAG)) {
                data.read(player.registryAccess(), root.getCompound(Constants.ROOT_TAG));
            }
            return data;
        });
    }

    /**
     * 查询缓存中的数据（可能为空，例如目标尚未登录或数据为默认）。
     */
    public static Optional<FoxMaidData> peek(Player player) {
        return Optional.ofNullable(CACHE.get(player.getUUID()));
    }

    /**
     * 数据写回玩家 persistentData（随玩家存档自动落盘）。
     */
    public static void persist(Player player) {
        FoxMaidData data = CACHE.get(player.getUUID());
        if (data != null) {
            player.getPersistentData().put(Constants.ROOT_TAG, data.write(player.registryAccess()));
            data.clearDirty();
        }
    }

    /**
     * 落盘并向本人与所有追踪者同步标量状态。
     */
    public static void persistAndSync(Player target) {
        if (target.level().isClientSide) {
            return;
        }
        FoxMaidData data = getOrCreate(target);
        persist(target);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                SyncFoxStatePacket.of(target.getId(), data));
    }

    // ===== 主人（真实实体引用） =====

    /**
     * 解析某玩家当前的主人实体；无主人、主人在线但已离线、或该玩家不是服务端玩家时返回 null。
     *
     * <p>主人不存在于世界时（离线）一律视为「无主人在场」：饰品主人联动静默跳过、
     * 背包权限退回未认主时的开放规则。</p>
     */
    @Nullable
    public static ServerPlayer resolveOwner(Player maid) {
        FoxMaidData data = CACHE.get(maid.getUUID());
        if (data == null || data.getOwnerUuid() == null) {
            return null;
        }
        MinecraftServer server = maid.getServer();
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(data.getOwnerUuid());
    }

    /**
     * 按 UUID 认主（UUID 可能来自界面选择或反射 API）：仅当该 UUID 对应在线玩家时才写入，
     * 并用其当前名字刷新显示名；传入 null 或解析不到时清除主人（UUID 与显示名一并清）。
     */
    public static void setOwnerByUuid(ServerPlayer maid, @Nullable UUID ownerUuid) {
        applyOwnerByUuid(getOrCreate(maid), maid, ownerUuid);
        persistAndSync(maid);
    }

    /**
     * 认主的纯写入部分（不落库不广播，供需要把主人与其它设置合并成一次同步的调用方使用）。
     *
     * @return 解析成功的主人实体；UUID 为 null 或该玩家不在线时返回 null，并清空主人引用
     */
    @Nullable
    public static ServerPlayer applyOwnerByUuid(FoxMaidData data, Player maid, @Nullable UUID ownerUuid) {
        MinecraftServer server = maid.getServer();
        ServerPlayer owner = ownerUuid == null || server == null
                ? null
                : server.getPlayerList().getPlayer(ownerUuid);
        data.setOwnerUuid(owner == null ? null : owner.getUUID());
        data.setOwnerName(owner == null ? null : owner.getName().getString());
        return owner;
    }

    /**
     * 按显示名认主：名字能对上线上的玩家就建立真实引用，否则只留显示名、清空引用
     * （保持调试器「只填一个名字」的旧行为可用，同时不允许伪造不存在的绑定）。
     *
     * <p>名字与既有显示名相同时保留既有主人引用：调试器（superdbg）提交快照时会把
     * 读到的显示名原样回填，若此处按「解析不到就清空」处理，主人一旦离线，
     * 经调试器保存一次就会把真实主人关系抹掉。</p>
     */
    public static void setOwnerByName(ServerPlayer maid, @Nullable String ownerName) {
        FoxMaidData data = getOrCreate(maid);
        String normalized = ownerName == null || ownerName.isBlank() ? null : ownerName.trim();
        if (java.util.Objects.equals(normalized, data.getOwnerName())) {
            data.setOwnerName(normalized);
            persistAndSync(maid);
            return;
        }
        ServerPlayer owner = null;
        if (normalized != null && maid.getServer() != null) {
            owner = maid.getServer().getPlayerList().getPlayerByName(normalized);
        }
        data.setOwnerName(normalized);
        data.setOwnerUuid(owner == null ? null : owner.getUUID());
        persistAndSync(maid);
    }

    /**
     * 访问者是否有权打开目标玩家的背包：目标未认主时维持开放规则；
     * 已认主时仅主人本人可开（本人打开自己的界面走 visitor == target 分支，不经过此判定）。
     */
    public static boolean canBeOpenedBy(Player target, Player visitor) {
        FoxMaidData data = CACHE.get(target.getUUID());
        if (data == null || !data.hasOwner()) {
            return true;
        }
        return data.getOwnerUuid().equals(visitor.getUUID());
    }

    // ===== 旧存档迁移 =====

    /**
     * 旧存档迁移：早期版本激活人是狐时会把玩家背包转移到独立的“女仆大背包”存储，
     * 现已改为界面顶部网格直接映射真实背包。这里把旧数据里残留的物品归还到玩家
     * 真实背包，放不下的掉落在脚下，避免升级后物品丢失。
     */
    private static void migrateLegacyBackpack(Player player, FoxMaidData data) {
        ItemStackHandler backpack = data.getBackpack();
        boolean moved = false;
        for (int i = 0; i < backpack.getSlots(); i++) {
            ItemStack stack = backpack.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            backpack.setStackInSlot(i, ItemStack.EMPTY);
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
            moved = true;
        }
        if (moved) {
            persist(player);
        }
    }

    // ===== Forge 事件（静态注册） =====

    /** 每 20 tick 检查一次：数据有改动（如饰品栏变动）就落盘并广播，保证展示状态及时同步。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 20 != 0) {
            return;
        }
        FoxMaidData data = CACHE.get(player.getUUID());
        if (data != null && data.isDirty()) {
            persistAndSync(player);
        }
    }

    /** 玩家登录：重建缓存并同步给本人（追踪者随后由 StartTracking 处理）。 */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        FoxMaidData data = getOrCreate(player);
        migrateLegacyBackpack(player, data);
        persistAndSync(player);
    }

    /** 玩家重进世界（资源重载/重连等）：也同步一次。 */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        // 复活后实体是新对象：以 UUID 取旧缓存；Clone 已把 NBT 复制到新实体
        getOrCreate(player);
        persistAndSync(player);
    }

    /** 玩家登出：清缓存（数据已在 persistentData 中）。 */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        persist(player);
        CACHE.remove(player.getUUID());
    }

    /**
     * 死亡复活：把原玩家的人是狐数据复制到新实体（调试性质功能，死亡不清理）。
     */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CompoundTag original = event.getOriginal().getPersistentData();
            if (original.contains(Constants.ROOT_TAG)) {
                event.getEntity().getPersistentData()
                        .put(Constants.ROOT_TAG, original.getCompound(Constants.ROOT_TAG).copy());
            }
        }
    }

    /** 玩家开始追踪目标玩家：把目标当前状态补发给追踪者。 */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (target.level().isClientSide || !(target instanceof Player targetPlayer)) {
            return;
        }
        FoxMaidData data = CACHE.get(targetPlayer.getUUID());
        if (data == null) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer tracker) {
            PacketDistributor.sendToPlayer(tracker,
                    SyncFoxStatePacket.of(targetPlayer.getId(), data));
        }
    }
}
