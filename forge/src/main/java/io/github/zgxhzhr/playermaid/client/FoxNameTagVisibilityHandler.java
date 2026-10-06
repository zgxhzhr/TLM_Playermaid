package io.github.zgxhzhr.playermaid.client;

import com.mojang.logging.LogUtils;
import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 人是狐玩家的头顶名牌显隐：仅在准星对准时显示，与车万女仆女仆名牌的表现一致。
 *
 * <p>1.20.1 中玩家名牌的绘制入口是 {@code EntityRenderer#render}，其方法体为
 * （Forge 补丁后的实际形态，已由字节码实证）：</p>
 *
 * <pre>
 * RenderNameTagEvent event = new RenderNameTagEvent(entity, entity.getDisplayName(), this, poseStack, buffer, packedLight, partialTicks);
 * MinecraftForge.EVENT_BUS.post(event);
 * if (event.getResult() != Event.Result.DENY) {
 *     if (event.getResult() == Event.Result.ALLOW || this.shouldShowName(entity)) {
 *         this.renderNameTag(entity, event.getContent(), poseStack, buffer, packedLight);
 *     }
 * }
 * </pre>
 *
 * <p>因此本方法是最可靠、对渲染方式不敏感的拦截点：YSM 的玩家渲染器、车万女仆的
 * Geo 替换渲染器都会重写 {@code shouldShowName} 并绕过父类实现，但它们最终仍经由此处
 * 投递名牌事件；反之，任何在 {@code renderNameTag} 上做的取消注入都拦不住第三方模组
 * （例如 NamePain）在事件里自行绘制的名牌。真伪判定只放在事件层，注入点差异一律不影响。</p>
 *
 * <p>优先级取 {@link EventPriority#HIGHEST}：名牌事件上还有第三方模组（NamePain）会
 * 在中途 <b>自行绘制</b>名牌并以 {@code setResult(DENY)} 收尾，其绘制在事件回调内完成，
 * 事后再设 {@code DENY} 无法抹去已写入缓冲的几何。必须抢在它之前设 {@code DENY}
 * （它开头的 {@code if (event.getResult() == DENY) return;} 会让它直接放弃绘制）。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FoxNameTagVisibilityHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private FoxNameTagVisibilityHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getResult() == Event.Result.DENY) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!ClientFoxState.isActive(player)) {
            return;
        }
        boolean targeted = Minecraft.getInstance().crosshairPickEntity == player;
        if (player.tickCount % 40 == 0) {
            LOGGER.info("[playermaid] 名牌判定 玩家={} 人是狐=true 准星命中={} 文本={}",
                    player.getName().getString(), targeted, event.getContent().getString());
        }
        if (!targeted) {
            event.setResult(Event.Result.DENY);
        }
    }
}
