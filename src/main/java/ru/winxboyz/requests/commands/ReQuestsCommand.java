package ru.winxboyz.requests.commands;

import org.bukkit.command.CommandSender;
// import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
// import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
// import com.mojang.brigadier.builder.RequiredArgumentBuilder;
// import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
// import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
// import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import ru.winxboyz.requests.ReQuests;
import ru.winxboyz.requests.managers.CommandManager;
// import ru.winxboyz.requests.managers.DatabaseManager;
import ru.winxboyz.requests.managers.PluginManager;

public class ReQuestsCommand {

    private ReQuests plugin;
    private CommandManager commandManager;

    private final LiteralArgumentBuilder<CommandSourceStack>
        requests = Commands.literal("requests").requires(ctx -> ctx.getSender().isOp()),
        list     = Commands.literal("list"),
        reload     = Commands.literal("reload");
        // accept   = Commands.literal("accept");
    // private final RequiredArgumentBuilder<CommandSourceStack,PlayerSelectorArgumentResolver>
    //     player  = Commands.argument("player", ArgumentTypes.player());
    // private final RequiredArgumentBuilder<CommandSourceStack,String>
    //     quest = Commands.argument("quest", StringArgumentType.word()); 

    private final Command<CommandSourceStack>
    // executeAccept          = ctx -> {
    //     PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
    //     try {
    //         Player player = resolver.resolve(ctx.getSource()).getFirst();
    //         String questName = ctx.getArgument("quest", String.class);
    //         commandManager.handleAccept(player, questName);
    //     } catch(CommandSyntaxException e) {
    //         plugin.getLogger().warning(commandManager.getMessages().get("syntax-error") + " add:\n" + e.getMessage());
    //     }
    //     return Command.SINGLE_SUCCESS;
    // },
    executeList = ctx -> {
        CommandSender sender = ctx.getSource().getSender();
        commandManager.handleList(sender);
        return Command.SINGLE_SUCCESS;
    },
    executeReload = ctx -> {
        plugin.reload();
        ctx.getSource().getSender().sendMessage(Component.text("Данные конфигурации перезагружены"));
        return Command.SINGLE_SUCCESS;
    };

    

    public LiteralCommandNode<CommandSourceStack> initialize() {
        LiteralArgumentBuilder<CommandSourceStack> builder = requests
        // .then(player.then(quest.then(accept.executes(executeAccept))))
        .then(list.executes(executeList))
        .then(reload.executes(executeReload));
        return builder.build();
    }

    public ReQuestsCommand(ReQuests plugin) {
        this.plugin = plugin;
        this.commandManager = PluginManager.getInstance().getCommandManager();
    }
}
