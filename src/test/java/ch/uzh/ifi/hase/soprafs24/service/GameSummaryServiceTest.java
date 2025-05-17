package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;

public class GameSummaryServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private GameSummaryService gameSummaryService;

    private Match match;
    private Game game1, game2, game3, game4;
    private MatchPlayer matchPlayer1, matchPlayer2, matchPlayer3, matchPlayer4;
    private User user1, user2, user3, user4;

    String newsFlash;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        List<Integer> scoreList = List.of(15, 2, 5, 20);

        game1 = createGame(1, scoreList);
        game2 = createGame(2, scoreList);
        game3 = createGame(3, scoreList);
        game4 = createGame(4, scoreList);

        List<Game> games = List.of(game1, game2, game3, game4);

        user1 = createUser("user1");
        user2 = createUser("user2");
        user3 = createUser("user3");
        user4 = createUser("user4");

        matchPlayer1 = createMatchPlayer(user1, 70, 1, 0, 1);
        matchPlayer2 = createMatchPlayer(user2, 80, 2, 2, 2);
        matchPlayer3 = createMatchPlayer(user3, 90, 3, 3, 3);
        matchPlayer4 = createMatchPlayer(user4, 50, 4, 0, 0);

        List<MatchPlayer> matchPlayers = List.of(matchPlayer1, matchPlayer2, matchPlayer3, matchPlayer4);

        match = new Match();
        match.setMatchId(1L);
        match.setGames(new ArrayList<>(games));
        match.setMatchPlayers(new ArrayList<>(matchPlayers));
        match.setMatchSummary(new MatchSummary());

        newsFlash = "Another beautiful game.";

        // Sorted by slot
        match.setMatchPlayers(new ArrayList<>(matchPlayers));
    }

    @Test
    public void saveGameResultHtml_setsHtmlAndCallsSave() {
        gameSummaryService.saveGameResultHtml(match, game4, newsFlash);
        String html = match.getMatchSummary().getGameSummaryHtml();

        assertNotNull(html);
        assertTrue(html.contains("modalMessageGameResult"));
        assertTrue(html.contains("user1"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("20")); // one of the scores
        verify(matchRepository).save(match);
    }

    @Test
    public void saveGameResultHtml_throwsIfMatchSummaryNull() {
        match.setMatchSummary(null);
        assertThrows(GameplayException.class, () -> gameSummaryService.saveGameResultHtml(match, game1, newsFlash));
    }

    @Test
    public void buildGameResultHtml_returnsValidHtml() {
        String html = gameSummaryService.buildGameResultHtml(match, game1, newsFlash);

        assertNotNull(html);
        assertTrue(html.contains("Game"));
        assertTrue(html.contains("Total:"));
        assertTrue(html.contains(newsFlash));
        assertTrue(html.contains("user1"));
        assertTrue(html.contains("20"));
    }

    @Test
    public void buildMatchResultHtml_returnsValidHtmlWithWinners() {
        String html = gameSummaryService.buildMatchResultHtml(match, game1);

        assertNotNull(html);
        assertTrue(html.contains("Moon shots"));
        assertTrue(html.contains("Perfect games"));
        assertTrue(html.contains("Total:"));
        assertTrue(html.contains("winner") || html.contains("winners"));
        assertTrue(html.contains("user4")); // lowest score
    }

    // --- Helper methods ---

    private Game createGame(int gameNumber, List<Integer> scores) {
        Game game = new Game();
        game.setGameNumber(gameNumber);
        game.setGameScoresList(scores);
        return game;
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private MatchPlayer createMatchPlayer(User user, int score, int slot, int perfectGames, int moonShots) {
        MatchPlayer player = new MatchPlayer();
        player.setUser(user);
        player.setMatchScore(score);
        player.setMatchPlayerSlot(slot);
        player.setPerfectGames(perfectGames);
        player.setShotTheMoonCount(moonShots);
        return player;
    }
}
