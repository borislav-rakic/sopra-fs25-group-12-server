package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchUtilsTest {

    private MatchPlayerRepository matchPlayerRepository;

    private User createUser(boolean isAi) {
        User user = new User();
        user.setIsAiPlayer(isAi);
        return user;
    }

    private MatchPlayer createPlayer(boolean isAi, boolean isReady) {
        MatchPlayer player = new MatchPlayer();
        player.setUser(createUser(isAi));
        player.setReady(isReady);
        return player;
    }

    @BeforeEach
    void setUp() {
        matchPlayerRepository = mock(MatchPlayerRepository.class);
    }

    @Test
    void resetReadyStateForHumanPlayers_setsHumanReadyFalse() {
        MatchPlayer human1 = createPlayer(false, true);
        MatchPlayer ai1 = createPlayer(true, true);
        MatchPlayer human2 = createPlayer(false, true);

        Match match = new Match();
        match.setMatchPlayers(List.of(human1, ai1, human2));

        MatchUtils.resetReadyStateForHumanPlayers(match, matchPlayerRepository);

        assertFalse(human1.getIsReady());
        assertTrue(ai1.getIsReady()); // unchanged
        assertFalse(human2.getIsReady());

        verify(matchPlayerRepository, times(1)).saveAndFlush(human1);
        verify(matchPlayerRepository, times(0)).saveAndFlush(ai1);
        verify(matchPlayerRepository, times(1)).saveAndFlush(human2);
    }

    @Test
    void verifyAllHumanMatchPlayersReady_allReady_returnsTrue() {
        MatchPlayer human1 = createPlayer(false, true);
        MatchPlayer human2 = createPlayer(false, true);
        MatchPlayer ai = createPlayer(true, false);

        Match match = new Match();
        match.setMatchPlayers(List.of(human1, human2, ai));

        assertTrue(MatchUtils.verifyAllHumanMatchPlayersReady(match));
    }

    @Test
    void verifyAllHumanMatchPlayersReady_someNotReady_returnsFalse() {
        MatchPlayer human1 = createPlayer(false, true);
        MatchPlayer human2 = createPlayer(false, false);

        Match match = new Match();
        match.setMatchPlayers(List.of(human1, human2));

        assertFalse(MatchUtils.verifyAllHumanMatchPlayersReady(match));
    }

    @Test
    void verifyAllHumanMatchPlayersReady_noHumanPlayers_returnsTrue() {
        MatchPlayer ai1 = createPlayer(true, false);
        MatchPlayer ai2 = createPlayer(true, true);

        Match match = new Match();
        match.setMatchPlayers(List.of(ai1, ai2));

        assertTrue(MatchUtils.verifyAllHumanMatchPlayersReady(match));
    }
}
