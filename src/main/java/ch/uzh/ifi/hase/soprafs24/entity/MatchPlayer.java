package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The MATCH_PLAYER relation saves the ids of the players that are in a match,
 * and their card decks.
 */
@Entity
@Table(name = "MATCH_PLAYER")
public class MatchPlayer {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchPlayerId;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private User user;

    @Column(name = "slot", nullable = false)
    private int slot;

    @OneToMany(mappedBy = "matchPlayer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MatchPlayerCards> cardsInHand;

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

    public List<MatchPlayerCards> getCardsInHand() {
        return cardsInHand;
    }

    public void setCardsInHand(List<MatchPlayerCards> cardsInHand) {
        this.cardsInHand = cardsInHand;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
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

    public Boolean isReady() {
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

    public void addCardToHand(String cardCode) {
        if (this.cardsInHand == null) {
            this.cardsInHand = new ArrayList<>();
        }
        this.cardsInHand.add(MatchPlayerCards.of(cardCode, this));
    }

    public void resetMatchStats() {
        this.setMatchScore(0);
        this.setPerfectGames(0);
        this.setShotTheMoonCount(0);
    }
}
