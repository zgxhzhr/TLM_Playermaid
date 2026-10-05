package io.github.zgxhzhr.playermaid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.zgxhzhr.playermaid.api.FoxMaidApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 人是狐指令。
 *
 * <p>{@code /playermaid clear}：强制解除自己的人（玩家）是狐状态。
 * 无需 OP 权限，供玩家在状态异常时自救；背包与饰品数据保留，重新开启后仍在。</p>
 */
public final class FoxMaidCommands {

    private FoxMaidCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("playermaid")
                .then(Commands.literal("clear")
                        .executes(FoxMaidCommands::clearSelf)));
    }

    private static int clearSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.playermaid.player_only"));
            return 0;
        }
        if (!FoxMaidApi.isActive(player)) {
            source.sendFailure(Component.translatable("command.playermaid.clear.not_active"));
            return 0;
        }
        FoxMaidApi.setActive(player, false);
        source.sendSuccess(() -> Component.translatable("command.playermaid.clear.success"), false);
        return 1;
    }
}
