package io.github.zgxhzhr.playermaid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.api.FoxMaidApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 人是狐开关指令。
 *
 * <p>{@code /playermaid true|false [玩家]}：设置玩家（默认自己）的人是狐状态。
 * 无玩家参数（对自己）任意玩家均可执行 true 或 false；带玩家参数（指定他人）
 * 必须 OP 等级 ≥ 2。指令与 {@code /playermaid clear}（自救清除）同挂
 * {@code playermaid} 根节点，Brigadier 对同名字面量子树自动合并，两者互不覆盖。</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoxMaidCommand {

    private FoxMaidCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("playermaid")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        // 无玩家参数：作用于执行者自己（任意玩家可对自己执行 true/false）
                        .executes(ctx -> setActive(ctx, getSelf(ctx), getValue(ctx), true))
                        // 带玩家参数：作用于指定玩家（需 OP 等级 2）
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> setActive(ctx, EntityArgument.getPlayer(ctx, "player"), getValue(ctx), false)))));
    }

    /** 取布尔参数值（true/false）。 */
    private static boolean getValue(CommandContext<CommandSourceStack> context) {
        return BoolArgumentType.getBool(context, "value");
    }

    /** 取执行者自己；执行者不是玩家时抛异常（由命令框架转为用法错误提示）。 */
    private static ServerPlayer getSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrException();
    }

    /**
     * 设置目标玩家的人是狐状态。
     *
     * <p>权限：{@code isSelf} 为 true（无玩家参数、对自己）时任意玩家可执行 true/false；
     * {@code isSelf} 为 false（指定他人）时执行者必须为 OP 等级 ≥ 2，否则拒绝。</p>
     */
    private static int setActive(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean value, boolean isSelf) {
        CommandSourceStack source = context.getSource();
        ServerPlayer executor;
        try {
            executor = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("该指令必须由玩家执行"));
            return 0;
        }
        // 指定他人：必须 OP 等级 2；对自己无权限限制
        if (!isSelf && !source.hasPermission(2)) {
            source.sendFailure(Component.literal("权限不足：指定他人需要 OP 等级 2"));
            return 0;
        }
        // 防御：无玩家参数分支的目标必须是执行者自己
        if (isSelf && !executor.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("权限不足：不能对他人执行"));
            return 0;
        }
        FoxMaidApi.setActive(target, value);
        source.sendSuccess(() -> Component.literal("已将 " + target.getName().getString() + " 的人是狐状态设为 "
                + (value ? "开" : "关")), true);
        return 1;
    }
}
