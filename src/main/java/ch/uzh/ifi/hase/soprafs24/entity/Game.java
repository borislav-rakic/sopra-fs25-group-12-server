package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;

@Entity
@Table(name = "GAME")
public class Game {

    // Required by JPA (always keep this)
    public Game() {
        // Default constructor (to distinguish from copy constructor, see below)
    }

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrickPhase trickPhase = TrickPhase.READY;

    @Column(name = "trick_leader_match_player_slot", nullable = false)
    private Integer trickLeaderMatchPlayerSlot = 0;

    @Column(name = "current_trick", length = 32)
    private String currentTrick = ""; // e.g., "2C,3D,QH,AS"

    @Column(name = "previous_trick", length = 32)
    private String previousTrick = ""; // e.g., "2C,3D,QH,AS"

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

    @Column(name = "trick_just_completed_time")
    private Instant trickJustCompletedTime = Instant.now();

    @Column(name = "game_scores_csv")
    private String gameScoresCsv = "0,0,0,0"; // Example: "4,5,3,13"

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

    public TrickPhase getTrickPhase() {
        return trickPhase;
    }

    public void setTrickPhase(TrickPhase trickPhase) {
        this.trickPhase = trickPhase;
    }

    public Instant getTrickJustCompletedTime() {
        return trickJustCompletedTime;
    }

    public void setTrickJustCompletedTime(Instant trickJustCompletedTime) {
        this.trickJustCompletedTime = trickJustCompletedTime;
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

    /******************* CURRENT TRICK *******************************/

    public List<String> getCurrentTrick() {
        if (currentTrick == null || currentTrick.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(currentTrick.split(",")));
    }

    public String getCurrentTrickAsString() {
        return currentTrick;
    }

    public void setCurrentTrick(List<String> cards) {
        this.currentTrick = String.join(",", cards);
    }

    public void addCardCodeToCurrentTrick(String cardCode) {
        if (cardCode == null || !cardCode.matches(GameConstants.CARD_CODE_REGEX)) {
            throw new IllegalArgumentException("Invalid card code format: " + cardCode);
        }

        List<String> cards = getCurrentTrick();
        if (cards.size() >= 4) {
            throw new IllegalStateException("Current trick already has 4 cards.");
        }

        cards.add(cardCode);
        setCurrentTrick(cards);
    }

    public void clearCurrentTrick() {
        this.currentTrick = "";
    }

    public int getCurrentTrickSize() {
        return getCurrentTrick().size();
    }

    public String getCardCodeInCurrentTrick(int index) {
        List<String> cards = getCurrentTrick();
        if (index < 0 || index >= cards.size()) {
            throw new IndexOutOfBoundsException("Invalid card index in current trick: " + index);
        }
        return cards.get(index);
    }

    public String getCardCodeOfFirstCardInCurrentTrick() {
        List<String> cards = getCurrentTrick();
        if (cards.isEmpty()) {
            return "";
        }
        return cards.get(0);
    }

    public String getSuitOfFirstCardInCurrentTrick() {
        List<String> cards = getCurrentTrick();
        if (cards.isEmpty()) {
            return "";
        }

        String card = cards.get(0);
        if (card == null || card.length() < 1) {
            return "";
        }

        return card.substring(card.length() - 1); // e.g., "H"
    }

    /******************* PREVIOUS TRICK *******************************/
    public List<String> getPreviousTrick() {
        if (previousTrick == null || previousTrick.isBlank()) {
            return new ArrayList<>();
        }
        return List.of(previousTrick.split(","));
    }

    public String getPreviousTrickAsString() {
        return previousTrick;
    }

    public void setPreviousTrick(List<String> cards) {
        this.previousTrick = String.join(",", cards);
    }

    @Transient
    public List<Integer> getGameScoresList() {
        if (gameScoresCsv == null || gameScoresCsv.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(gameScoresCsv.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public void setGameScoresList(List<Integer> scores) {
        this.gameScoresCsv = scores.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public int getScoreForSlot(int matchPlayerSlot) {
        List<Integer> scores = getGameScoresList();
        if (matchPlayerSlot < 1 || matchPlayerSlot > scores.size()) {
            throw new IllegalArgumentException("Invalid player slot: " + matchPlayerSlot);
        }
        return scores.get(matchPlayerSlot - 1);
    }

    // Making a Copy

    public Game(Game source) {
        this.currentMatchPlayerSlot = source.currentMatchPlayerSlot;
        this.gameNumber = source.gameNumber + 1;
        this.deckId = source.deckId;
        this.isHeartsBroken = source.isHeartsBroken;
        this.phase = source.phase;
        this.trickPhase = source.trickPhase;
        this.trickLeaderMatchPlayerSlot = source.trickLeaderMatchPlayerSlot;
        this.currentTrick = source.currentTrick;
        this.previousTrick = source.previousTrick;
        this.currentTrickNumber = source.currentTrickNumber;
        this.currentPlayOrder = source.currentPlayOrder;
        this.previousTrickLeaderMatchPlayerSlot = source.previousTrickLeaderMatchPlayerSlot;
        this.previousTrickWinnerMatchPlayerSlot = source.previousTrickWinnerMatchPlayerSlot;
        this.previousTrickPoints = source.previousTrickPoints;
        this.trickJustCompletedTime = source.trickJustCompletedTime;
        this.gameScoresCsv = source.gameScoresCsv;
    }

}
