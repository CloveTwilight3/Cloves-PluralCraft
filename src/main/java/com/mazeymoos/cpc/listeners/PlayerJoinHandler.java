package com.mazeymoos.cpc.listeners;

import com.mazeymoos.cpc.util.SkinUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Handles player join events to restore skin data
 */
public class PlayerJoinHandler {

    /**
     * Register event handlers
     */
    public static void registerEvents() {
        // Player join event - restore skin data
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Schedule skin restoration with a delay to ensure player is fully loaded
            server.execute(() -> {
                try {
                    // Wait a bit to ensure player is properly initialized
                    Thread.sleep(1000);

                    // Load and apply saved skin data
                    SkinUtil.loadAndApplySkinFromPlayerData(player);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        // Server shutdown - clean up resources
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // Ensure executor service is shut down properly
            try {
                // Use the new shutdown method in SkinUtil
                SkinUtil.shutdown();
            } catch (Exception e) {
                System.err.println("[CPC] ⚠ Error shutting down skin executor: " + e.getMessage());
            }
        });
    }
}