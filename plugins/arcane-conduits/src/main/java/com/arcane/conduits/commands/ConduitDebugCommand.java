package com.arcane.conduits.commands;

import com.arcane.conduits.ArcaneConduitsPlugin;
import com.arcane.conduits.blocks.state.ConduitBlockState;
import com.arcane.conduits.core.power.ConduitNetworkManager;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import javax.annotation.Nonnull;

/**
 * Debug command for testing and inspecting conduit networks.
 *
 * Usage:
 *   /conduit power <x> <y> <z>  - Get power level at position
 *   /conduit network <x> <y> <z> - Get network info at position
 *   /conduit recalc <x> <y> <z> - Force recalculate network
 *   /conduit set <x> <y> <z> <power> - Set power level (testing)
 */
public class ConduitDebugCommand extends CommandBase {

    public ConduitDebugCommand() {
        super("conduit", "Debug commands for Arcane Conduits plugin.");
        this.setPermissionGroup(GameMode.Creative);  // Requires creative/OP
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String[] args = ctx.getArguments();

        if (args.length == 0) {
            sendHelp(ctx);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "power" -> handlePower(ctx, args);
            case "network" -> handleNetwork(ctx, args);
            case "recalc" -> handleRecalculate(ctx, args);
            case "set" -> handleSetPower(ctx, args);
            case "help" -> sendHelp(ctx);
            default -> ctx.sendMessage(Message.raw("Unknown subcommand: " + subCommand + ". Use /conduit help"));
        }
    }

    private void handlePower(CommandContext ctx, String[] args) {
        if (args.length < 4) {
            ctx.sendMessage(Message.raw("Usage: /conduit power <x> <y> <z>"));
            return;
        }

        Vector3i pos = parsePosition(ctx, args, 1);
        if (pos == null) return;

        World world = getWorld(ctx);
        if (world == null) return;

        ConduitBlockState state = getConduitState(world, pos);
        if (state == null) {
            ctx.sendMessage(Message.raw("No conduit at " + formatPos(pos)));
            return;
        }

        ctx.sendMessage(Message.raw(String.format(
            "Conduit at %s: power=%d/%d (%s), connections=%d, decay=%d",
            formatPos(pos),
            state.getPowerLevel(),
            state.getMaxPower(),
            state.getPowerCategory(),
            Integer.bitCount(state.getConnectionMask()),
            state.getDecayRate()
        )));
    }

    private void handleNetwork(CommandContext ctx, String[] args) {
        if (args.length < 4) {
            ctx.sendMessage(Message.raw("Usage: /conduit network <x> <y> <z>"));
            return;
        }

        Vector3i pos = parsePosition(ctx, args, 1);
        if (pos == null) return;

        World world = getWorld(ctx);
        if (world == null) return;

        ArcaneConduitsPlugin plugin = ArcaneConduitsPlugin.getInstance();
        if (plugin == null || plugin.getNetworkManager() == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized"));
            return;
        }

        ConduitNetworkManager.NetworkDebugInfo info =
            plugin.getNetworkManager().getNetworkDebugInfo(world, pos);

        ctx.sendMessage(Message.raw(String.format(
            "Network at %s: %s",
            formatPos(pos),
            info.toString()
        )));
    }

    private void handleRecalculate(CommandContext ctx, String[] args) {
        if (args.length < 4) {
            ctx.sendMessage(Message.raw("Usage: /conduit recalc <x> <y> <z>"));
            return;
        }

        Vector3i pos = parsePosition(ctx, args, 1);
        if (pos == null) return;

        World world = getWorld(ctx);
        if (world == null) return;

        ArcaneConduitsPlugin plugin = ArcaneConduitsPlugin.getInstance();
        if (plugin == null || plugin.getNetworkManager() == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized"));
            return;
        }

        plugin.getNetworkManager().recalculateNetworkNow(world, pos);
        ctx.sendMessage(Message.raw("Network recalculated at " + formatPos(pos)));
    }

    private void handleSetPower(CommandContext ctx, String[] args) {
        if (args.length < 5) {
            ctx.sendMessage(Message.raw("Usage: /conduit set <x> <y> <z> <power>"));
            return;
        }

        Vector3i pos = parsePosition(ctx, args, 1);
        if (pos == null) return;

        int power;
        try {
            power = Integer.parseInt(args[4]);
            if (power < 0 || power > 15) {
                ctx.sendMessage(Message.raw("Power must be 0-15"));
                return;
            }
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("Invalid power value: " + args[4]));
            return;
        }

        World world = getWorld(ctx);
        if (world == null) return;

        ConduitBlockState state = getConduitState(world, pos);
        if (state == null) {
            ctx.sendMessage(Message.raw("No conduit at " + formatPos(pos)));
            return;
        }

        state.setPowerLevel(power);
        ctx.sendMessage(Message.raw(String.format(
            "Set power to %d at %s",
            power, formatPos(pos)
        )));
    }

    private void sendHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Arcane Conduits Debug Commands:"));
        ctx.sendMessage(Message.raw("  /conduit power <x> <y> <z> - Get power level"));
        ctx.sendMessage(Message.raw("  /conduit network <x> <y> <z> - Get network info"));
        ctx.sendMessage(Message.raw("  /conduit recalc <x> <y> <z> - Force recalculate"));
        ctx.sendMessage(Message.raw("  /conduit set <x> <y> <z> <power> - Set power (0-15)"));
    }

    // ==================== Helper Methods ====================

    private Vector3i parsePosition(CommandContext ctx, String[] args, int startIndex) {
        try {
            int x = Integer.parseInt(args[startIndex]);
            int y = Integer.parseInt(args[startIndex + 1]);
            int z = Integer.parseInt(args[startIndex + 2]);
            return new Vector3i(x, y, z);
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("Invalid coordinates"));
            return null;
        }
    }

    private World getWorld(CommandContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) {
            ctx.sendMessage(Message.raw("Command must be run by a player"));
            return null;
        }

        World world = player.getWorld();
        if (world == null) {
            ctx.sendMessage(Message.raw("Player not in a world"));
            return null;
        }

        return world;
    }

    private ConduitBlockState getConduitState(World world, Vector3i pos) {
        WorldChunk chunk = world.getChunkIfLoaded(pos.x >> 5, pos.z >> 5);
        if (chunk == null) {
            return null;
        }

        BlockState state = chunk.getState(pos.x & 31, pos.y, pos.z & 31);
        if (state instanceof ConduitBlockState) {
            return (ConduitBlockState) state;
        }
        return null;
    }

    private String formatPos(Vector3i pos) {
        return String.format("(%d, %d, %d)", pos.x, pos.y, pos.z);
    }
}
