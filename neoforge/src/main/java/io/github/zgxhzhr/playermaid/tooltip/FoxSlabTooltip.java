package io.github.zgxhzhr.playermaid.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 装有「人是狐」玩家的魂符的悬停预览数据（名字 + 被收容玩家 UUID + 魂符展示模型 id），
 * 用于在客户端渲染旋转的车万女仆模型示意图。
 */
public record FoxSlabTooltip(@Nullable String playerName, @Nullable UUID playerUuid,
                            @Nullable String slabModelId) implements TooltipComponent {
}
