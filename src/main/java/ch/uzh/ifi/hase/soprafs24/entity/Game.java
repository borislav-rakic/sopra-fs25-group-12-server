package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "GAME")
public class Game {

    private static final Logger log = LoggerFactory.getLogger(Game.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gameId;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "current_match_player_slot", nullable = false)
    private int currentMatchPlayerSlot = 1; // values 1–4

    @Column(nullable = false)
    private int gameNumber;

    @Column(name = "deck_id")
    private String deckId;

    @Column(nullable = false)
    private boolean isHeartsBroken = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase phase = GamePhase.PRESTART;

    @Column(name = "trick_leader_match_player_slot", nullable = false)
    private Integer trickLeaderMatchPlayerSlot = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "current_trick", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "card_code")
    private Set<String> currentTrick = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "previous_trick", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "card_code")
    private Set<String> previousTrick = new LinkedHashSet<>();

    @Column(nullable = false)
    private int currentTrickNumber = 0;

    @Column(nullable = false)
    private int currentPlayOrder = 0;

    @Column(name = "previous_trick_leader_match_player_slot")
    private Integer previousTrickLeaderMatchPlayerSlot;

    @Column(name = "previous_trick_winner_match_player_slot")
    private Integer previousTrickWinnerMatchPlayerSlot;

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

    public int getCurrentMatchPlayerSlot() {
        return currentMatchPlayerSlot;
    }

    public void setCurrentMatchPlayerSlot(int currentMatchPlayerSlot) {
        this.currentMatchPlayerSlot = currentMatchPlayerSlot;
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

    public String getCurrentTrickAsString() {
        List<String> trick = this.getCurrentTrick();
        return trick.stream().collect(Collectors.joining(","));
    }

    public List<String> getPreviousTrick() {
        return new ArrayList<>(previousTrick);
    }

    public void setPreviousTrick(List<String> previousTrick) {
        this.previousTrick = new LinkedHashSet<>(previousTrick);
    }

    public Integer getTrickLeaderMatchPlayerSlot() {
        return trickLeaderMatchPlayerSlot;
    }

    public void setTrickLeaderMatchPlayerSlot(int trickLeaderMatchPlayerSlot) {
        this.trickLeaderMatchPlayerSlot = trickLeaderMatchPlayerSlot;
    }

    public Integer getPreviousTrickLeaderMatchPlayerSlot() {
        return previousTrickLeaderMatchPlayerSlot;
    }

    public void setPreviousTrickLeaderMatchPlayerSlot(Integer previousTrickLeaderMatchPlayerSlot) {
        this.previousTrickLeaderMatchPlayerSlot = previousTrickLeaderMatchPlayerSlot;
    }

    public List<Integer> getTrickMatchPlayerSlotOrder() {
        if (trickLeaderMatchPlayerSlot == null || trickLeaderMatchPlayerSlot < 1 || trickLeaderMatchPlayerSlot > 4) {
            throw new IllegalStateException(
                    "Invalid or unset trickLeaderMatchPlayerSlot: " + trickLeaderMatchPlayerSlot);
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int matchPlayerSlot = ((trickLeaderMatchPlayerSlot - 1 + i) % 4) + 1;
            order.add(matchPlayerSlot);
        }
        return order;
    }

    public List<Integer> getPreviousTrickMatchPlayerSlotOrder() {
        if (previousTrickLeaderMatchPlayerSlot == null || previousTrickLeaderMatchPlayerSlot < 1
                || previousTrickLeaderMatchPlayerSlot > 4) {
            throw new IllegalStateException(
                    "Invalid or unset previousTrickLeaderMatchPlayerSlot: " + previousTrickLeaderMatchPlayerSlot);
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int matchPlayerSlot = ((previousTrickLeaderMatchPlayerSlot - 1 + i) % 4) + 1;
            order.add(matchPlayerSlot);
        }
        return order;
    }

    public String getTrickMatchPlayerSlotOrderAsString() {
        return getTrickMatchPlayerSlotOrder().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
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

    public Integer getPreviousTrickWinnerMatchPlayerSlot() {
        return previousTrickWinnerMatchPlayerSlot;
    }

    public void setPreviousTrickWinnerMatchPlayerSlot(Integer previousTrickWinnerMatchPlayerSlot) {
        this.previousTrickWinnerMatchPlayerSlot = previousTrickWinnerMatchPlayerSlot;
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

    public void emptyCurrentTrick() {
        if (this.currentTrick != null) {
            this.currentTrick.clear();
        }
    }

    public void updatePhaseBasedOnPlayOrder() {
        if (currentPlayOrder >= 49) {
            this.phase = GamePhase.FINALTRICK;
            log.info("GamePhase set to FINALTRICK (playOrder = {}).", currentPlayOrder);
        } else if (currentPlayOrder >= 5) {
            this.phase = GamePhase.NORMALTRICK;
            log.info("GamePhase set to NORMALTRICK (playOrder = {}).", currentPlayOrder);
        } else {
            log.info("GamePhase remains unchanged (playOrder = {}).", currentPlayOrder);
        }
    }
}
