package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchStats;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.MatchStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

import javax.transaction.Transactional;

@Service
@Transactional
public class MatchStatsService {

    private final Logger log = LoggerFactory.getLogger(MatchStatsService.class);

    private final MatchStatsRepository matchStatsRepository;

    @Autowired
    public MatchStatsService(@Qualifier("matchStatsRepository") MatchStatsRepository matchStatsRepository) {
        this.matchStatsRepository = matchStatsRepository;
    }

    public void updateStats(Match match, User player, int malusPoints, boolean gotPerfectGame, boolean shotTheMoon) {
        MatchStats stats = matchStatsRepository.findByMatchAndPlayer(match, player);
        if (stats == null) {
            throw new IllegalStateException("MatchStats not found for player: " + player.getUsername());
        }

        stats.addMalusPoints(malusPoints);

        if (gotPerfectGame) {
            stats.incrementPerfectGames();
        }

        if (shotTheMoon) {
            stats.incrementShotTheMoon();
        }

        matchStatsRepository.save(stats);
        log.info("Updated match stats for player {} in match {}: +{} malus, perfectGame={}, shotTheMoon={}",
                player.getUsername(), match.getMatchId(), malusPoints, gotPerfectGame, shotTheMoon);
    }

    public void initializeStats(Match match, List<User> players) {
        for (User player : players) {
            MatchStats stats = new MatchStats();
            stats.setMatch(match);
            stats.setPlayer(player);
            stats.setMalusPoints(0);
            stats.setPerfectGames(0);
            stats.setShotTheMoonCount(0);

            matchStatsRepository.save(stats);
        }
        log.info("Initialized match stats for match ID {} and {} players", match.getMatchId(), players.size());
    }

}
