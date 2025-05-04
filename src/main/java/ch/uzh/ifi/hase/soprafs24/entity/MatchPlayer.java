package ch.uzh.ifi.hase.soprafs24.entity;

import java.time.Instant;

import javax.persistence.*;

import ch.uzh.ifi.hase.soprafs24.constant.AiMatchPlayerState;
import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

/**
 * The MATCH_PLAYER relation saves the ids of the players that are in a match,
 * and their card decks.
 */
@Entity
@Table(name = "MATCH_PLAYER")
public class MatchPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchPlayerId;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private User user;

    @Column(name = "matchPlayerSlot", nullable = false)
    private int matchPlayerSlot;

    @Column(name = "hand", nullable = false)
    private String hand = "";

    @Column(name = "match_score", nullable = false)
    private int matchScore = 0; // total score across games (formerly "score")

    @Column(name = "game_score", nullable = false)
    private int gameScore = 0; // score inside the current Game

    @Column(nullable = false)
    private Boolean ready = false; // player clicked "OK" on result screen

    @Column(nullable = false)
    private int perfectGames = 0; // how many perfect (0 point) games

    @Column(nullable = false)
    private int shotTheMoonCount = 0; // how many times shot the moon

    @Column(nullable = false)
    private Boolean isAiPlayer = false;

    @Column
    @Enumerated(EnumType.STRING)
    private Strategy strategy;

    @Column(nullable = false)
    private Instant lastPollTime = Instant.now();

    @Column(nullable = false)
    private int pollCounter = 0;

    @Column
    @Enumerated(EnumType.STRING)
    AiMatchPlayerState aiMatchPlayerState = AiMatchPlayerState.READY;

    // === Getter and Setter methods ===

    public Long getMatchPlayerId() {
        return matchPlayerId;
    }

    public void setMatchPlayerId(Long matchPlayerId) {
        this.matchPlayerId = matchPlayerId;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHand() {
        return CardUtils.normalizeCardCodeString(hand);
    }

    public void setHand(String hand) {
        this.hand = hand;
    }

    public int getMatchPlayerSlot() {
        return matchPlayerSlot;
    }

    public void setMatchPlayerSlot(int matchPlayerSlot) {
        this.matchPlayerSlot = matchPlayerSlot;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public int getGameScore() {
        return gameScore;
    }

    public void setGameScore(int gameScore) {
        this.gameScore = gameScore;
    }

    public Boolean getIsReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    public Boolean getIsAiPlayer() {
        return isAiPlayer;
    }

    public void setIsAiPlayer(Boolean isAiPlayer) {
        this.isAiPlayer = isAiPlayer;
    }

    public int getPerfectGames() {
        return perfectGames;
    }

    public void setPerfectGames(int perfectGames) {
        this.perfectGames = perfectGames;
    }

    public int getShotTheMoonCount() {
        return shotTheMoonCount;
    }

    public void setShotTheMoonCount(int shotTheMoonCount) {
        this.shotTheMoonCount = shotTheMoonCount;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public Instant getLastPollTime() {
        return lastPollTime;
    }

    public void setLastPollTime(Instant lastPollTime) {
        this.lastPollTime = lastPollTime;
    }

    public void updateLastPollTime() {
        this.lastPollTime = Instant.now();
    }

    // === Optional helpers ===

    public void addToMatchScore(int points) {
        this.matchScore += points;
    }

    public void addToGameScore(int points) {
        this.gameScore += points;
    }

    public void resetGameScore() {
        this.gameScore = 0;
    }

    public void resetReady() {
        this.ready = false;
    }

    public void resetPerfectGames() {
        this.perfectGames = 0;
    }

    public void incrementPerfectGames() {
        this.perfectGames++;
    }

    public void incrementShotTheMoonCount() {
        this.shotTheMoonCount++;
    }

    public void resetShotTheMoonCount() {
        this.shotTheMoonCount = 0;
    }

    public void resetMatchStats() {
        this.setMatchScore(0);
        this.setPerfectGames(0);
        this.setShotTheMoonCount(0);
    }

    // === HAND MANIPULATION HELPERS (PURE STRING ONLY) ===

    public void addCardCodeToHand(String cardCode) {
        if (hand == null || hand.isBlank()) {
            hand = cardCode;
        } else {
            hand += "," + cardCode;
        }
    }

    public boolean removeCardCodeFromHand(String cardCode) {
        if (hand == null || hand.isBlank()) {
            return false;
        }

        String[] cards = hand.split(",");
        StringBuilder newHand = new StringBuilder();
        boolean removed = false;

        for (String card : cards) {
            if (!card.equals(cardCode)) {
                if (newHand.length() > 0) {
                    newHand.append(",");
                }
                newHand.append(card);
            } else {
                removed = true;
            }
        }

        hand = newHand.toString();
        return removed;
    }

    public boolean hasCardCodeInHand(String cardCode) {
        if (hand == null || hand.isBlank()) {
            return false;
        }
        String[] cards = hand.split(",");
        for (String card : cards) {
            if (card.equals(cardCode)) {
                return true;
            }
        }
        return false;
    }

    public int numberOfCardsInHand() {
        if (hand == null || hand.isBlank()) {
            return 0;
        }
        return hand.split(",").length;
    }

    public void clearHand() {
        this.hand = "";
    }

    public boolean isProperHandFormat() {
        if (hand == null || hand.isBlank()) {
            return true; // Empty hand is OK
        }
        return hand.matches(GameConstants.CARD_CODE_REGEX);
    }

    public boolean hasNoDuplicateCards() {
        if (hand == null || hand.isBlank()) {
            return true; // Empty hand, no duplicates
        }
        String[] cards = hand.split(",");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String card : cards) {
            if (!seen.add(card)) {
                return false; // Card already seen -> duplicate
            }
        }
        return true;
    }

    public boolean isValidHand() {
        return isProperHandFormat() && hasNoDuplicateCards();
    }

    public void sortHand() {
        if (hand == null || hand.isBlank()) {
            return;
        }
        String[] cards = hand.split(",");
        java.util.Arrays.sort(cards, java.util.Comparator.comparingInt(CardUtils::calculateCardOrder));
        hand = String.join(",", cards);
    }

    public String getCardsOfSuit(char suit) {
        if (hand == null || hand.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] cards = hand.split(",");

        for (String card : cards) {
            if (!card.isEmpty() && card.charAt(card.length() - 1) == suit) {
                if (result.length() > 0) {
                    result.append(",");
                }
                result.append(card);
            }
        }

        return result.toString();
    }

    public String getCardsNotOfSuit(char suit) {
        if (hand == null || hand.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] cards = hand.split(",");

        for (String card : cards) {
            if (!card.isEmpty() && card.charAt(card.length() - 1) != suit) {
                if (result.length() > 0) {
                    result.append(",");
                }
                result.append(card);
            }
        }

        return result.toString();
    }

    public String[] getHandCardsArray() {
        if (hand == null || hand.isBlank()) {
            return new String[0];
        }
        return hand.split(",");
    }

    public String getInfo() {
        return this.getMatch().getMatchId() + "/" + this.getMatchPlayerSlot();
    }

    public int getPollCounter() {
        return pollCounter;
    }

    public void incrementPollCounter() {
        this.pollCounter++;
    }

    public AiMatchPlayerState getAiMatchPlayerState() {
        return aiMatchPlayerState;
    }

    public void setAiMatchPlayerState(AiMatchPlayerState aiMatchPlayerState) {
        this.aiMatchPlayerState = aiMatchPlayerState;
    }

}
