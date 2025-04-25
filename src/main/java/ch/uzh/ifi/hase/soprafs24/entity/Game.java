package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    @Column(name = "trick_leader_slot")
    private Integer trickLeaderSlot;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "current_trick", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "card_code")
    private Set<String> currentTrick = new LinkedHashSet<>();

    @Column(nullable = false)
    private int currentTrickNumber = 0;

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

    public List<String> getCurrentTrick() {
        return new ArrayList<>(currentTrick); // preserve order
    }

    public void setCurrentTrick(List<String> currentTrick) {
        this.currentTrick = new LinkedHashSet<>(currentTrick); // preserve order & remove duplicates
    }

    public Integer getTrickLeaderSlot() {
        return trickLeaderSlot;
    }

    public void setTrickLeaderSlot(Integer trickLeaderSlot) {
        this.trickLeaderSlot = trickLeaderSlot;
    }

    public int getCurrentTrickNumber() {
        return currentTrickNumber;
    }

    public void setCurrentTrickNumber(int currentTrickNumber) {
        this.currentTrickNumber = currentTrickNumber;
    }

    // === Game logic convenience methods ===

    public void addCardToCurrentTrick(String cardCode) {
        if (currentTrick.size() >= 4) {
            throw new IllegalStateException("Current trick already has 4 cards");
        }
        currentTrick.add(cardCode);
    }

    public int getCurrentTrickSize() {
        return currentTrick.size();
    }

    public String getCardInCurrentTrick(int index) {
        List<String> trickAsList = new ArrayList<>(currentTrick);
        if (index < 0 || index >= trickAsList.size()) {
            throw new IndexOutOfBoundsException("No card at index " + index);
        }
        return trickAsList.get(index);
    }

    public void clearCurrentTrick() {
        currentTrick.clear();
    }

}
