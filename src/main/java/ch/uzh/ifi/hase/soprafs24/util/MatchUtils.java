package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;

public class MatchUtils {

    public static void resetReadyStateForHumanPlayers(Match match, MatchPlayerRepository matchPlayerRepository) {
        for (MatchPlayer player : match.getMatchPlayers()) {
            if (!player.getUser().getIsAiPlayer()) {
                player.setReady(false);
                matchPlayerRepository.saveAndFlush(player);
            }
        }
    }

    public static boolean verifyAllHumanMatchPlayersReady(Match match) {
        return match.getMatchPlayers().stream()
                .filter(player -> !player.getUser().getIsAiPlayer())
                .allMatch(player -> player.getIsReady());
    }

}
