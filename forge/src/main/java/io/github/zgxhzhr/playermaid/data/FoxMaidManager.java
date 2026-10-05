package io.github.zgxhzhr.playermaid.data;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.network.NetworkHandler;
import io.github.zgxhzhr.playermaid.network.packet.SyncFoxStatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
            FoxMaidData data = new FoxMaidData();
            CompoundTag root = player.getPersistentData();
            if (root.contains(Constants.ROOT_TAG)) {
                data.read(root.getCompound(Constants.ROOT_TAG));
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
            player.getPersistentData().put(Constants.ROOT_TAG, data.write());
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
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                SyncFoxStatePacket.of(target.getId(), data));
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
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
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> tracker),
                    SyncFoxStatePacket.of(targetPlayer.getId(), data));
        }
    }
}
