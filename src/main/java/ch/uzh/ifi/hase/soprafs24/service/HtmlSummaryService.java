package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;

import java.util.List;

@Service
public class HtmlSummaryService {

    private GameStatsService gameStatsService;

    public HtmlSummaryService(GameStatsService gameStatsService) {
        this.gameStatsService = gameStatsService;
    }

    public String buildGameResultHtml(Match match, Game game) {
        String html = """
                <div className="modalMessage modalMessageGameResult">
                      <table>
                        <thead>
                          <tr>
                            <th>GAME</th>
                            <th>1</th>
                            <th>2</th>
                            <th>3</th>
                            <th>4</th>
                            <th>| Match</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>Jane</td>
                            <td>7</td>
                            <td>2</td>
                            <td>26</td>
                            <td>9</td>
                            <td>44</td>
                          </tr>
                          <tr>
                            <td>Peter</td>
                            <td>14</td>
                            <td>8</td>
                            <td>26</td>
                            <td>10</td>
                            <td>32</td>
                          </tr>
                          <tr>
                            <td>Lisa</td>
                            <td>0</td>
                            <td>6</td>
                            <td>26</td>
                            <td>0</td>
                            <td>6</td>
                          </tr>
                          <tr>
                            <td>AI Lee (medium)</td>
                            <td>5</td>
                            <td>10</td>
                            <td>0</td>
                            <td>7</td>
                            <td>22</td>
                          </tr>
                        </tbody>
                      </table>
                      <div>
                        In Game 4 nobody was able to shoot the moon, but Lisa scored a perfect game, congrats!
                      </div>
                    </div>
                    """;
        return html;
    }

    public String buildMatchResultHtml(Match match, Game game) {
        String html = """
                <div className="modalMessage modalMessageMatchFinished">
                    <table>
                        <thead>
                            <tr>
                                <th>GAME</th>
                """;

        List<Game> games = match.getGames();
        int counter = 1;
        for (int i = 0; i < games.size(); i++) {
            html = html.concat(String.format("                <th>%s</th>\n", counter));
            counter++;
        }

        html = html.concat("""
                                <th>| Match</th>
                                <th>Perfect<br/>Rounds</th>
                                <th>Shooting<br/>the Moon</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

        MatchPlayer matchWinner = new MatchPlayer();
        matchWinner.setMatchScore(1000);

        for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
            // Checks if this player has a lower score than the one before
            if (matchPlayer.getMatchScore() < matchWinner.getMatchScore()) {
                matchWinner = matchPlayer;
            }

            // Adds the username
            html = html.concat(String.format("""
                                <tr>
                                    <td>%s</td>
                    """, matchPlayer.getUser().getUsername()));

            // Adds the score for each game
            for (int i = 0; i < games.size(); i++) {
                int matchPlayerScore = gameStatsService.getPlayerScoreInGame(matchPlayer.getMatchPlayerSlot(), games.get(i));
                html = html.concat(String.format("""
                                        <td>%s</td>
                        """, matchPlayerScore));
            }

            // Adds the total score, amount of perfect rounds, and amount of moon shots
            html = html.concat(String.format("""
                                    <td>%s</td>
                                    <td>%s</td>
                                    <td>%s</td>
                                </tr>
                    """, matchPlayer.getMatchScore(), matchPlayer.getPerfectGames(), matchPlayer.getShotTheMoonCount()));
        }

        // Adds the winner of the match
        html = html.concat(String.format("""
                        </tbody>
                    </table>
                        <div>
                            %s wins, CONGRATS!
                        </div>
                </div>
                """, matchWinner.getUser().getUsername()));

        return html;
    }
}
