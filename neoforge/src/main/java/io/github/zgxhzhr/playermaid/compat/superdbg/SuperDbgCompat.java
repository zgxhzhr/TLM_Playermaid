package io.github.zgxhzhr.playermaid.compat.superdbg;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * 调试器（superdbg）软兼容层：读取其存储的玩家渲染名。
 *
 * <p>零编译依赖：superdbg 未安装时 {@link #LOADED} 为 false，收容逻辑回退到真实名字。
 * 反射说明：superdbg 随模组 jar 发布，其自有类名/方法名在开发与生产环境保持一致
 * （重混淆仅作用于原版 Minecraft 的方法名），按签名取 {@code get(Entity)} 即可。</p>
 */
public final class SuperDbgCompat {

    private static final String STORE_CLASS = "io.github.zgxhzhr.superdbg.entity.RenderNameStore";

    /** 调试器是否已加载且存储类可解析。 */
    public static final boolean LOADED = detect();

    private static Method get;

    private SuperDbgCompat() {
    }

    private static boolean detect() {
        try {
            Class<?> store = Class.forName(STORE_CLASS);
            // 按签名查找：静态方法、单个 Entity 参数、返回 String，不依赖具体方法名
            for (Method method : store.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(method.getModifiers())
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == Entity.class
                        && method.getReturnType() == String.class) {
                    get = method;
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // 调试器缺席或签名变化：视为未加载
        }
        return false;
    }

    /** 读取玩家渲染名；无渲染名或调试器缺席时返回 null。 */
    @Nullable
    public static String getRenderName(Entity player) {
        if (!LOADED || get == null) {
            return null;
        }
        try {
            return (String) get.invoke(null, player);
        } catch (Exception ignored) {
            return null;
        }
    }
}
