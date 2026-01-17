package com.example.ctf.match;

import com.example.ctf.CTFPlugin;
import com.example.ctf.FlagTeam;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manages CTF match state, scoring, and win conditions.
 */
public class MatchManager {

    private final CTFPlugin plugin;
    private MatchState state;
    private final Map<FlagTeam, Integer> scores;
    private int scoreLimit;
    @Nullable
    private FlagTeam winner;

    public MatchManager(@Nonnull CTFPlugin plugin) {
        this.plugin = plugin;
        this.state = MatchState.WAITING;
        this.scores = new EnumMap<>(FlagTeam.class);
        this.scoreLimit = 3; // Default: first to 3 captures wins
        resetScores();
    }

    /**
     * Starts the match. Transitions from WAITING to ACTIVE.
     *
     * @return true if the match was started, false if already active/ended
     */
    public boolean startMatch() {
        if (state != MatchState.WAITING) {
            return false;
        }

        state = MatchState.ACTIVE;
        winner = null;
        resetScores();
        broadcastMessage("Match started! First to " + scoreLimit + " captures wins!");
        plugin.getLogger().atInfo().log("CTF match started");
        return true;
    }

    /**
     * Ends the match early without a winner.
     *
     * @return true if the match was ended, false if already ended
     */
    public boolean endMatch() {
        if (state == MatchState.ENDED) {
            return false;
        }

        state = MatchState.ENDED;
        broadcastMessage("Match ended!");
        plugin.getLogger().atInfo().log("CTF match ended manually");
        return true;
    }

    /**
     * Ends the match with a winner.
     *
     * @param winningTeam The team that won
     */
    private void endMatchWithWinner(@Nonnull FlagTeam winningTeam) {
        state = MatchState.ENDED;
        winner = winningTeam;

        String finalScore = String.format("%s %d - %d %s",
            FlagTeam.RED.getDisplayName(), scores.get(FlagTeam.RED),
            scores.get(FlagTeam.BLUE), FlagTeam.BLUE.getDisplayName());

        broadcastMessage(winningTeam.getDisplayName() + " wins! Final score: " + finalScore);
        plugin.getLogger().atInfo().log("CTF match won by {} with score {}", winningTeam, finalScore);
    }

    /**
     * Resets the match back to WAITING state.
     *
     * @return true if the match was reset
     */
    public boolean resetMatch() {
        state = MatchState.WAITING;
        winner = null;
        resetScores();

        // Return all flags to stands
        plugin.getFlagCarrierManager().returnFlagToStand(FlagTeam.RED);
        plugin.getFlagCarrierManager().returnFlagToStand(FlagTeam.BLUE);

        broadcastMessage("Match reset. Scores cleared.");
        plugin.getLogger().atInfo().log("CTF match reset");
        return true;
    }

    /**
     * Adds a point to a team's score and checks win condition.
     *
     * @param team The team that scored
     * @return true if this score won the match
     */
    public boolean addScore(@Nonnull FlagTeam team) {
        if (state != MatchState.ACTIVE) {
            return false;
        }

        int newScore = scores.get(team) + 1;
        scores.put(team, newScore);

        String scoreString = String.format("%s %d - %d %s",
            FlagTeam.RED.getDisplayName(), scores.get(FlagTeam.RED),
            scores.get(FlagTeam.BLUE), FlagTeam.BLUE.getDisplayName());

        broadcastMessage(team.getDisplayName() + " captured the flag! Score: " + scoreString);
        plugin.getLogger().atInfo().log("{} scored! Current: {}", team, scoreString);

        // Check win condition
        if (newScore >= scoreLimit) {
            endMatchWithWinner(team);
            return true;
        }

        return false;
    }

    /**
     * Gets a team's current score.
     *
     * @param team The team
     * @return The team's score
     */
    public int getScore(@Nonnull FlagTeam team) {
        return scores.get(team);
    }

    /**
     * Gets the current match state.
     *
     * @return The match state
     */
    @Nonnull
    public MatchState getState() {
        return state;
    }

    /**
     * Checks if the match is currently active.
     *
     * @return true if the match is in ACTIVE state
     */
    public boolean isMatchActive() {
        return state == MatchState.ACTIVE;
    }

    /**
     * Gets the winner of the match, if any.
     *
     * @return The winning team, or null if no winner yet
     */
    @Nullable
    public FlagTeam getWinner() {
        return winner;
    }

    /**
     * Gets the score limit (captures needed to win).
     *
     * @return The score limit
     */
    public int getScoreLimit() {
        return scoreLimit;
    }

    /**
     * Sets the score limit (captures needed to win).
     * Only effective when match is in WAITING state.
     *
     * @param limit The new score limit (minimum 1)
     * @return true if the limit was set
     */
    public boolean setScoreLimit(int limit) {
        if (state != MatchState.WAITING || limit < 1) {
            return false;
        }
        this.scoreLimit = limit;
        return true;
    }

    /**
     * Gets a formatted score string.
     *
     * @return The score string like "Red: 2, Blue: 1"
     */
    @Nonnull
    public String getScoreString() {
        return String.format("%s: %d, %s: %d",
            FlagTeam.RED.getDisplayName(), scores.get(FlagTeam.RED),
            FlagTeam.BLUE.getDisplayName(), scores.get(FlagTeam.BLUE));
    }

    private void resetScores() {
        scores.put(FlagTeam.RED, 0);
        scores.put(FlagTeam.BLUE, 0);
    }

    private void broadcastMessage(@Nonnull String message) {
        // Broadcast to all connected players
        for (PlayerRef playerRef : plugin.getServerState().getAllPlayerRefs()) {
            playerRef.sendChatMessage(Message.raw("[CTF] " + message));
        }
    }
}
