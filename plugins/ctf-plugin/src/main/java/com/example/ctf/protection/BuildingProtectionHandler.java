package com.example.ctf.protection;

import com.example.ctf.CTFPlugin;
import com.example.ctf.arena.ArenaManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Handles building protection in CTF arenas.
 * Prevents block placement and breaking in protected regions (flag rooms, etc).
 */
public class BuildingProtectionHandler {

    private final CTFPlugin plugin;

    public BuildingProtectionHandler(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
        registerEvents();
    }

    private void registerEvents() {
        // Listen for block placement
        plugin.getEventRegistry().register(
            PlaceBlockEvent.class,
            this::onBlockPlace
        );

        // Listen for block breaking
        plugin.getEventRegistry().register(
            BreakBlockEvent.class,
            this::onBlockBreak
        );

        plugin.getLogger().atInfo().log("BuildingProtectionHandler: Event listeners registered");
    }

    /**
     * Called when a player tries to place a block.
     */
    private void onBlockPlace(@Nonnull PlaceBlockEvent event) {
        ArenaManager arenaManager = plugin.getArenaManager();
        if (arenaManager == null) {
            return;
        }

        Vector3i blockPos = event.getTargetBlock();

        if (arenaManager.isBlockProtected(blockPos)) {
            event.setCancelled(true);
            notifyPlayer(event.getEntityRef(), "This area is protected!");
        }
    }

    /**
     * Called when a player tries to break a block.
     */
    private void onBlockBreak(@Nonnull BreakBlockEvent event) {
        ArenaManager arenaManager = plugin.getArenaManager();
        if (arenaManager == null) {
            return;
        }

        Vector3i blockPos = event.getTargetBlock();

        if (arenaManager.isBlockProtected(blockPos)) {
            event.setCancelled(true);
            notifyPlayer(event.getEntityRef(), "This area is protected!");
        }
    }

    /**
     * Sends a message to the player who triggered the event.
     */
    private void notifyPlayer(Ref<EntityStore> entityRef, String message) {
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player player = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef != null) {
            playerRef.sendChatMessage(Message.raw("[CTF] " + message));
        }
    }
}
