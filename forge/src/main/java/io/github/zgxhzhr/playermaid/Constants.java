package io.github.zgxhzhr.playermaid;

/**
 * 「人是狐」模组常量。
 */
public final class Constants {

    /** 模组 id（与 mods.toml、gradle.properties 保持一致）。 */
    public static final String MOD_ID = "playermaid";

    /** 玩家 persistentData 中存放人是狐状态的根 compound 键名。 */
    public static final String ROOT_TAG = "PlayerMaidData";

    /** 好感度点数上限（与车万女仆 FavorabilityManager 的 384 一致）。 */
    public static final int MAX_FAVORABILITY = 384;

    /** 女仆饰品栏槽位总数（5 列 × 6 排）。 */
    public static final int BAUBLE_SLOT_COUNT = 30;

    /** 女仆背包槽位总数（默认 6 格 + 扩展 5 排 × 6 列）。 */
    public static final int BACKPACK_SLOT_COUNT = 36;

    private Constants() {
    }
}
