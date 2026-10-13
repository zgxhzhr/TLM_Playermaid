package io.github.zgxhzhr.playermaid.compat.slab;

import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.mojang.logging.LogUtils;
import io.github.zgxhzhr.playermaid.api.FoxMaidApi;
import io.github.zgxhzhr.playermaid.compat.superdbg.SuperDbgCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 装有「人是狐」玩家的魂符数据读写。
 *
 * <p>复用车万女仆的魂符（touhou_little_maid:smart_slab_has_maid）作为收容容器，
 * 但不再沿用其女仆 NBT（MaidInfo），而是写入本模组独立的标记数据块，
 * 因此车万女仆不会把其中的数据当作女仆放出（其 hasMaidData 判定为否）。
 * 标记数据块存放被收容玩家的 UUID、收容者 UUID 与收容位置，用于放出与逃脱传送。</p>
 */
public final class FoxSlabData {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 魂符上的标记数据块键。 */
    public static final String TAG = "playermaid:fox_slab";

    private static final String PLAYER_UUID = "PlayerUUID";
    private static final String PLAYER_NAME = "PlayerName";
    private static final String SLAB_MODEL_ID = "SlabModelId";
    private static final String CONTAINER_UUID = "ContainerUUID";
    private static final String CONTAINER_NAME = "ContainerName";
    private static final String CAPTURE_DIM = "CaptureDim";
    private static final String CAPTURE_X = "CaptureX";
    private static final String CAPTURE_Y = "CaptureY";
    private static final String CAPTURE_Z = "CaptureZ";

    private FoxSlabData() {
    }

    /** 是否为装有「人是狐」玩家的魂符。 */
    public static boolean isFoxSlab(ItemStack stack) {
        // 1.21.1 物品 NBT（hasTag/getTag）已移除：标记数据块改存在原版 CUSTOM_DATA 组件里
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(TAG);
    }

    /**
     * 读取魂符上的标记数据块。
     *
     * <p>1.21.1 用原版 {@code CUSTOM_DATA} 组件承载（等价于 1.20.1 物品 NBT 根标签下的 {@link #TAG} 子标签）；
     * 该组件只存本模组自己的数据，不写车万女仆的 MAID_INFO 组件，因此不会被当作女仆放出。</p>
     */
    @Nullable
    private static CompoundTag getFoxTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag root = customData.copyTag();
        // 与原版 getTagElement 语义对齐：不存在或类型不符都返回 null
        return root.contains(TAG, Tag.TAG_COMPOUND) ? root.getCompound(TAG) : null;
    }

    /** 该魂符是否装着指定玩家。 */
    public static boolean isFoxSlabFor(ItemStack stack, UUID playerUuid) {
        return isFoxSlab(stack) && playerUuid.equals(getPlayerUuid(stack));
    }

    /** 生成装有指定玩家的魂符（收容时调用）。 */
    public static ItemStack makeFoxSlab(ServerPlayer fox, Player container) {
        ItemStack slab = InitItems.SMART_SLAB_HAS_MAID.get().getDefaultInstance();
        // 1.21.1 无 getOrCreateTagElement：先在独立 CompoundTag 里写入，末尾整体存进 CUSTOM_DATA 组件
        CompoundTag data = new CompoundTag();
        data.putUUID(PLAYER_UUID, fox.getUUID());
        // 魂符显示名优先用调试器定义的渲染名，无则回退真实名字
        String renderName = SuperDbgCompat.getRenderName(fox);
        String displayName = (renderName == null || renderName.isBlank())
                ? fox.getGameProfile().getName() : renderName;
        data.putString(PLAYER_NAME, displayName);
        // 魂符展示模型 id：玩家配置了车万女仆模型 id 才写入（空值不写，读取端容错）
        String slabModelId = FoxMaidApi.getSlabModelId(fox);
        LOGGER.info("[playermaid] FoxSlabData.makeFoxSlab: getSlabModelId={}", slabModelId);
        if (slabModelId != null) {
            data.putString(SLAB_MODEL_ID, slabModelId);
        }
        data.putUUID(CONTAINER_UUID, container.getUUID());
        data.putString(CONTAINER_NAME, container.getName().getString());
        data.putString(CAPTURE_DIM, fox.level().dimension().location().toString());
        data.putInt(CAPTURE_X, fox.getBlockX());
        data.putInt(CAPTURE_Y, fox.getBlockY());
        data.putInt(CAPTURE_Z, fox.getBlockZ());
        // 把标记数据块写进 CUSTOM_DATA 组件（update 语义为合并写入，不覆盖组件里其他内容）
        CustomData.update(DataComponents.CUSTOM_DATA, slab, root -> root.put(TAG, data));
        // 打印实际写入魂符的数据（未写入的模型 id 读出为空串）
        LOGGER.info("[playermaid] FoxSlabData.makeFoxSlab: playerName={} slabModelIdWritten={}",
                displayName, data.getString(SLAB_MODEL_ID));
        return slab;
    }

    @Nullable
    public static UUID getPlayerUuid(ItemStack stack) {
        // 1.21.1 无 getTagElement：改为从 CUSTOM_DATA 组件读取标记数据块
        CompoundTag data = getFoxTag(stack);
        return data == null ? null : data.getUUID(PLAYER_UUID);
    }

    @Nullable
    public static UUID getContainerUuid(ItemStack stack) {
        // 1.21.1 无 getTagElement：改为从 CUSTOM_DATA 组件读取标记数据块
        CompoundTag data = getFoxTag(stack);
        return data == null ? null : data.getUUID(CONTAINER_UUID);
    }

    @Nullable
    public static String getPlayerName(ItemStack stack) {
        // 1.21.1 无 getTagElement：改为从 CUSTOM_DATA 组件读取标记数据块
        CompoundTag data = getFoxTag(stack);
        return data == null ? null : data.getString(PLAYER_NAME);
    }

    /** 魂符展示模型 id（车万女仆女仆模型 id），未写入或空白视为 null。 */
    @Nullable
    public static String getSlabModelId(ItemStack stack) {
        // 1.21.1 无 getTagElement：改为从 CUSTOM_DATA 组件读取标记数据块
        CompoundTag data = getFoxTag(stack);
        if (data == null || !data.contains(SLAB_MODEL_ID)) {
            return null;
        }
        String value = data.getString(SLAB_MODEL_ID);
        return value.isBlank() ? null : value;
    }

    /** 收容时的位置，作为收容者不在线时放出/逃脱的兜底传送目标。 */
    @Nullable
    public static ResourceKey<Level> getCaptureDimension(ItemStack stack) {
        // 1.21.1 无 getTagElement：改为从 CUSTOM_DATA 组件读取标记数据块
        CompoundTag data = getFoxTag(stack);
        if (data == null) {
            return null;
        }
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(data.getString(CAPTURE_DIM)));
    }

    @Nullable
    public static BlockPos getCapturePos(ItemStack stack) {
        // 1.21.1 无 getTagElement：改为从 CUSTOM_DATA 组件读取标记数据块
        CompoundTag data = getFoxTag(stack);
        if (data == null) {
            return null;
        }
        return new BlockPos(data.getInt(CAPTURE_X), data.getInt(CAPTURE_Y), data.getInt(CAPTURE_Z));
    }
}
