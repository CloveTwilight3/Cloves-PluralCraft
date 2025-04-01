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
import com.mazeymoos.cpc.listeners.SkinChanger;

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
                .then(CommandManager.literal("skin")
                        .then(CommandManager.argument("name", StringArgumentType.string()).suggests(MEMBER_SUGGESTIONS)
                                .then(CommandManager.argument("url", StringArgumentType.string())
                                        .executes(ctx -> setSkin(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "url")
                                        ))
                                )
                        )
                )
        );
    }

    private static int handleFront(ServerCommandSource source, String action, String name) {
        UUID uuid = source.getPlayer().getUuid();

        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cYou do not have a system!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ClovesPluralCraft.SystemData data = ClovesPluralCraft.systemDataMap.get(uuid);

        switch (action) {
            case "add":
                data.fronts.put(name, true);
                source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aFront '" + name + "' added!"), false);
                break;

            case "delete":
                if (data.fronts.remove(name) != null) {
                    source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aFront '" + name + "' deleted!"), false);
                } else {
                    source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cFront '" + name + "' does not exist!"), false);
                }
                break;

            case "set":
                if (data.fronts.containsKey(name)) {
                    data.activeFront = name;
                    source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aNow fronting as '" + name + "'!"), false);
                    String skin = data.frontSkins.getOrDefault(name, "");
                    if (!skin.isEmpty()) {
                        SkinChanger.applySkin(source.getPlayer(), skin);
                    }
                } else {
                    source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cFront '" + name + "' does not exist!"), false);
                }
                break;

            case "clear":
                data.activeFront = "";
                source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aFront cleared!"), false);
                break;
        }

        ClovesPluralCraft.saveSystem(uuid);
        return Command.SINGLE_SUCCESS;
    }

    private static int setSkin(ServerCommandSource source, String frontName, String skinUrl) {
        UUID uuid = source.getPlayer().getUuid();

        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cYou do not have a system!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ClovesPluralCraft.SystemData data = ClovesPluralCraft.systemDataMap.get(uuid);

        // Check if the front exists
        if (!data.fronts.containsKey(frontName)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cFront '" + frontName + "' does not exist!"), false);
            return Command.SINGLE_SUCCESS;
        }

        // Save the skin URL for this front
        data.frontSkins.put(frontName, skinUrl);
        ClovesPluralCraft.saveSystem(uuid);

        // Only apply the skin if the specified front is the currently active front
        if (frontName.equals(data.activeFront)) {
            SkinChanger.applySkin(source.getPlayer(), skinUrl);
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aSkin for '" + frontName + "' updated and applied!"), false);
        } else {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aSkin for '" + frontName + "' saved! It will be applied when you switch to this front."), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int listMembers(ServerCommandSource source) {
        UUID uuid = source.getPlayer().getUuid();

        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cYou do not have a system!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ClovesPluralCraft.SystemData data = ClovesPluralCraft.systemDataMap.get(uuid);
        String members = String.join(", ", data.fronts.keySet());

        source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aSystem members: \u00a7f" + members), false);
        return Command.SINGLE_SUCCESS;
    }
}