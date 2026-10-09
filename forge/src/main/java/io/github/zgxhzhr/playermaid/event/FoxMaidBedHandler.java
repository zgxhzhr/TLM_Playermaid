package io.github.zgxhzhr.playermaid.event;

import com.github.tartaricacid.touhoulittlemaid.block.BlockMaidBed;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「人是狐」玩家右键 TLM 女仆床：取消 TLM 染色交互，转交原版床睡眠逻辑。
 *
 * <p>TLM {@code BlockMaidBed.use} 只处理染料染色，不处理睡眠；这里在
 * {@link PlayerInteractEvent.RightClickBlock} 中拦截，取消后调用原版
 * {@code Player.startSleepInBed}（配合 {@code BlockMaidBedMixin} 对
 * {@code isBed} 的放行即可入睡）。起床沿用原版机制（起床键/睡满 8 小时自动起床），
 * 无需额外处理。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoxMaidBedHandler {

    private FoxMaidBedHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        if (!(state.getBlock() instanceof BlockMaidBed)) {
            return;
        }
        // 非人是狐玩家不拦截（染色逻辑保留）
        if (!FoxMaidStatus.isActive(player)) {
            return;
        }
        // 已处于睡眠状态（如刚醒来）不重复处理
        if (player.isSleeping()) {
            return;
        }
        // 取消 TLM 染色逻辑，走原版睡眠。
        // 原版 BedBlock.use 会把入睡坐标规范到床头格，这里点床尾时同样规范，
        // 否则 sleepingPos 落在床尾格，躺姿模型会相对整张床偏移一格。
        event.setCanceled(true);
        player.startSleepInBed(normalizeToHead(event.getLevel(), pos, state));
    }

    /**
     * 把入睡坐标规范到床头格，复刻原版 {@code BedBlock#use} 的处理。
     * 女仆床尾（{@code PART} 为 {@code FOOT}）的床头位于 {@code FACING} 正方向一格。
     */
    private static BlockPos normalizeToHead(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(BlockMaidBed.PART) == BedPart.HEAD) {
            return pos;
        }
        BlockPos headPos = pos.relative(state.getValue(HorizontalDirectionalBlock.FACING));
        BlockState headState = level.getBlockState(headPos);
        if (headState.getBlock() instanceof BlockMaidBed
                && headState.getValue(BlockMaidBed.PART) == BedPart.HEAD) {
            return headPos;
        }
        return pos;
    }
}
