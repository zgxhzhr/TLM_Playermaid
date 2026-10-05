package io.github.zgxhzhr.playermaid.client;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端人是狐状态缓存（仅客户端使用）。
 */
public final class ClientFoxState {

    private static final Map<Integer, FoxMaidStateView> VIEWS = new ConcurrentHashMap<>();

    private ClientFoxState() {
    }

    /**
     * 收到服务端同步包后更新缓存。
     */
    public static void update(FoxMaidStateView view) {
        VIEWS.put(view.entityId(), view);
    }

    public static Optional<FoxMaidStateView> get(int entityId) {
        return Optional.ofNullable(VIEWS.get(entityId));
    }

    public static Optional<FoxMaidStateView> get(Entity entity) {
        return get(entity.getId());
    }

    /**
     * 该实体是否处于人是狐状态。
     */
    public static boolean isActive(@Nullable Entity entity) {
        return entity != null && get(entity.getId()).map(FoxMaidStateView::active).orElse(false);
    }

    /**
     * 该实体的自定义渲染名（无则 null）。
     */
    @Nullable
    public static String getRenderName(@Nullable Entity entity) {
        if (entity == null) {
            return null;
        }
        return get(entity.getId()).map(FoxMaidStateView::renderName).orElse(null);
    }

    /**
     * 断开连接时清空缓存，避免残留到下个世界。
     */
    public static void clear() {
        VIEWS.clear();
    }
}
