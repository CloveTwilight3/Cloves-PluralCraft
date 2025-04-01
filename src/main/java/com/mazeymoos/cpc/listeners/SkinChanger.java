package com.mazeymoos.cpc.listeners;

import com.mazeymoos.cpc.util.SkinUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Handles skin changing events and integration with the front system
 */
public class SkinChanger {

    /**
     * Apply a skin to a player and show feedback
     * @param player The player to apply the skin to
     * @param skinUrl The URL of the skin to apply
     */
    public static void applySkin(ServerPlayerEntity player, String skinUrl) {
        // Clean up URL
        skinUrl = skinUrl.trim();

        // Remove surrounding quotes if present
        if (skinUrl.startsWith("\"") && skinUrl.endsWith("\"")) {
            skinUrl = skinUrl.substring(1, skinUrl.length() - 1);
        }

        if (skinUrl.isEmpty()) {
            player.sendMessage(Text.literal("§d[CPC] §cNo skin URL provided!"), false);
            return;
        }

        // Validate URL format - more flexible validation
        if (!skinUrl.startsWith("http")) {
            player.sendMessage(Text.literal("§d[CPC] §cInvalid skin URL! Must be a direct link to a .png file."), false);
            return;
        }

        // Enforce .png extension if missing
        final String finalUrl = skinUrl.endsWith(".png") ? skinUrl : skinUrl + ".png";

        // Show feedback to player
        player.sendMessage(Text.literal("§d[CPC] §aApplying skin from: §7" + finalUrl), false);
        player.sendMessage(Text.literal("§d[CPC] §eThis may take a moment..."), false);

        // Try to detect if this should be a slim model
        SkinUtil.detectSlimModel(finalUrl).thenAccept(isSlim -> {
            if (isSlim) {
                player.sendMessage(Text.literal("§d[CPC] §bDetected slim skin model."), false);
            }

            // Apply the skin with detected model type
            SkinUtil.applySkinToProfile(player, finalUrl, isSlim);
        });
    }

    /**
     * Apply a skin with explicitly defined model type
     * @param player The player to apply the skin to
     * @param skinUrl The URL of the skin to apply
     * @param isSlim Whether to use the slim model
     */
    public static void applySkinWithModel(ServerPlayerEntity player, String skinUrl, boolean isSlim) {
        // Clean up URL
        skinUrl = skinUrl.trim();

        // Remove surrounding quotes if present
        if (skinUrl.startsWith("\"") && skinUrl.endsWith("\"")) {
            skinUrl = skinUrl.substring(1, skinUrl.length() - 1);
        }

        if (skinUrl.isEmpty()) {
            player.sendMessage(Text.literal("§d[CPC] §cNo skin URL provided!"), false);
            return;
        }

        // Validate URL format - more flexible validation
        if (!skinUrl.startsWith("http")) {
            player.sendMessage(Text.literal("§d[CPC] §cInvalid skin URL! Must be a direct link to a .png file."), false);
            return;
        }

        // Enforce .png extension if missing
        final String finalUrl = skinUrl.endsWith(".png") ? skinUrl : skinUrl + ".png";

        // Show feedback to player
        player.sendMessage(Text.literal("§d[CPC] §aApplying " + (isSlim ? "slim" : "classic") + " skin from: §7" + finalUrl), false);
        player.sendMessage(Text.literal("§d[CPC] §eThis may take a moment..."), false);

        // Apply the skin with explicit model type
        SkinUtil.applySkinToProfile(player, finalUrl, isSlim);
    }
}