package com.mazeymoos.cpc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import com.mazeymoos.cpc.ClovesPluralCraft;
import java.util.UUID;

public class SystemCommand {

    private static final SuggestionProvider<ServerCommandSource> ACTION_SUGGESTIONS = (context, builder) ->
            net.minecraft.command.CommandSource.suggestMatching(new String[]{"create", "rename", "remove"}, builder);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("system")
                .then(CommandManager.argument("action", StringArgumentType.word())
                        .suggests(ACTION_SUGGESTIONS)
                        .then(CommandManager.argument("name", StringArgumentType.string()).executes(context -> {
                            String action = StringArgumentType.getString(context, "action");
                            UUID playerUUID = context.getSource().getPlayer().getUuid();
                            String name = StringArgumentType.getString(context, "name");

                            switch (action) {
                                case "create":
                                    createSystem(playerUUID, name, context.getSource());
                                    break;
                                case "rename":
                                    renameSystem(playerUUID, name, context.getSource());
                                    break;
                                default:
                                    sendMessage(context.getSource(), "Invalid action!", Formatting.RED);
                                    break;
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                        .executes(context -> {
                            String action = StringArgumentType.getString(context, "action");
                            UUID playerUUID = context.getSource().getPlayer().getUuid();

                            if ("remove".equals(action)) {
                                removeSystem(playerUUID, context.getSource());
                                return Command.SINGLE_SUCCESS;
                            }

                            sendMessage(context.getSource(), "Please provide a valid action or additional arguments!", Formatting.RED);
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private static void createSystem(UUID uuid, String name, ServerCommandSource source) {
        if (ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            sendMessage(source, "You already have a system!", Formatting.RED);
            return;
        }

        ClovesPluralCraft.SystemData newSystem = new ClovesPluralCraft.SystemData(name);
        ClovesPluralCraft.systemDataMap.put(uuid, newSystem);
        ClovesPluralCraft.saveSystem(uuid);
        sendMessage(source, "System '" + name + "' created!", Formatting.GREEN);
    }

    private static void renameSystem(UUID uuid, String newName, ServerCommandSource source) {
        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            sendMessage(source, "You do not have a system!", Formatting.RED);
            return;
        }

        ClovesPluralCraft.systemDataMap.get(uuid).systemName = newName;
        ClovesPluralCraft.saveSystem(uuid);
        sendMessage(source, "System renamed to '" + newName + "'!", Formatting.GREEN);
    }

    private static void removeSystem(UUID uuid, ServerCommandSource source) {
        if (!ClovesPluralCraft.systemDataMap.containsKey(uuid)) {
            sendMessage(source, "You do not have a system to remove!", Formatting.RED);
            return;
        }

        ClovesPluralCraft.systemDataMap.remove(uuid);
        ClovesPluralCraft.saveSystem(uuid);
        sendMessage(source, "Your system has been removed!", Formatting.GREEN);
    }

    private static void sendMessage(ServerCommandSource source, String message, Formatting color) {
        Text prefix = Text.literal("[CPC] ").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE));
        Text textMessage = Text.literal(message).setStyle(Style.EMPTY.withColor(color));
        source.sendFeedback(() -> Text.empty().append(prefix).append(textMessage), false);
    }
}
