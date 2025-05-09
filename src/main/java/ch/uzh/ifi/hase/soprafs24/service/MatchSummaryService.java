package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class MatchSummaryService {

  private static final Logger log = LoggerFactory.getLogger(MatchSummaryService.class);
  private MatchRepository matchRepository;

  public MatchSummaryService(
      MatchRepository matchRepository) {
    this.matchRepository = matchRepository;
  }

  public void saveGameResultHtml(Match match, Game game, String newsFlash) {
    String gameResultHtml = buildGameResultHtml(match, game, newsFlash);
    log.info("gameResultHtml{}", gameResultHtml);
    MatchSummary matchSummary = match.getMatchSummary();
    if (matchSummary == null) {
      throw new GameplayException("Could not update matchSummary (save game result).");
    }
    matchSummary.setGameSummaryHtml(buildGameResultHtml(match, game, newsFlash));
    log.info("saved GameResult");
    matchRepository.save(match);
  }

  public String buildGameResultHtml(Match match, Game game, String newsFlash) {
    String html = "<div className=\"modalMessage modalMessageGameResult\">";
    html += "<table>";
    List<Game> games = match.getGames();
    List<MatchPlayer> mp = match.getMatchPlayers();
    html += "<tr>";
    html += "<th>Game</th>";
    html += "<th>" + mp.get(0).getUser().getUsername() + "</th>";
    html += "<th>" + mp.get(1).getUser().getUsername() + "</th>";
    html += "<th>" + mp.get(2).getUser().getUsername() + "</th>";
    html += "<th>" + mp.get(3).getUser().getUsername() + "</th>";
    html += "</tr>";
    for (int i = 0; i < games.size(); i++) {
      Game someGame = games.get(i);
      List<Integer> gameScores = someGame.getGameScoresList();
      html += "<tr>";
      html += "<td>" + someGame.getGameNumber() + "</td>";
      for (int j = 0; j < gameScores.size(); j++) {
        html += "<td>" + gameScores.get(j) + "</td>";
      }
      html += "</tr>";
    }
    html += "<tr>";
    html += "<th>Total:</th>";
    html += "<th>" + mp.get(0).getMatchScore() + "</th>";
    html += "<th>" + mp.get(1).getMatchScore() + "</th>";
    html += "<th>" + mp.get(2).getMatchScore() + "</th>";
    html += "<th>" + mp.get(3).getMatchScore() + "</th>";
    html += "</tr>";
    html += "</table>";
    html += "<div class=\"modalMessageNewsFlash\">" + newsFlash + "</div>";
    html += "</div>";
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
        int matchPlayerScore = games.get(i).getGameScoresList().get(matchPlayer.getMatchPlayerSlot() - 1);
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
