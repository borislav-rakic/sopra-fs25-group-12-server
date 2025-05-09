package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;

public class MatchUtils {

    /**
     * Sets all human MatchPlayers of a match into setReady(false).
     * 
     * @param match                 Current Match object.
     * @param matchPlayerRepository An injected instantiation of
     *                              matchPlayerRepository.
     */

    public static void resetReadyStateForHumanPlayers(Match match, MatchPlayerRepository matchPlayerRepository) {
        for (MatchPlayer player : match.getMatchPlayers()) {
            if (!player.getUser().getIsAiPlayer()) {
                player.setReady(false);
                matchPlayerRepository.saveAndFlush(player);
            }
        }
    }

    /**
     * Returns true if all human players of a match are ready.
     * 
     * @param match current Match object.
     * @return true if all human players of the given match are set to ready.
     */
    public static boolean verifyAllHumanMatchPlayersReady(Match match) {
        return match.getMatchPlayers().stream()
                .filter(player -> !player.getUser().getIsAiPlayer())
                .allMatch(player -> player.getIsReady());
    }

}
