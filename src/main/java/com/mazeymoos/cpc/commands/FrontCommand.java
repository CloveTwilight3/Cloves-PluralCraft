package com.mazeymoos.cpc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.mazeymoos.cpc.ClovesPluralCraft;
import java.util.UUID;

public class FrontCommand {

    private static final SuggestionProvider<ServerCommandSource> MEMBER_SUGGESTIONS = (context, builder) -> {
        UUID playerUUID = context.getSource().getPlayer().getUuid();
        if (ClovesPluralCraft.systemDataMap.containsKey(playerUUID)) {
            return CommandSource.suggestMatching(
                    ClovesPluralCraft.systemDataMap.get(playerUUID).fronts.keySet(), builder);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("front")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(ctx -> handleFront(ctx.getSource(), "add", StringArgumentType.getString(ctx, "name")))
                        )
                )
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.string()).suggests(MEMBER_SUGGESTIONS)
                                .executes(ctx -> handleFront(ctx.getSource(), "delete", StringArgumentType.getString(ctx, "name")))
                        )
                )
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("name", StringArgumentType.string()).suggests(MEMBER_SUGGESTIONS)
                                .executes(ctx -> handleFront(ctx.getSource(), "set", StringArgumentType.getString(ctx, "name")))
                        )
                )
                .then(CommandManager.literal("list")
                        .executes(ctx -> listMembers(ctx.getSource()))
                )
                .then(CommandManager.literal("clear")
                        .executes(ctx -> handleFront(ctx.getSource(), "clear", ""))
                )
        );
    }

    private static int handleFront(ServerCommandSource source, String action, String name) {
        UUID uuid = source.getPlayer().getUuid();

        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("§d[CPC] §cYou do not have a system!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ClovesPluralCraft.SystemData data = ClovesPluralCraft.systemDataMap.get(uuid);

        switch (action) {
            case "add":
                data.fronts.put(name, true);
                source.sendFeedback(() -> Text.literal("§d[CPC] §aFront '" + name + "' added!"), false);
                break;

            case "delete":
                if (data.fronts.remove(name) != null) {
                    source.sendFeedback(() -> Text.literal("§d[CPC] §aFront '" + name + "' deleted!"), false);
                } else {
                    source.sendFeedback(() -> Text.literal("§d[CPC] §cFront '" + name + "' does not exist!"), false);
                }
                break;

            case "set":
                if (data.fronts.containsKey(name)) {
                    data.activeFront = name;
                    source.sendFeedback(() -> Text.literal("§d[CPC] §aNow fronting as '" + name + "'!"), false);
                } else {
                    source.sendFeedback(() -> Text.literal("§d[CPC] §cFront '" + name + "' does not exist!"), false);
                }
                break;

            case "clear":
                data.activeFront = "";
                source.sendFeedback(() -> Text.literal("§d[CPC] §aFront cleared!"), false);
                break;
        }

        ClovesPluralCraft.saveSystem(uuid);
        return Command.SINGLE_SUCCESS;
    }

    private static int listMembers(ServerCommandSource source) {
        UUID uuid = source.getPlayer().getUuid();

        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("§d[CPC] §cYou do not have a system!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ClovesPluralCraft.SystemData data = ClovesPluralCraft.systemDataMap.get(uuid);
        String members = String.join(", ", data.fronts.keySet());

        source.sendFeedback(() -> Text.literal("§d[CPC] §aSystem members: §f" + members), false);
        return Command.SINGLE_SUCCESS;
    }
}
