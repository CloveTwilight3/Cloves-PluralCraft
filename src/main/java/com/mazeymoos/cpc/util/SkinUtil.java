package com.mazeymoos.cpc.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkinUtil {
    // Thread pool for async skin operations
    private static final ExecutorService SKIN_EXECUTOR = Executors.newFixedThreadPool(2);

    // Cache for skin data - persist between player sessions
    private static final Map<UUID, SkinData> PLAYER_SKIN_CACHE = new HashMap<>();

    /**
     * Stores skin data for persistence
     */
    public static class SkinData {
        public final String value;
        public final String signature;
        public final String url;
        public final boolean slim;

        public SkinData(String value, String signature, String url, boolean slim) {
            this.value = value;
            this.signature = signature;
            this.url = url;
            this.slim = slim;
        }
    }

    /**
     * Apply a skin to a player's profile
     * @param player The server player entity
     * @param skinUrl URL to the skin (must be .png)
     */
    public static void applySkinToProfile(ServerPlayerEntity player, String skinUrl) {
        applySkinToProfile(player, skinUrl, false);
    }

    /**
     * Apply a skin to a player's profile with model type
     * @param player The server player entity
     * @param skinUrl URL to the skin (must be .png)
     * @param isSlimModel Whether to use the slim model
     */
    public static void applySkinToProfile(final ServerPlayerEntity player, final String skinUrl, final boolean isSlimModel) {
        if (player == null || player.getServer() == null) {
            System.err.println("[CPC] ⚠ Cannot apply skin: Player or server is null");
            return;
        }

        final GameProfile profile = player.getGameProfile();

        // Clean up URL if needed
        final String cleanedUrl = cleanUrl(skinUrl);

        if (!cleanedUrl.endsWith(".png")) {
            System.out.println("[CPC] ⚠ Unsupported skin format. Only .png links are allowed: " + cleanedUrl);
            return;
        }

        // Apply a brief invisibility effect to help with visual update
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, false, false));

        // Log the upload attempt
        System.out.println("[CPC] Uploading to Mineskin: " + cleanedUrl);

        // Do the skin upload asynchronously to avoid blocking the main thread
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://api.mineskin.org/generate/url");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                // Add model option and set visibility to public
                String modelVariant = isSlimModel ? "slim" : "classic";
                String jsonPayload = "{\"url\":\"" + cleanedUrl + "\",\"name\":\"player_skin\",\"variant\":\"" + modelVariant + "\",\"visibility\":1}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() != 200) {
                    System.err.println("[CPC] ❌ Failed to upload to Mineskin. Code: " + conn.getResponseCode());
                    return;
                }

                JsonObject json = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                JsonObject data = json.getAsJsonObject("data");
                JsonObject texture = data.getAsJsonObject("texture");

                final String value = texture.get("value").getAsString();
                final String signature = texture.get("signature").getAsString();

                // Cache the skin data for persistence
                PLAYER_SKIN_CACHE.put(player.getUuid(), new SkinData(value, signature, cleanedUrl, isSlimModel));

                // Run texture application on main thread
                player.getServer().execute(() -> {
                    try {
                        // Properly remove all existing texture properties before adding new one
                        profile.getProperties().removeAll("textures");
                        profile.getProperties().put("textures", new Property("textures", value, signature));

                        // Save to player NBT for persistence across sessions
                        saveSkinToPlayerData(player, value, signature, cleanedUrl, isSlimModel);

                        System.out.println("[CPC] ✅ Applied signed skin from Mineskin to " + profile.getName());

                        // Apply the refresh with a small delay to ensure the texture is processed
                        if (player.getServer() != null) {
                            final MinecraftServer server = player.getServer();
                            server.execute(() -> {
                                try {
                                    Thread.sleep(200); // Increased delay to ensure property application
                                    PlayerRefresher.refreshPlayer(player);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("[CPC] ⚠ Error applying skin properties: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("[CPC] ⚠ Failed to apply skin from URL: " + cleanedUrl);
                e.printStackTrace();
            }
        }, SKIN_EXECUTOR);
    }

    /**
     * Clean up URL string by removing quotes and extra whitespace
     */
    private static String cleanUrl(String url) {
        if (url == null) return null;

        // Remove quotes if present
        url = url.trim();
        if (url.startsWith("\"") && url.endsWith("\"")) {
            url = url.substring(1, url.length() - 1);
        }

        return url;
    }

    /**
     * Save skin data to player NBT for persistence
     */
    private static void saveSkinToPlayerData(ServerPlayerEntity player, String value, String signature, String url, boolean isSlim) {
        try {
            // Get the player's persistent NBT data
            NbtCompound persistentData = player.writeNbt(new NbtCompound());

            // Create or get CPC compound
            NbtCompound cpcData;
            if (persistentData.contains("cpc")) {
                cpcData = persistentData.getCompound("cpc");
            } else {
                cpcData = new NbtCompound();
            }

            // Store skin data
            cpcData.putString("skinValue", value);
            cpcData.putString("skinSignature", signature);
            cpcData.putString("skinUrl", url);
            cpcData.putBoolean("skinSlim", isSlim);

            // Save back to player data
            persistentData.put("cpc", cpcData);

            // Mark player data as dirty so it gets saved
            player.readNbt(persistentData);

            System.out.println("[CPC] 💾 Saved skin data to player NBT");
        } catch (Exception e) {
            System.err.println("[CPC] ⚠ Failed to save skin data to player NBT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load and apply skin from player data on join
     */
    public static void loadAndApplySkinFromPlayerData(final ServerPlayerEntity player) {
        try {
            // Get the player's persistent NBT data
            NbtCompound persistentData = player.writeNbt(new NbtCompound());

            if (persistentData != null && persistentData.contains("cpc")) {
                NbtCompound cpcData = persistentData.getCompound("cpc");

                if (cpcData.contains("skinValue") && cpcData.contains("skinSignature")) {
                    final String value = cpcData.getString("skinValue");
                    final String signature = cpcData.getString("skinSignature");
                    final String url = cpcData.getString("skinUrl");
                    final boolean isSlim = cpcData.getBoolean("skinSlim");

                    // Apply saved skin
                    GameProfile profile = player.getGameProfile();
                    profile.getProperties().removeAll("textures");
                    profile.getProperties().put("textures", new Property("textures", value, signature));

                    // Cache the skin data
                    PLAYER_SKIN_CACHE.put(player.getUuid(), new SkinData(value, signature, url, isSlim));

                    // Schedule a refresh after player fully joins
                    if (player.getServer() != null) {
                        final MinecraftServer server = player.getServer();
                        server.execute(() -> {
                            try {
                                Thread.sleep(1000); // Wait for player to fully load
                                PlayerRefresher.refreshPlayer(player);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    }

                    System.out.println("[CPC] 🔄 Restored saved skin for " + player.getName().getString());
                }
            }
        } catch (Exception e) {
            System.err.println("[CPC] ⚠ Failed to load skin data from player NBT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Auto-detect if a skin should use the slim model
     * @param skinUrl URL to analyze
     * @return CompletableFuture that completes with true if slim, false otherwise
     */
    public static CompletableFuture<Boolean> detectSlimModel(final String skinUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Try to download the image to analyze pixel data
                URL url = new URL(skinUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Check for filename hints first (common naming conventions)
                String path = url.getPath().toLowerCase();
                if (path.contains("slim") || path.contains("alex")) {
                    return true;
                }

                // Default to classic model if we can't determine
                return false;
            } catch (Exception e) {
                System.err.println("[CPC] ⚠ Error detecting slim model: " + e.getMessage());
                return false; // Default to classic model on error
            }
        }, SKIN_EXECUTOR);
    }

    /**
     * Shutdown the executor service when the server stops
     */
    public static void shutdown() {
        if (!SKIN_EXECUTOR.isShutdown()) {
            SKIN_EXECUTOR.shutdown();
            System.out.println("[CPC] 🛑 Shut down skin executor service");
        }
    }
}