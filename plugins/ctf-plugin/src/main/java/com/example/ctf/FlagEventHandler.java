package com.example.ctf;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.TransformComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Handles CTF flag-related events:
 * - Drop item requests (G key) to drop the flag
 *
 * Note: Death detection is handled via FlagCarrierManager's tick system
 * which checks for dead carriers using the DeathComponent.
 */
public class FlagEventHandler {

    private final CTFPlugin plugin;

    public FlagEventHandler(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
        registerEvents();
    }

    private void registerEvents() {
        // Listen for drop item requests (G key)
        plugin.getEventRegistry().register(
            DropItemEvent.PlayerRequest.class,
            this::onDropItemRequest
        );

        plugin.getLogger().atInfo().log("FlagEventHandler: Event listeners registered");
    }

    /**
     * Called when a player presses G to drop an item.
     * If they're dropping the flag item, we handle it specially.
     */
    private void onDropItemRequest(@Nonnull DropItemEvent.PlayerRequest event) {
        // Get the player from the event context
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player player = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        FlagCarrierManager flagManager = plugin.getFlagCarrierManager();

        // Check if this player is carrying a flag
        if (!flagManager.isCarryingFlag(playerUuid)) {
            return;
        }

        // Check if they're trying to drop from slot 0 (the flag slot)
        if (event.getSlotId() != 0) {
            // Not dropping from the flag slot, allow normal behavior
            return;
        }

        // Check if the item being dropped is a flag
        ItemStack droppingItem = player.getInventory().getHotbar().getItemStack(event.getSlotId());
        if (droppingItem == null || ItemStack.isEmpty(droppingItem)) {
            return;
        }

        String itemId = droppingItem.getItem().getId();
        if (itemId == null || !itemId.startsWith("CTF_Flag")) {
            return;
        }

        // Cancel the normal drop - we handle it specially
        event.setCancelled(true);

        // Get player position for drop location
        TransformComponent transform = entityRef.getStore()
            .getComponent(entityRef, TransformComponent.getComponentType());

        Vector3d dropPosition;
        if (transform != null) {
            dropPosition = transform.getTranslation();
        } else {
            dropPosition = new Vector3d(0, 0, 0);
        }

        // Drop the flag through our manager
        flagManager.dropFlag(playerUuid, dropPosition);

        plugin.getLogger().atInfo().log("Player {} dropped flag via G key at {}",
            playerUuid, dropPosition);
    }

}
