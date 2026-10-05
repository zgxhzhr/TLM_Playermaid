package io.github.zgxhzhr.playermaid.data;

import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * 单个玩家的「人是狐」状态数据（服务端持有）。
 *
 * <p>标量字段随状态包同步给客户端；背包与饰品物品只通过容器槽位同步。
 * 数据持久化在玩家 persistentData 的 {@link Constants#ROOT_TAG} compound 中。</p>
 */
public class FoxMaidData {

    // ===== NBT 键名 =====
    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_RENDER_NAME = "RenderName";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_FAVORABILITY = "Favorability";
    private static final String TAG_SCHEDULE = "Schedule";
    private static final String TAG_INVULNERABLE = "Invulnerable";
    private static final String TAG_BAUBLES = "Baubles";
    private static final String TAG_BACKPACK = "Backpack";
    private static final String TAG_TASK_UID = "TaskUid";

    /** 默认工作模式：空闲（车万女仆默认任务）。 */
    public static final String DEFAULT_TASK_UID = "touhou_little_maid:idle";

    /** 第三方法术模组的光环饰品 id：装备后在玩家脑后渲染女仆光环。 */
    private static final ResourceLocation HALO_BAUBLE_ID =
            new ResourceLocation("touhou_little_maid_spell", "dream_cat_crystal");

    /** 人是狐开关。 */
    private boolean active;
    /** 自定义渲染名（仅头顶名牌与 Jade 标题），null 表示不覆盖。 */
    @Nullable
    private String renderName;
    /** 主人显示名（Jade 面板展示），null 表示未设置。 */
    @Nullable
    private String ownerName;
    /** 好感度点数（0-384）。 */
    private int favorability;
    /** 日程模式。 */
    private ScheduleMode schedule = ScheduleMode.DAY;
    /** 无敌展示开关（Jade 面板行）。 */
    private boolean invulnerable;
    /** 当前工作模式（车万女仆任务 uid）。 */
    private String taskUid = DEFAULT_TASK_UID;

    /** 女仆饰品栏：30 格，每格 1 个，仅接受车万女仆女仆饰品。 */
    private final ItemStackHandler baubles = createBaubleHandler(() -> dirty = true);

    /**
     * 创建女仆饰品栏处理器。服务端数据实例与客户端容器虚拟槽共用同一套校验规则，
     * 避免单人集成服下客户端点击预测直接改写真实数据。
     *
     * @param onChange 内容变更回调，可为 null
     */
    public static ItemStackHandler createBaubleHandler(@Nullable Runnable onChange) {
        return new ItemStackHandler(Constants.BAUBLE_SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // 与车万女仆女仆饰品栏一致：注册进 BaubleManager 的物品才算女仆饰品
                return !stack.isEmpty() && BaubleManager.getBauble(stack) != null;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (onChange != null) {
                    onChange.run();
                }
            }
        };
    }

    /** 女仆背包：36 格独立存储（与玩家真实背包无关），随人是狐数据持久化。 */
    private final ItemStackHandler backpack = new ItemStackHandler(Constants.BACKPACK_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
        }
    };

    /** 脏标记：内容变更后置 true，玩家保存时据此写回 NBT。 */
    private transient boolean dirty;

    // ===== 标量读写 =====

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            dirty = true;
        }
    }

    @Nullable
    public String getRenderName() {
        return renderName;
    }

    public void setRenderName(@Nullable String renderName) {
        String normalized = normalize(renderName);
        this.renderName = normalized;
        dirty = true;
    }

    @Nullable
    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(@Nullable String ownerName) {
        this.ownerName = normalize(ownerName);
        dirty = true;
    }

    public int getFavorability() {
        return favorability;
    }

    public void setFavorability(int favorability) {
        this.favorability = Math.max(0, Math.min(Constants.MAX_FAVORABILITY, favorability));
        dirty = true;
    }

    public ScheduleMode getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleMode schedule) {
        this.schedule = schedule;
        dirty = true;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
        dirty = true;
    }

    public String getTaskUid() {
        return taskUid;
    }

    public void setTaskUid(String taskUid) {
        if (!this.taskUid.equals(taskUid)) {
            this.taskUid = taskUid;
            dirty = true;
        }
    }

    /** 饰品栏中是否装备有光环饰品（用于玩家侧光环渲染同步）。 */
    public boolean hasHaloBauble() {
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && HALO_BAUBLE_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }

    public ItemStackHandler getBaubles() {
        return baubles;
    }

    public ItemStackHandler getBackpack() {
        return backpack;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 好感度等级（与车万女仆 FavorabilityManager 阈值一致）：
     * 0-63 为 0 级，64-191 为 1 级，192-383 为 2 级，384 为 3 级。
     */
    public int getFavorabilityLevel() {
        if (favorability < 64) {
            return 0;
        }
        if (favorability < 192) {
            return 1;
        }
        if (favorability < 384) {
            return 2;
        }
        return 3;
    }

    /**
     * 升到下一等级还需要的点数；已满级返回 0。
     */
    public int getNextLevelPoint() {
        int level = getFavorabilityLevel();
        return switch (level) {
            case 0 -> 64 - favorability;
            case 1 -> 192 - favorability;
            case 2 -> 384 - favorability;
            default -> 0;
        };
    }

    // ===== NBT 持久化 =====

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_ACTIVE, active);
        if (renderName != null) {
            tag.putString(TAG_RENDER_NAME, renderName);
        }
        if (ownerName != null) {
            tag.putString(TAG_OWNER_NAME, ownerName);
        }
        tag.putInt(TAG_FAVORABILITY, favorability);
        tag.putString(TAG_SCHEDULE, schedule.name());
        tag.putBoolean(TAG_INVULNERABLE, invulnerable);
        tag.putString(TAG_TASK_UID, taskUid);
        tag.put(TAG_BAUBLES, baubles.serializeNBT());
        tag.put(TAG_BACKPACK, backpack.serializeNBT());
        return tag;
    }

    public void read(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        active = tag.getBoolean(TAG_ACTIVE);
        renderName = tag.contains(TAG_RENDER_NAME) ? normalize(tag.getString(TAG_RENDER_NAME)) : null;
        ownerName = tag.contains(TAG_OWNER_NAME) ? normalize(tag.getString(TAG_OWNER_NAME)) : null;
        favorability = Math.max(0, Math.min(Constants.MAX_FAVORABILITY, tag.getInt(TAG_FAVORABILITY)));
        schedule = ScheduleMode.fromName(tag.getString(TAG_SCHEDULE));
        invulnerable = tag.getBoolean(TAG_INVULNERABLE);
        taskUid = tag.contains(TAG_TASK_UID) ? tag.getString(TAG_TASK_UID) : DEFAULT_TASK_UID;
        if (tag.contains(TAG_BAUBLES)) {
            baubles.deserializeNBT(tag.getCompound(TAG_BAUBLES));
        }
        if (tag.contains(TAG_BACKPACK)) {
            backpack.deserializeNBT(tag.getCompound(TAG_BACKPACK));
        }
        dirty = false;
    }
}
