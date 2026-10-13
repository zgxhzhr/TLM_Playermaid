package io.github.zgxhzhr.playermaid.command;

import net.neoforged.fml.common.EventBusSubscriber;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidSettingsService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * 「人是狐设置」界面指令。
 *
 * <p>{@code /playermaidgui} 打开自己的设置界面：任意玩家可用，无需 OP，
 * 因此不安装调试器（superdbg）也能自行开启并配置人是狐。</p>
 *
 * <p>{@code /playermaidgui <玩家>} 打开指定玩家的设置界面（帮别人配置，含认主）：
 * 需要 OP 等级 ≥ 2，且该参数在命令树里对无权限者不可见。</p>
 */
@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class FoxMaidGuiCommand {

    private FoxMaidGuiCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("playermaidgui")
                // 无参数：打开自己的设置界面，任意玩家可用
                .executes(FoxMaidGuiCommand::openSelf)
                // 带玩家参数：帮他人配置，需 OP 等级 ≥ 2（运行时校验，便于给出明确提示）
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(FoxMaidGuiCommand::openOther)));
    }

    /** 打开自己的设置界面。 */
    private static int openSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer self = context.getSource().getPlayerOrException();
        return FoxMaidSettingsService.open(self, self) ? 1 : 0;
    }

    /** 打开指定玩家的设置界面（OP ≥ 2）。 */
    private static int openOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor;
        try {
            executor = context.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("该指令必须由玩家执行"));
            return 0;
        }
        if (!context.getSource().hasPermission(2)) {
            context.getSource().sendFailure(Component.literal("权限不足：配置其他玩家需要 OP 等级 2"));
            return 0;
        }
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return FoxMaidSettingsService.open(executor, target) ? 1 : 0;
    }
}
