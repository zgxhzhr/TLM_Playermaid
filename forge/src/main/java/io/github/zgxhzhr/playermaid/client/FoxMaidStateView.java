package io.github.zgxhzhr.playermaid.client;

import io.github.zgxhzhr.playermaid.data.ScheduleMode;

import javax.annotation.Nullable;

/**
 * 客户端持有的玩家人是狐状态只读视图（由服务端同步）。
 *
 * @param entityId      玩家实体 id
 * @param active        是否处于人是狐状态
 * @param renderName    自定义渲染名（null 表示无）
 * @param ownerName     主人显示名（null 表示未设置）
 * @param favorability  好感度点数 0-384
 * @param schedule      日程模式
 * @param invulnerable  无敌展示开关
 * @param taskUid       当前工作模式（车万女仆任务 uid）
 * @param hasHalo       是否装备光环饰品（玩家侧光环渲染开关）
 */
public record FoxMaidStateView(int entityId, boolean active, @Nullable String renderName,
                               @Nullable String ownerName, int favorability,
                               ScheduleMode schedule, boolean invulnerable,
                               String taskUid, boolean hasHalo) {

    /**
     * 好感度等级（阈值与车万女仆一致）。
     */
    public int favorabilityLevel() {
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
     * 距下一等级所需点数，满级返回 0。
     */
    public int nextLevelPoint() {
        return switch (favorabilityLevel()) {
            case 0 -> 64 - favorability;
            case 1 -> 192 - favorability;
            case 2 -> 384 - favorability;
            default -> 0;
        };
    }
}
