package io.github.zgxhzhr.playermaid.compat.slab;

import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.api.FoxMaidApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 人是狐玩家的随机语音。
 *
 * <p>仿车万女仆的随机环境语音：处于人是狐状态的玩家会以较低概率周期性播放
 * 女仆空闲语音（touhou_little_maid:maid.mode.idle，语音包内多个音色随机播放）。
 * 播放走服务端 {@code level.playSound}，附近玩家都能听到，与女仆语音表现一致。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoxVoiceHandler {

    /** 每 tick 播放概率的倒数（约 150 tick 一次，与女仆随机环境声节奏相近）。 */
    private static final int VOICE_CHANCE = 150;

    private FoxVoiceHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!FoxMaidApi.isActive(player)) {
                continue;
            }
            if (player.getRandom().nextInt(VOICE_CHANCE) != 0) {
                continue;
            }
            // 中立音源（与女仆发声一致），音量 0.5，音高随机
            player.level().playSound(null, player.blockPosition(), InitSounds.MAID_IDLE.get(),
                    SoundSource.NEUTRAL, 0.5F, player.getRandom().nextFloat() * 0.1F + 0.9F);
        }
    }
}
