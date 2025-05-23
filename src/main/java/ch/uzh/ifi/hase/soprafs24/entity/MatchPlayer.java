package ch.uzh.ifi.hase.soprafs24.entity;

import java.time.Instant;

import javax.persistence.*;

import ch.uzh.ifi.hase.soprafs24.constant.AiMatchPlayerState;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;

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

    /**
     * Absolute slot number assigned to the player in the match (1 to 4).
     * Used on the backend to identify who plays when.
     * 
     * Note: On the frontend, this slot is rotated so that the pollling/local player
     * is always in position 0.
     */

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

    @Column
    boolean isHost = false;

    @Column // Not persisted in DB unless you want it to be
    private String takenCards;

    private int rankingInMatch;

    private int rankingInGame;

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
        return hand;
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

    public void incrementShotTheMoonCount() {
        this.shotTheMoonCount++;
    }

    public void resetShotTheMoonCount() {
        this.shotTheMoonCount = 0;
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

    public boolean getIsHost() {
        return isHost;
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
    }

    public String getTakenCards() {
        return takenCards;
    }

    public void setTakenCards(String takenCards) {
        this.takenCards = takenCards;
    }

    public int getRankingInMatch() {
        return rankingInMatch;
    }

    public void setRankingInMatch(int rankingInMatch) {
        this.rankingInMatch = rankingInMatch;
    }

    public int getRankingInGame() {
        return rankingInGame;
    }

    public void setRankingInGame(int rankingInGame) {
        this.rankingInGame = rankingInGame;
    }

}
