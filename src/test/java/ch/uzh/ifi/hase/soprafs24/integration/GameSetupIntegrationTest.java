package ch.uzh.ifi.hase.soprafs24.integration;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameSetupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GameSetupIntegrationTest {

    @Autowired
    private GameSetupService gameSetupService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private MatchPlayerRepository matchPlayerRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @Test
    void testCreateAndStartGameForMatch_withSeed_distributesCardsCorrectly() {

        // Setup: a Match with 4 players
        Match match = new Match();
        match.setPhase(ch.uzh.ifi.hase.soprafs24.constant.MatchPhase.BEFORE_GAMES);
        match.setStarted(false);
        matchRepository.save(match);

        for (int i = 0; i < 4; i++) {
            User user = new User();
            user.setUsername("player" + i);
            user.setPassword("pw" + i);
            userRepository.saveAndFlush(user);

            MatchPlayer player = new MatchPlayer();
            player.setMatch(match);
            player.setMatchPlayerSlot(i);
            player.setUser(user);
            matchPlayerRepository.saveAndFlush(player);

            match.getMatchPlayers().add(player);
        }

        match = matchRepository.findById(match.getMatchId()).orElseThrow();

        // Use a known seed to get deterministic card distribution
        Long seed = 123L * 10000 + 9247;

        // Act: start the game
        matchRepository.saveAndFlush(match);
        match = matchRepository.findById(match.getMatchId()).orElseThrow();
        Game game = gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, seed);

        // Assert
        assertNotNull(game.getDeckId());
        assertEquals(ch.uzh.ifi.hase.soprafs24.constant.MatchPhase.IN_PROGRESS, match.getPhase());
        assertNotNull(game.getPhase());
        assertEquals(4, match.getMatchPlayers().size());

        for (MatchPlayer player : match.getMatchPlayers()) {
            assertNotNull(player.getHand());
            assertEquals(13, player.getHand().split(",").length);
        }
    }
}
