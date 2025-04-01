package com.mazeymoos.cpc.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.packet.s2c.play.PositionFlag;

import java.util.EnumSet;
import java.util.Set;

public class PlayerRefresher {

    /**
     * Refresh a player's skin without requiring them to relog
     * @param player The server player entity to refresh
     */
    public static void refreshPlayer(ServerPlayerEntity player) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) {
                System.err.println("[CPC] ⚠ Cannot refresh player: Server is null");
                return;
            }

            PlayerManager playerManager = server.getPlayerManager();

            // Store current player state
            Vec3d pos = player.getPos();
            float yaw = player.getYaw();
            float pitch = player.getPitch();
            ServerWorld world = (ServerWorld)player.getWorld();

            // First apply a brief invisibility to help with visual transition
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20, 0, false, false));

            try {
                // Using 1.21.4 compatible packet approach

                // Get the player's entity ID
                int playerId = player.getId();

                // Create a destroy packet for the player entity
                EntitiesDestroyS2CPacket destroyPacket = new EntitiesDestroyS2CPacket(playerId);

                // Send the destroy packet to all players except self
                for (ServerPlayerEntity otherPlayer : playerManager.getPlayerList()) {
                    if (otherPlayer != player && otherPlayer.getWorld() == player.getWorld() &&
                            otherPlayer.distanceTo(player) < 200) {
                        otherPlayer.networkHandler.sendPacket(destroyPacket);
                    }
                }

                // Wait a bit to ensure packets are processed
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Update the player's position to ensure they don't move during refresh
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();

                // Reset velocity
                player.setVelocity(0, 0, 0);

                // Use the correct teleport method with flags
                Set<PositionFlag> flags = EnumSet.noneOf(PositionFlag.class);
                player.teleport(world, x, y, z, flags, yaw, pitch, false);

                // Force position packet to ensure client position is correct
                PlayerPosition position = new PlayerPosition(new Vec3d(x, y, z), new Vec3d(0, 0, 0), yaw, pitch);
                PlayerPositionLookS2CPacket posPacket = new PlayerPositionLookS2CPacket(playerId, position, flags);
                player.networkHandler.sendPacket(posPacket);

                // Force respawn for the player
                if (player.isDisconnected()) {
                    return;
                }

                playerManager.respawnPlayer(player, true, Entity.RemovalReason.DISCARDED);

                // Make sure the player is back in the right position
                player.setPosition(x, y, z);
                player.setYaw(yaw);
                player.setPitch(pitch);

            } catch (NoSuchMethodError | NoClassDefFoundError e) {
                // Fallback approach
                System.err.println("[CPC] ⚠ Using fallback skin refresh: " + e.getMessage());

                // Simple fallback approach
                player.requestTeleport(pos.getX(), pos.getY(), pos.getZ());
                player.setYaw(yaw);
                player.setPitch(pitch);

                // Force player update
                player.onSpawn();
            }

            // Log success
            System.out.println("[CPC] 🔁 Player skin refreshed for: " + player.getGameProfile().getName());
        } catch (Exception e) {
            System.err.println("[CPC] ⚠ Error refreshing player: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
