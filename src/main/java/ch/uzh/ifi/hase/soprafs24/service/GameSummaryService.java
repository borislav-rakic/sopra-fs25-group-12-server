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

import java.util.ArrayList;
import java.util.List;

@Service
public class GameSummaryService {

    private static final Logger log = LoggerFactory.getLogger(MatchSummaryService.class);
    private MatchRepository matchRepository;

    public GameSummaryService(
            MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public void saveGameResultHtml(Match match, Game game, String newsFlash) {
        // String gameResultHtml = buildGameResultHtml(match, game, newsFlash);
        // log.info("gameResultHtml{}", gameResultHtml);
        MatchSummary matchSummary = match.getMatchSummary();
        if (matchSummary == null) {
            throw new GameplayException("Could not update matchSummary (save game result).");
        }
        matchSummary.setGameSummaryHtml(buildGameResultHtml(match, game, newsFlash));
        // log.info("saved GameResult");
        matchRepository.save(match);
    }

    public String buildGameResultHtml(Match match, Game game, String newsFlash) {
        String html = "<div className=\"modalMessage modalMessageGameResult\">";
        html += "<table>";
        List<Game> games = match.getGames();
        List<MatchPlayer> mp = match.getMatchPlayersSortedBySlot();
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
        if (html.length() > 10000) {
            log.info("Very long summary.");
        }
        return html;
    }

    public String buildMatchResultHtml(Match match, Game game) {
        String html = "<div className=\"modalMessage modalMessageMatchResult\">";
        html += "<table>";
        List<Game> games = match.getGames();
        List<MatchPlayer> mp = match.getMatchPlayersSortedBySlot();
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
        html += "<td><em><strong>Moon shots</strong></em></td>";
        html += "<td><em><strong>" + mp.get(0).getShotTheMoonCount() + "</strong></em></td>";
        html += "<td><em><strong>" + mp.get(1).getShotTheMoonCount() + "</strong></em></td>";
        html += "<td><em><strong>" + mp.get(2).getShotTheMoonCount() + "</strong></em></td>";
        html += "<td><em><strong>" + mp.get(3).getShotTheMoonCount() + "</strong></em></td>";
        html += "</tr>";

        html += "<tr>";
        html += "<td><em><strong>Perfect games</strong></em></td>";
        html += "<td><em><strong>" + mp.get(0).getPerfectGames() + "</strong></em></td>";
        html += "<td><em><strong>" + mp.get(1).getPerfectGames() + "</strong></em></td>";
        html += "<td><em><strong>" + mp.get(2).getPerfectGames() + "</strong></em></td>";
        html += "<td><em><strong>" + mp.get(3).getPerfectGames() + "</strong></em></td>";
        html += "</tr>";

        MatchPlayer winner = new MatchPlayer();
        winner.setMatchScore(5000);

        for (int i = 0; i < mp.size(); i++) {
            if (mp.get(i).getMatchScore() < winner.getMatchScore()) {
                winner = mp.get(i);
            }
        }

        List<MatchPlayer> mpWinners = new ArrayList<>();

        for (int i = 0; i < mp.size(); i++) {
            if (mp.get(i).getMatchScore() == winner.getMatchScore()) {
                mpWinners.add(mp.get(i));
            }
        }

        html += "<tr>";
        html += "<th>Total:</th>";
        html += "<th>" + mp.get(0).getMatchScore() + "</th>";
        html += "<th>" + mp.get(1).getMatchScore() + "</th>";
        html += "<th>" + mp.get(2).getMatchScore() + "</th>";
        html += "<th>" + mp.get(3).getMatchScore() + "</th>";
        html += "</tr>";
        html += "</table>";
        html += "<div class=\"modalMessageNewsFlash\">";
        for (int i = 0; i < mpWinners.size(); i++) {
            if (mpWinners.size() == 1) {
                html += "<div class=\"modalMessageNewsFlashItem\">The winner is "
                        + mpWinners.get(i).getUser().getUsername()
                        + "!</div>";
                break;
            }

            if (i == 0) {
                html += "<div class=\"modalMessageNewsFlashItem\">The winners are ";
            } else {
                html += ", ";
            }

            html += mpWinners.get(i).getUser().getUsername();

            if (i == mpWinners.size() - 1) {
                html += "!</div>";
            }
        }
        html += "</div>";
        html += "</div>";

        return html;
    }
}
