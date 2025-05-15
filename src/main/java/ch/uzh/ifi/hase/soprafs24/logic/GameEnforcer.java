package ch.uzh.ifi.hase.soprafs24.logic;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;

public final class GameEnforcer {

    private GameEnforcer() {
    } // Prevent instantiation

    // --- In-memory (from Match object) ---

    public static Game getOnlyActiveGameOrNull(Match match) {
        List<Game> activeGames = match.getGames().stream()
                .filter(g -> g.getPhase() != GamePhase.FINISHED && g.getPhase() != GamePhase.ABORTED)
                .toList();

        if (activeGames.size() > 1) {
            throw new IllegalStateException("Multiple active games found for match " + match.getMatchId());
        }

        return activeGames.isEmpty() ? null : activeGames.get(0);
    }

    public static Game requireExactlyOneActiveGame(Match match) {
        Game game = getOnlyActiveGameOrNull(match);
        if (game == null) {
            throw new IllegalStateException("No active game found for match " + match.getMatchId());
        }
        return game;
    }

    public static void assertNoActiveGames(Match match) {
        if (getOnlyActiveGameOrNull(match) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active game already exists for match " + match.getMatchId());
        }
    }

    // --- DB-backed (from matchId + repository) ---

    public static Game getOnlyActiveGameOrNull(Long matchId, GameRepository repo) {
        List<Game> games = repo.findActiveGamesByMatchId(matchId);

        if (games.size() > 1) {
            throw new IllegalStateException("Multiple active games found in DB for match " + matchId);
        }

        return games.isEmpty() ? null : games.get(0);
    }

    public static Game requireExactlyOneActiveGame(Long matchId, GameRepository repo) {
        Game game = getOnlyActiveGameOrNull(matchId, repo);
        if (game == null) {
            throw new IllegalStateException("No active game found in DB for match " + matchId);
        }
        return game;
    }

    public static void assertNoActiveGames(Long matchId, GameRepository repo) {
        if (getOnlyActiveGameOrNull(matchId, repo) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active game already exists in DB for match " + matchId);
        }
    }
}
