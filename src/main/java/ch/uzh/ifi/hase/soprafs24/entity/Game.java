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
    private boolean isHeartsBroken = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase phase = GamePhase.PRESTART;

    @Column(name = "trick_leader_slot", nullable = false)
    private Integer trickLeaderSlot = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "current_trick", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "card_code")
    private Set<String> currentTrick = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "previous_trick", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "card_code")
    private Set<String> previousTrick = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "current_trick_slots", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "slot_number")
    private Set<Integer> currentTrickSlots = new LinkedHashSet<>();

    @Column(nullable = false)
    private int currentTrickNumber = 0;

    @Column(nullable = false)
    private int currentPlayOrder = 0;

    @Column(name = "previous_trick_winner_slot")
    private int previousTrickWinnerSlot;

    @Column(name = "previous_trick_points")
    private int previousTrickPoints;
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameStats> gameStats = new ArrayList<>();

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
        return isHeartsBroken;
    }

    public void setHeartsBroken(Boolean isHeartsBroken) {
        this.isHeartsBroken = isHeartsBroken;
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

    public List<String> getPreviousTrick() {
        return new ArrayList<>(previousTrick);
    }

    public void setPreviousTrick(List<String> previousTrick) {
        this.previousTrick = new LinkedHashSet<>(previousTrick);
    }

    public List<Integer> getCurrentTrickSlots() {
        return new ArrayList<>(currentTrickSlots);
    }

    public void setCurrentTrickSlots(List<Integer> currentTrickSlots) {
        this.currentTrickSlots = new LinkedHashSet<>(currentTrickSlots);
    }

    public Integer getTrickLeaderSlot() {
        return trickLeaderSlot;
    }

    public void setTrickLeaderSlot(Integer trickLeaderSlot) {
        this.trickLeaderSlot = trickLeaderSlot;

        if (trickLeaderSlot != null) {
            List<Integer> orderedSlots = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int slot = ((trickLeaderSlot - 1 + i) % 4) + 1;
                orderedSlots.add(slot);
            }
            this.currentTrickSlots = new LinkedHashSet<>(orderedSlots);
        }
    }

    public int getCurrentTrickNumber() {
        return currentTrickNumber;
    }

    public void setCurrentTrickNumber(int currentTrickNumber) {
        this.currentTrickNumber = currentTrickNumber;
    }

    public int getCurrentPlayOrder() {
        return currentPlayOrder;
    }

    public void setCurrentPlayOrder(int currentPlayOrder) {
        this.currentPlayOrder = currentPlayOrder;
    }

    public int getPreviousTrickWinnerSlot() {
        return previousTrickWinnerSlot;
    }

    public void setPreviousTrickWinnerSlot(int previousTrickWinnerSlot) {
        this.previousTrickWinnerSlot = previousTrickWinnerSlot;
    }

    public int getPreviousTrickPoints() {
        return previousTrickPoints;
    }

    public void setPreviousTrickPoints(int previousTrickPoints) {
        this.previousTrickPoints = previousTrickPoints;
    }

    public List<GameStats> getGameStats() {
        return gameStats;
    }

    public void setGameStats(List<GameStats> gameStats) {
        this.gameStats = gameStats;
    }
    // === Game logic convenience methods ===

    public void addCardToCurrentTrick(String cardCode, int slot) {
        if (currentTrick.size() >= 4) {
            throw new IllegalStateException("Current trick already has 4 cards");
        }

        currentTrick.add(cardCode);
        currentTrickSlots.add(slot);
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

    public void emptyCurrentTrick() {
        if (this.currentTrick != null) {
            this.currentTrick.clear();
        }
    }
}
