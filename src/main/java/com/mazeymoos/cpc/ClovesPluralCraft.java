package com.mazeymoos.cpc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mazeymoos.cpc.commands.SystemCommand;
import com.mazeymoos.cpc.commands.FrontCommand;
import com.mazeymoos.cpc.commands.ChatProxyListener;
import com.mazeymoos.cpc.listeners.SkinChanger;

import static net.minecraft.server.command.CommandManager.literal;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class ClovesPluralCraft implements ModInitializer {
	public static final String MOD_ID = "cloves-pluralcraft";
	public static final String MOD_NAME = "Clove's PluralCraft";
	private static final Path CONFIG_DIR = Paths.get("config/cpc");
	public static final Path SKINS_DIR = Paths.get("config/cpc/skins");
	private static final Path SYSTEMS_DIR = Paths.get("config/cpc/systems");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final Map<UUID, SystemData> systemDataMap = new HashMap<>();

	@Override
	public void onInitialize() {
		System.out.println("[CPC] " + MOD_NAME + " Loaded!");
		loadAllSystems();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			SystemCommand.register(dispatcher);
			FrontCommand.register(dispatcher);

			// Aliases registration
			dispatcher.register(literal("sys").redirect(dispatcher.getRoot().getChild("system")));
			dispatcher.register(literal("f").redirect(dispatcher.getRoot().getChild("front")));
			dispatcher.register(literal("member").redirect(dispatcher.getRoot().getChild("front")));
			dispatcher.register(literal("proxy").redirect(dispatcher.getRoot().getChild("front")));
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveAllSystems());
		ChatProxyListener.register();
	}

	public static void loadAllSystems() {
		try {
			Files.createDirectories(CONFIG_DIR);
			Files.createDirectories(SKINS_DIR);
			Files.createDirectories(SYSTEMS_DIR);

			Files.list(SYSTEMS_DIR).forEach(path -> {
				try {
					if (!path.toString().endsWith(".json")) return;
					UUID uuid = UUID.fromString(path.getFileName().toString().replace(".json", ""));
					SystemData data = GSON.fromJson(Files.readString(path), SystemData.class);
					systemDataMap.put(uuid, data);
				} catch (Exception e) {
					System.err.println("[CPC] Failed to load system file: " + path);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveAllSystems() {
		systemDataMap.forEach((uuid, system) -> saveSystem(uuid));
	}

	public static void saveSystem(UUID uuid) {
		try {
			Path filePath = SYSTEMS_DIR.resolve(uuid.toString() + ".json");
			Files.writeString(filePath, GSON.toJson(systemDataMap.get(uuid)));
		} catch (Exception e) {
			System.err.println("[CPC] Failed to save system file: " + uuid);
		}
	}

	public static Text formatMessage(String message, int color) {
		return Text.literal("[CPC] ").styled(style -> style.withColor(TextColor.fromRgb(0xFF55FF)))
				.append(Text.literal(message).styled(style -> style.withColor(TextColor.fromRgb(color))));
	}

	public static class SystemData {
		public String systemName;
		public Map<String, Boolean> fronts = new HashMap<>();
		public String activeFront = "";
		public Map<String, String> frontSkins = new HashMap<>();

		public SystemData(String systemName) {
			this.systemName = systemName;
		}
	}
}
