package com.mazeymoos.cpc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import com.mazeymoos.cpc.ClovesPluralCraft;
import java.util.UUID;

public class SystemCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("system")
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(context -> {
                                    UUID playerUUID = context.getSource().getPlayer().getUuid();
                                    String systemName = StringArgumentType.getString(context, "name");
                                    createSystem(playerUUID, systemName, context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("newName", StringArgumentType.string())
                                .executes(context -> {
                                    UUID playerUUID = context.getSource().getPlayer().getUuid();
                                    String newName = StringArgumentType.getString(context, "newName");
                                    renameSystem(playerUUID, newName, context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(CommandManager.literal("remove")
                        .executes(context -> {
                            UUID playerUUID = context.getSource().getPlayer().getUuid();
                            removeSystem(playerUUID, context.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
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
