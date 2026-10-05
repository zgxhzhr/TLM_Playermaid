package io.github.zgxhzhr.playermaid.jade;

import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import io.github.zgxhzhr.playermaid.client.ClientFoxState;
import io.github.zgxhzhr.playermaid.client.FoxMaidStateView;
import io.github.zgxhzhr.playermaid.data.ScheduleMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 准心面板的人是狐信息行。
 *
 * <p>复用车万女仆与 Jade 的本地化键，使面板与真实女仆完全一致：
 * 主人、模式、日程表（工作/休息/睡觉，按当前时间从日程推出）、好感度等级、还需点数、无敌。
 * 仅在目标玩家处于人是狐状态时追加。</p>
 */
public class FoxMaidJadeProvider implements IEntityComponentProvider {

    public static final ResourceLocation ID = new ResourceLocation("playermaid", "fox_maid_info");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        FoxMaidStateView view = ClientFoxState.get(entity).orElse(null);
        if (view == null || !view.active()) {
            return;
        }

        // 主人（复用 Jade 自带的所有者行格式，与驯服生物一致）
        if (view.ownerName() != null && !view.ownerName().isEmpty()) {
            tooltip.add(Component.translatable("jade.owner", view.ownerName()));
        }

        // 模式：按同步的工作模式显示任务名，未同步或无效时回退空闲（与女仆面板格式一致）
        tooltip.add(Component.translatable("top.touhou_little_maid.entity_maid.task")
                .append(taskName(view.taskUid())));

        // 日程表：按当前时间从日程模式推出当前活动（工作/休息/睡觉），与真实女仆一致
        long dayTime = accessor.getLevel().getDayTime();
        String activityKey = currentActivityKey(view.schedule(), dayTime);
        tooltip.add(Component.translatable("top.touhou_little_maid.entity_maid.schedule")
                .append(Component.translatable("gui.touhou_little_maid.activity." + activityKey)));

        // 好感度等级 + 还需点数
        tooltip.add(Component.translatable("top.touhou_little_maid.entity_maid.favorability",
                view.favorabilityLevel()));
        tooltip.add(Component.translatable("top.touhou_little_maid.entity_maid.nex_favorability_point",
                view.nextLevelPoint()));

        // 无敌
        if (view.invulnerable()) {
            tooltip.add(Component.translatable("top.touhou_little_maid.entity_maid.invulnerable")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** 按任务 uid 解析显示名，无效时回退空闲任务名。 */
    private static Component taskName(String taskUid) {
        ResourceLocation location = ResourceLocation.tryParse(taskUid);
        if (location != null) {
            var task = TaskManager.findTask(location);
            if (task.isPresent()) {
                return task.get().getName();
            }
        }
        return Component.translatable("task.touhou_little_maid.idle");
    }

    /**
     * 由日程模式与当天时间推出当前活动，时间点与车万女仆 InitEntities 的
     * 白班/夜班日程完全一致：
     * 白班 0-12000 工作、12000-16000 休息、16000-24000 睡觉；
     * 夜班 0-8000 睡觉、8000-12000 休息、12000-24000 工作；全天始终工作。
     */
    private static String currentActivityKey(ScheduleMode mode, long dayTime) {
        int time = (int) (dayTime % 24000L);
        return switch (mode) {
            case ALL -> "work";
            case DAY -> time < 12000 ? "work" : (time < 16000 ? "idle" : "rest");
            case NIGHT -> time < 8000 ? "rest" : (time < 12000 ? "idle" : "work");
        };
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
