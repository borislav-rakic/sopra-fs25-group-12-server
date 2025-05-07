package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class GameStatsService {

    private final Logger log = LoggerFactory.getLogger(GameStatsService.class);

    private final GameStatsRepository gameStatsRepository;
    private final MatchRepository matchRepository;

    @Autowired
    public GameStatsService(
            @Qualifier("gameStatsRepository") GameStatsRepository gameStatsRepository,
            @Qualifier("matchRepository") MatchRepository matchRepository) {
        this.gameStatsRepository = gameStatsRepository;
        this.matchRepository = matchRepository;
    }

    public void initializeGameStats(Match match, Game game) {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                GameStats gameStats = new GameStats();
                gameStats.setMatch(match);
                gameStats.setGame(game);
                gameStats.setSuit(suit);
                gameStats.setRank(rank);
                gameStats.setPlayOrder(0);
                gameStats.setPlayedBy(0);
                gameStats.setPointsWorth(
                        suit == Suit.S && rank == Rank.Q ? 13 : (suit == Suit.H ? 1 : 0));
                gameStats.setPossibleHolders(0b1111);
                gameStats.setPointsBilledTo(0);
                gameStats.setCardHolder(0); // to be filled right after
                gameStats.setCardOrder(CardUtils.calculateCardOrder(rank.getSymbol() + suit.getSymbol()));
                gameStatsRepository.save(gameStats);
            }
        }
        gameStatsRepository.flush();

    }

    public List<GameStats> getGameStatsForMatch(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new IllegalArgumentException("Match not found with ID: " + matchId);
        }

        List<GameStats> stats = gameStatsRepository.findByMatch(match);

        if (stats == null || stats.isEmpty()) {
            // You can also use HttpStatus.NOT_FOUND if this is an API layer
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No game stats found for this match");
        }

        return stats;
    }

    public void deleteGameStatsForMatch(Match match) {
        gameStatsRepository.deleteByMatch(match);
        log.info("Deleted game stats for Match ID: {}", match.getMatchId());
    }

    public List<GameStats> getTrickByIndex(Game game, int trickIndex) {
        List<GameStats> allPlays = gameStatsRepository
                .findByGameAndPlayOrderGreaterThanOrderByPlayOrderAsc(game, 0);

        int start = trickIndex * 4;
        int end = Math.min(start + 4, allPlays.size());

        if (start >= allPlays.size()) {
            return List.of(); // no such trick
        }

        return allPlays.subList(start, end);
    }

    public List<GameStats> getPlayedCards(Game game) {
        return gameStatsRepository.findByGameAndPlayOrderGreaterThanOrderByPlayOrderAsc(game, 0);
    }

    @Transactional
    public void recordCardPlay(Game game, MatchPlayer matchPlayer, String cardCode) {
        // Validate matchPlayer
        if (matchPlayer == null) {
            throw new IllegalStateException("Match Player is null.");
        }
        // Get match safely
        Match match = matchPlayer.getMatch();
        if (match == null) {
            throw new IllegalStateException("Match Player does not belong to any match.");
        }

        // Record stats
        GameStats gameStats = gameStatsRepository.findByGameAndRankSuit(game, cardCode);
        // who dealt it?
        gameStats.setPlayedBy(matchPlayer.getMatchPlayerSlot()); // or getUser().getId(), depending on your entity
        // the how-many-eth card was it in this game?
        gameStats.setPlayOrder(game.getCurrentPlayOrder()); // explained below
        //
        gameStats.setOnlyPossibleHolder(matchPlayer.getMatchPlayerSlot()); // now we know exactly who had the card

        gameStats.setTrickNumber(game.getCurrentTrickNumber());

        gameStats.setTrickLeadBySlot(game.getTrickLeaderMatchPlayerSlot());

    }

    @Transactional
    public void updateGameStatsFromPlayers(Match match, Game game) {
        List<GameStats> statsToUpdate = new ArrayList<>();

        for (MatchPlayer player : match.getMatchPlayers()) {
            String hand = player.getHand(); // or getCardsAsString()
            if (hand != null && !hand.isBlank()) {
                String[] cards = hand.split(",");
                for (String code : cards) {
                    // Extract rank and suit from card string
                    Rank rank = Rank.fromSymbol(code.substring(0, code.length() - 1));
                    Suit suit = Suit.fromSymbol(code.substring(code.length() - 1));

                    // Fetch existing GameStats
                    GameStats existingStats = gameStatsRepository.findByRankAndSuitAndGame(rank, suit, game);

                    if (existingStats == null) {
                        throw new IllegalStateException("GameStats not found for card: " + code);
                    }

                    // Update the card holder
                    existingStats.setCardHolder(player.getMatchPlayerSlot());

                    statsToUpdate.add(existingStats);
                }
            }
        }

        // Save all updated GameStats
        gameStatsRepository.saveAll(statsToUpdate);
        gameStatsRepository.flush(); // good habit to flush when doing batch updates
    }

    /**
     * Updates the points_billed_to column in the respective entry in the GAME_STATS
     * relation.
     * 
     * @param game                  The given game.
     * @param winnerMatchPlayerSlot The winner of the trick.
     * @param rankSuit              The card code of the card.
     */
    public void updateGameStatsPointsBilledTo(Game game, String rankSuit, int winnerMatchPlayerSlot) {
        GameStats entry = gameStatsRepository.findByRankSuitAndGame(rankSuit, game);

        if (entry == null) {
            throw new IllegalStateException("GameStats entry not found for rank: " + rankSuit);
        }

        entry.setPointsBilledTo(winnerMatchPlayerSlot);
        gameStatsRepository.save(entry);
        gameStatsRepository.flush();

        log.info(String.format(" Points from card with rank %s were billed to %s", rankSuit, winnerMatchPlayerSlot));
    }

    /**
     * Returns the score of a given player in a given game.
     * 
     * @param matchPlayerSlot The match player slot of the given player.
     * @param game            The given game.
     * @return The score of the given player in the given game.
     */
    public int getPlayerScoreInGame(int matchPlayerSlot, Game game) {
        int score = 0;

        List<GameStats> gameStats = gameStatsRepository.findByGameAndPointsBilledTo(game, matchPlayerSlot);

        for (GameStats gameStat : gameStats) {
            score += gameStat.getPointsWorth();
        }

        return score;
    }

    /**
     * 
     * 
     * */
    public void updateGameStatsAfterTrickChange(Game game) {
        // If a player plays a card as the first person in the trick, then no inferences
        // can be made about their cards.
        // If, however, a player does not play in suit, we can infer that none of his
        // cards are in that suit.
        List<GameStats> gameStats = gameStatsRepository.findAllByGame(game);
        List<String> currentTrick = game.getCurrentTrick();
        if (currentTrick.size() == 0) {
            return;
        }
        int matchPlayerSlot = game.getCurrentMatchPlayerSlot();
        // It is a second, third or fourth card
        if (currentTrick.size() != 1) {
            String leadingCardCode = currentTrick.get(0);
            String leadingCardSuit = CardUtils.getSuitOfFirstCardInCardCodeString(leadingCardCode);
            String latestCardCode = currentTrick.get(currentTrick.size() - 1);
            String latestCardSuit = CardUtils.getSuitOfFirstCardInCardCodeString(latestCardCode);
            if (!latestCardSuit.equals(leadingCardSuit)) {
                int maskToClear = ~(1 << matchPlayerSlot); // bitmask to clear the player's bit

                for (GameStats stat : gameStats) {
                    if (stat.getSuit().getSymbol().equals(leadingCardSuit) && stat.getPlayOrder() == 0) {
                        int updatedHolders = stat.getPossibleHolders() & maskToClear;
                        stat.setPossibleHolders(updatedHolders);
                    }
                }
                gameStatsRepository.saveAll(gameStats);
                gameStatsRepository.flush();
            }

        }

    }

    public List<String> cardCodesInSlotFromSlot(Game game, int theirSlot, int mySlot) {
        List<String> possibleCards = new ArrayList<>();
        Match match = game.getMatch();
        MatchPlayer me = null;
        MatchPlayer them = null;

        for (MatchPlayer player : match.getMatchPlayers()) {
            if (player.getMatchPlayerSlot() == mySlot) {
                me = player;
            }
            if (player.getMatchPlayerSlot() == theirSlot) {
                them = player;
            }
        }

        if (me == null || them == null) {
            throw new IllegalArgumentException("Invalid player slot(s): " + mySlot + " or " + theirSlot);
        }

        List<GameStats> allStats = gameStatsRepository.findAllByGame(game);

        List<String> myHand = me.getHand() != null ? List.of(me.getHand().split(",")) : List.of();
        // List<String> passedToThem = getCardsIPassedToPlayer(game, mySlot, theirSlot);

        for (GameStats stat : allStats) {
            String cardCode = stat.getRank().getSymbol() + stat.getSuit().getSymbol();

            if (stat.getPlayOrder() > 0)
                continue;
            if (myHand.contains(cardCode))
                continue;
            if ((stat.getPossibleHolders() & (1 << theirSlot)) == 0)
                continue;

            possibleCards.add(cardCode);
        }

        return possibleCards;
    }

    public List<String> cardCodesInSlotFromSlotGivenLeadingSuit(
            Game game,
            int theirSlot,
            int mySlot,
            String leadingSuit) {
        // Do not even bother if there is no leadingSuit known.
        if (leadingSuit == null || leadingSuit.isBlank()) {
            return cardCodesInSlotFromSlot(game, theirSlot, mySlot);
        }
        List<String> possibleCards = new ArrayList<>();
        Match match = game.getMatch();
        MatchPlayer me = null;
        MatchPlayer them = null;

        for (MatchPlayer player : match.getMatchPlayers()) {
            if (player.getMatchPlayerSlot() == mySlot) {
                me = player;
            }
            if (player.getMatchPlayerSlot() == theirSlot) {
                them = player;
            }
        }

        if (me == null || them == null) {
            return List.of(); // gracefully return an empty list if slots are invalid
        }

        List<GameStats> allStats = gameStatsRepository.findAllByGame(game);
        List<String> myHand = me.getHand() != null ? List.of(me.getHand().split(",")) : List.of();

        if (leadingSuit == null || leadingSuit.isBlank()) {
            return cardCodesInSlotFromSlot(game, theirSlot, mySlot);
        }

        // Step 1: Determine if theirSlot might still have the leading suit
        boolean mightHaveLeadingSuit = false;
        for (GameStats stat : allStats) {
            if (stat.getSuit().getSymbol().equals(leadingSuit) &&
                    stat.getPlayOrder() == 0 &&
                    (stat.getPossibleHolders() & (1 << theirSlot)) != 0) {
                mightHaveLeadingSuit = true;
                break;
            }
        }

        // Step 2: Filter cards
        for (GameStats stat : allStats) {
            String cardCode = stat.getRank().getSymbol() + stat.getSuit().getSymbol();

            if (stat.getPlayOrder() > 0)
                continue;
            if (myHand.contains(cardCode))
                continue;
            if ((stat.getPossibleHolders() & (1 << theirSlot)) == 0)
                continue;

            // Enforce suit-following
            if (mightHaveLeadingSuit && !stat.getSuit().getSymbol().equals(leadingSuit))
                continue;

            possibleCards.add(cardCode);
        }

        return possibleCards;
    }

    public List<String> getCardsIPassedToPlayer(Game game, int mySlot, int theirSlot) {
        List<String> passedCardCodes = new ArrayList<>();

        List<GameStats> allStats = gameStatsRepository.findAllByGame(game);

        for (GameStats stat : allStats) {
            if (stat.getPassedBy() == mySlot && stat.getPassedTo() == theirSlot) {
                String cardCode = stat.getRankSuit();
                passedCardCodes.add(cardCode);
            }
        }

        return passedCardCodes;
    }

    public List<Integer> matchPlayerSlotsLeftAfterMyCard(Game game) {
        int mySlot = game.getCurrentMatchPlayerSlot();
        int alreadyPlayed = game.getCurrentTrick().size(); // number of cards already played
        int numPlayersLeftAfterMe = 3 - alreadyPlayed; // max 3 players follow me in trick

        if (numPlayersLeftAfterMe <= 0) {
            return List.of(); // I'm the last player in the trick
        }

        List<Integer> remaining = new ArrayList<>(numPlayersLeftAfterMe);
        for (int i = 1; i <= numPlayersLeftAfterMe; i++) {
            int nextSlot = ((mySlot - 1 + i) % 4) + 1; // wraparound for 1-based slots
            remaining.add(nextSlot);
        }
        return remaining;
    }

}
