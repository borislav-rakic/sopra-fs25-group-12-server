package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

public class HtmlSummaryServiceTest {
    @Mock
    private GameStatsService gameStatsService = Mockito.mock(GameStatsService.class);

    @InjectMocks
    private HtmlSummaryService htmlSummaryService = new HtmlSummaryService(gameStatsService);

    Match match;
    Game game1;
    Game game2;
    Game game3;
    Game game4;
    MatchPlayer matchPlayer1;
    MatchPlayer matchPlayer2;
    MatchPlayer matchPlayer3;
    MatchPlayer matchPlayer4;
    User user1;
    User user2;
    User user3;
    User user4;

    @BeforeEach
    public void setup() {
        game1 = new Game();
        game2 = new Game();
        game3 = new Game();
        game4 = new Game();

        List<Game> games = new ArrayList<>();
        games.add(game1);
        games.add(game2);
        games.add(game3);
        games.add(game4);

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
        matchPlayer2 = new MatchPlayer();
        matchPlayer2.setMatchScore(80);
        matchPlayer2.setUser(user2);
        matchPlayer2.setPerfectGames(2);
        matchPlayer2.setShotTheMoonCount(0);
        matchPlayer3 = new MatchPlayer();
        matchPlayer3.setMatchScore(90);
        matchPlayer3.setUser(user3);
        matchPlayer3.setPerfectGames(3);
        matchPlayer3.setShotTheMoonCount(0);
        matchPlayer4 = new MatchPlayer();
        matchPlayer4.setMatchScore(50);
        matchPlayer4.setUser(user4);
        matchPlayer4.setPerfectGames(0);
        matchPlayer4.setShotTheMoonCount(0);

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(matchPlayer1);
        matchPlayers.add(matchPlayer2);
        matchPlayers.add(matchPlayer3);
        matchPlayers.add(matchPlayer4);

        match = new Match();
        match.setMatchId(1L);
        match.setMatchGoal(100);
        match.setHostUsername("user1");
        match.getGames().addAll(games);
        match.getMatchPlayers().addAll(matchPlayers);
    }

    @Test
    public void buildMatchResultHtml() {
        given(gameStatsService.getPlayerScoreInGame(Mockito.anyInt(), Mockito.any())).willReturn(
                15, 2, 5, 20,
                15, 2, 5, 20,
                15, 2, 5, 20,
                15, 2, 5, 20);

        String expected = """
                <div className="modalMessage modalMessageMatchFinished">
                    <table>
                        <thead>
                            <tr>
                                <th>GAME</th>
                                <th>1</th>
                                <th>2</th>
                                <th>3</th>
                                <th>4</th>
                                <th>| Match</th>
                                <th>Perfect<br/>Rounds</th>
                                <th>Shooting<br/>the Moon</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>user1</td>
                                <td>15</td>
                                <td>2</td>
                                <td>5</td>
                                <td>20</td>
                                <td>70</td>
                                <td>0</td>
                                <td>1</td>
                            </tr>
                            <tr>
                                <td>user2</td>
                                <td>15</td>
                                <td>2</td>
                                <td>5</td>
                                <td>20</td>
                                <td>80</td>
                                <td>2</td>
                                <td>0</td>
                            </tr>
                            <tr>
                                <td>user3</td>
                                <td>15</td>
                                <td>2</td>
                                <td>5</td>
                                <td>20</td>
                                <td>90</td>
                                <td>3</td>
                                <td>0</td>
                            </tr>
                            <tr>
                                <td>user4</td>
                                <td>15</td>
                                <td>2</td>
                                <td>5</td>
                                <td>20</td>
                                <td>50</td>
                                <td>0</td>
                                <td>0</td>
                            </tr>
                        </tbody>
                    </table>
                        <div>
                            user4 wins, CONGRATS!
                        </div>
                </div>
                """;

        assertEquals(expected, htmlSummaryService.buildMatchResultHtml(match, game4));
    }
}
