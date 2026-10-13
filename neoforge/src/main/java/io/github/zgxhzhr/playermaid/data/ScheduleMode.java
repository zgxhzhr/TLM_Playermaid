package io.github.zgxhzhr.playermaid.data;

import java.util.Locale;

/**
 * 女仆日程模式（与车万女仆 {@code MaidSchedule} 的 DAY/NIGHT/ALL 一一对应）。
 */
public enum ScheduleMode {
    /** 白天模式。 */
    DAY,
    /** 夜晚模式。 */
    NIGHT,
    /** 全天模式。 */
    ALL;

    /**
     * 按名字解析日程，非法值回退到 {@link #DAY}。
     */
    public static ScheduleMode fromName(String name) {
        if (name == null) {
            return DAY;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DAY;
        }
    }

    /**
     * 切换到下一个模式（白天 → 夜晚 → 全天 → 白天）。
     */
    public ScheduleMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /**
     * 车万女仆本地化键后缀（{@code gui.touhou_little_maid.activity.day/night/all}）。
     */
    public String activityKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
