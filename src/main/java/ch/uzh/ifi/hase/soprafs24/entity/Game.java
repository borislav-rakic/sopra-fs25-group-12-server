package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;

@Entity
@Table(name = "GAME")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gameId;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "current_slot", nullable = false)
    private int currentSlot = 0; // values 1–4

    @Column(nullable = false)
    private int gameNumber;

    @Column(name = "deck_id")
    private String deckId;

    @Column(nullable = false)
    private boolean heartsBroken = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase phase = GamePhase.PRESTART;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameStats> playedCards = new ArrayList<>();

    // === Getters and Setters ===

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public int getCurrentSlot() {
        return currentSlot;
    }

    public void setCurrentSlot(int currentSlot) {
        this.currentSlot = currentSlot;
    }

    public int getGameNumber() {
        return gameNumber;
    }

    public void setGameNumber(int gameNumber) {
        this.gameNumber = gameNumber;
    }

    public List<GameStats> getPlayedCards() {
        return playedCards;
    }

    public void setPlayedCards(List<GameStats> playedCards) {
        this.playedCards = playedCards;
    }

    public String getDeckId() {
        return deckId;
    }

    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }

    public Boolean getHeartsBroken() {
        return heartsBroken;
    }

    public void setHeartsBroken(Boolean heartsBroken) {
        this.heartsBroken = heartsBroken;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }
}
