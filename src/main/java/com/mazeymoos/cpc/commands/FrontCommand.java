package com.mazeymoos.cpc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.mazeymoos.cpc.ClovesPluralCraft;
import java.util.UUID;

public class FrontCommand {
    private static final SuggestionProvider<ServerCommandSource> FRONT_SUGGESTIONS = (context, builder) -> {
        UUID playerUUID = context.getSource().getPlayer().getUuid();
        if (ClovesPluralCraft.systemDataMap.containsKey(playerUUID)) {
            return net.minecraft.command.CommandSource.suggestMatching(
                    ClovesPluralCraft.systemDataMap.get(playerUUID).fronts.keySet(), builder);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("front")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(context -> {
                                    UUID playerUUID = context.getSource().getPlayer().getUuid();
                                    String frontName = StringArgumentType.getString(context, "name");
                                    addFront(playerUUID, frontName, context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.string()).suggests(FRONT_SUGGESTIONS)
                                .executes(context -> {
                                    UUID playerUUID = context.getSource().getPlayer().getUuid();
                                    String frontName = StringArgumentType.getString(context, "name");
                                    deleteFront(playerUUID, frontName, context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("name", StringArgumentType.string()).suggests(FRONT_SUGGESTIONS)
                                .executes(context -> {
                                    UUID playerUUID = context.getSource().getPlayer().getUuid();
                                    String frontName = StringArgumentType.getString(context, "name");
                                    setFront(playerUUID, frontName, context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(CommandManager.literal("clear")
                        .executes(context -> {
                            UUID playerUUID = context.getSource().getPlayer().getUuid();
                            clearFront(playerUUID, context.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
    }

    private static void addFront(UUID uuid, String frontName, ServerCommandSource source) {
        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cYou do not have a system!"), false);
            return;
        }

        ClovesPluralCraft.systemDataMap.get(uuid).fronts.put(frontName, true);
        ClovesPluralCraft.saveSystem(uuid);
        source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aFront '" + frontName + "' added!"), false);
    }

    private static void deleteFront(UUID uuid, String frontName, ServerCommandSource source) {
        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cYou do not have a system!"), false);
            return;
        }

        if (!ClovesPluralCraft.systemDataMap.get(uuid).fronts.containsKey(frontName)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cFront '" + frontName + "' does not exist!"), false);
            return;
        }

        ClovesPluralCraft.systemDataMap.get(uuid).fronts.remove(frontName);
        ClovesPluralCraft.saveSystem(uuid);
        source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aFront '" + frontName + "' deleted!"), false);
    }

    private static void setFront(UUID uuid, String frontName, ServerCommandSource source) {
        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cYou do not have a system!"), false);
            return;
        }

        if (!ClovesPluralCraft.systemDataMap.get(uuid).fronts.containsKey(frontName)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cFront '" + frontName + "' does not exist!"), false);
            return;
        }

        ClovesPluralCraft.systemDataMap.get(uuid).activeFront = frontName;
        ClovesPluralCraft.saveSystem(uuid);
        source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aNow fronting as '" + frontName + "'!"), false);
    }

    private static void clearFront(UUID uuid, ServerCommandSource source) {
        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7cYou do not have a system!"), false);
            return;
        }

        ClovesPluralCraft.systemDataMap.get(uuid).activeFront = "";
        ClovesPluralCraft.saveSystem(uuid);
        source.sendFeedback(() -> Text.literal("\u00a7d[CPC] \u00a7aFront cleared!"), false);
    }
}
