package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MatchSummaryServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchSummaryService matchSummaryService;

    private Match match;
    private Game game1, game2, game3, game4;
    private MatchPlayer matchPlayer1, matchPlayer2, matchPlayer3, matchPlayer4;
    private User user1, user2, user3, user4;

    String newsFlash;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        List<Integer> scoreList = List.of(15, 2, 5, 20);

        game1 = new Game();
        game1.setGameScoresList(scoreList);
        game2 = new Game();
        game2.setGameScoresList(scoreList);
        game3 = new Game();
        game3.setGameScoresList(scoreList);
        game4 = new Game();
        game4.setGameScoresList(scoreList);

        List<Game> games = List.of(game1, game2, game3, game4);

        user1 = new User();
        user1.setUsername("user1");
        user2 = new User();
        user2.setUsername("user2");
        user3 = new User();
        user3.setUsername("user3");
        user4 = new User();
        user4.setUsername("user4");

        matchPlayer1 = new MatchPlayer();
        matchPlayer1.setMatchScore(70);
        matchPlayer1.setUser(user1);
        matchPlayer1.setPerfectGames(0);
        matchPlayer1.setShotTheMoonCount(1);
        matchPlayer1.setMatchPlayerSlot(1);

        matchPlayer2 = new MatchPlayer();
        matchPlayer2.setMatchScore(80);
        matchPlayer2.setUser(user2);
        matchPlayer2.setPerfectGames(2);
        matchPlayer2.setShotTheMoonCount(0);
        matchPlayer2.setMatchPlayerSlot(2);

        matchPlayer3 = new MatchPlayer();
        matchPlayer3.setMatchScore(90);
        matchPlayer3.setUser(user3);
        matchPlayer3.setPerfectGames(3);
        matchPlayer3.setShotTheMoonCount(0);
        matchPlayer3.setMatchPlayerSlot(3);

        matchPlayer4 = new MatchPlayer();
        matchPlayer4.setMatchScore(50);
        matchPlayer4.setUser(user4);
        matchPlayer4.setPerfectGames(0);
        matchPlayer4.setShotTheMoonCount(0);
        matchPlayer4.setMatchPlayerSlot(4);

        List<MatchPlayer> matchPlayers = List.of(matchPlayer1, matchPlayer2, matchPlayer3, matchPlayer4);

        match = new Match();
        match.setMatchId(1L);
        match.setMatchGoal(100);
        match.setHostUsername("user1");
        match.setGames(new ArrayList<>(games));
        match.setMatchPlayers(new ArrayList<>(matchPlayers));
        match.setMatchSummary(new MatchSummary());

        newsFlash = "Another beautiful game.";
    }

    @Test
    public void saveGameResultHtml_setsHtmlAndCallsSave() {
        matchSummaryService.saveGameResultHtml(match, game4, newsFlash);

        String html = match.getMatchSummary().getGameSummaryHtml();

        // Assert basic structure
        assertTrue(html.contains("modalMessageGameResult"));
        assertTrue(html.contains("user1"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("20")); // One of the scores

        // Verify that matchRepository.save was called
        verify(matchRepository).save(match);
    }

    @Test
    void saveGameResultHtml_shouldSetHtmlOnSummaryAndSave() {
        Match match = createMockMatch();
        Game game = createMockGame(1, List.of(10, 20, 30, 40));
        match.getGames().add(game);
        String newsFlash = "Game over!";
        match.setMatchSummary(new MatchSummary());

        matchSummaryService.saveGameResultHtml(match, game, newsFlash);

        assertNotNull(match.getMatchSummary().getGameSummaryHtml());
        verify(matchRepository, times(1)).save(match);
    }

    @Test
    void saveGameResultHtml_throwsIfMatchSummaryNull() {
        Match match = new Match();
        Game game = new Game();

        assertThrows(GameplayException.class, () -> {
            matchSummaryService.saveGameResultHtml(match, game, "news");
        });
    }

    @Test
    void buildGameResultHtml_createsExpectedHtml() {
        Match match = createMockMatch();
        match.getGames().add(createMockGame(1, List.of(1, 2, 3, 4)));
        String html = matchSummaryService.buildGameResultHtml(match, match.getGames().get(0), "News");

        assertTrue(html.contains("Game"));
        assertTrue(html.contains("Total:"));
        assertTrue(html.contains("News"));
    }

    @Test
    void buildMatchResultHtml_createsExpectedHtml() {
        Match match = createMockMatch();
        match.getGames().add(createMockGame(1, List.of(10, 20, 30, 40)));
        String html = matchSummaryService.buildMatchResultHtml(match, match.getGames().get(0));

        assertTrue(html.contains("Moon shots"));
        assertTrue(html.contains("Perfect games"));
        assertTrue(html.contains("Total:"));
        assertTrue(html.contains("winner") || html.contains("winners"));
    }

    // --- Helper Methods ---

    private Match createMockMatch() {
        Match match = new Match();
        match.setMatchSummary(new MatchSummary());
        match.setGames(new java.util.ArrayList<>());

        List<MatchPlayer> players = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            MatchPlayer player = new MatchPlayer();
            User user = new User();
            user.setUsername("player" + i);
            player.setUser(user);
            player.setMatchScore(i * 10);
            player.setPerfectGames(i);
            player.setShotTheMoonCount(i);
            players.add(player);
        }
        match.setMatchPlayers(players);
        return match;
    }

    private Game createMockGame(int gameNumber, List<Integer> scores) {
        Game game = new Game();
        game.setGameNumber(gameNumber);
        game.setGameScoresList(scores);
        return game;
    }
}
