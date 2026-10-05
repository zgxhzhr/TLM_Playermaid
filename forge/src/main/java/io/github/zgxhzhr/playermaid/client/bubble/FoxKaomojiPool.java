package io.github.zgxhzhr.playermaid.client.bubble;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 随机聊天气泡的文案池。
 *
 * <p>数据直接读取车万女仆（Touhou Little Maid，作者 TartaricAcid，MIT 协议开源）的
 * 数据包文件 {@code touhou_little_maid:chat_bubble/kaomoji.json}，分类与合并规则
 * （core 并入 work/idle）与其本体保持一致。代码为移植实现，特此声明。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FoxKaomojiPool implements ResourceManagerReloadListener {

    private static final ResourceLocation KAOMOJI_FILE =
            new ResourceLocation("touhou_little_maid", "chat_bubble/kaomoji.json");

    private static final Gson GSON = new Gson();
    private static final Map<String, List<String>> POOLS = new HashMap<>();

    /** 是否已成功加载到文案（加载失败时不置位，以便后续限流重试）。 */
    private static boolean loaded;
    /** 上次尝试加载时的世界时间（tick），用于失败时限流重试。 */
    private static long lastAttemptTick = Long.MIN_VALUE;
    /** 加载失败后的重试间隔（tick）。 */
    private static final long RETRY_INTERVAL_TICKS = 200L;

    private FoxKaomojiPool() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new FoxKaomojiPool());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        load(resourceManager);
    }

    /**
     * 取某一分类的文案池；尚未成功加载时兜底加载一次。
     *
     * <p>若首次加载时资源包尚未就绪（池为空），会按 {@link #RETRY_INTERVAL_TICKS} 限流重试，
     * 避免因一次加载时机不对就永久拿不到文案、气泡再也不出现。</p>
     */
    public static List<String> pool(String category) {
        if (!loaded) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getResourceManager() != null) {
                long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
                if (now - lastAttemptTick >= RETRY_INTERVAL_TICKS) {
                    lastAttemptTick = now;
                    load(minecraft.getResourceManager());
                }
            }
        }
        return POOLS.getOrDefault(category, Collections.emptyList());
    }

    private static void load(ResourceManager resourceManager) {
        Map<String, List<String>> merged = new HashMap<>();
        resourceManager.listPacks().forEach(pack -> {
            IoSupplier<InputStream> supplier = pack.getResource(PackType.SERVER_DATA, KAOMOJI_FILE);
            if (supplier == null) {
                return;
            }
            try (InputStream inputStream = supplier.get();
                 Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Map<String, List<String>> data = GSON.fromJson(reader,
                        new TypeToken<Map<String, List<String>>>() {
                        }.getType());
                if (data != null) {
                    data.forEach((key, value) ->
                            merged.computeIfAbsent(key, k -> new ArrayList<>()).addAll(value));
                }
            } catch (Exception ignored) {
                // 单个数据包损坏时跳过，不影响其余数据包
            }
        });
        if (merged.isEmpty()) {
            // 一个数据包都没读到：保持未加载状态，等待下一次限流重试
            return;
        }
        // 与车万女仆一致：core 分类同时并入 work 与 idle
        List<String> core = merged.remove("core");
        if (core != null) {
            merged.computeIfAbsent("work", k -> new ArrayList<>()).addAll(core);
            merged.computeIfAbsent("idle", k -> new ArrayList<>()).addAll(core);
        }
        POOLS.clear();
        POOLS.putAll(merged);
        loaded = true;
    }
}
