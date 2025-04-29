package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
@Table(name = "GAME_STATS", uniqueConstraints = @UniqueConstraint(columnNames = { "game_id", "rank_suit" }))
public class GameStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2, name = "rank_suit", nullable = false)
    private String rankSuit;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Suit suit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rank rank;

    @Column(nullable = false)
    private int playOrder;

    @Column(nullable = false)
    private int playedBy; // 0 = not yet played; 1–4 = slot number

    @Column(nullable = false)
    private int possibleHolders;

    @Column(nullable = false)
    private int pointsBilledTo;

    @Column(nullable = false)
    private int pointsWorth;

    @Column(nullable = false)
    private int passedBy;

    @Column(nullable = false)
    private int passedTo;

    @Column(nullable = false)
    private int cardHolder = 0; // 1–4 for slots, or 0 for unassigned

    @Column(name = "trick_number", nullable = false)
    private int trickNumber;

    @Column(name = "trick_lead_by_slot", nullable = false)
    private int trickLeadBySlot;

    @Column(name = "card_order", nullable = false)
    private int cardOrder;

    // Automatically set rankSuit on persist/update
    @PrePersist
    @PreUpdate
    private void updateRankSuit() {
        if (this.rank != null && this.suit != null) {
            this.rankSuit = this.rank.toString() + this.suit.name();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Suit getSuit() {
        return suit;
    }

    public void setSuit(Suit suit) {
        this.suit = suit;
        updateRankSuit();
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
        updateRankSuit();
    }

    public String getRankSuit() {
        return rankSuit;
    }

    // RANKSUIT HAS TO BE HANDLED BY FUNCTION
    // SEE BELOW: setCardFromString()

    public int getPlayOrder() {
        return playOrder;
    }

    public void setPlayOrder(int playOrder) {
        this.playOrder = playOrder;
    }

    public boolean isPlayed() {
        return playOrder > 0;
    }

    public int getPlayedBy() {
        return playedBy;
    }

    public void setPlayedBy(int playedBy) {
        this.playedBy = playedBy;
    }

    public int getCardOrder() {
        return cardOrder;
    }

    public void setCardOrder(int cardOrder) {
        this.cardOrder = cardOrder;
    }

    public int getPossibleHolders() {
        return possibleHolders;
    }

    public void setPossibleHolders(int possibleHolders) {
        this.possibleHolders = possibleHolders;
    }

    public int getPointsWorth() {
        return pointsWorth;
    }

    public void setPointsWorth(int pointsWorth) {
        this.pointsWorth = pointsWorth;
    }

    public int getpassedBy() {
        return passedBy;
    }

    public void setPassedBy(int passedBy) {
        this.passedBy = passedBy;
    }

    public int getPassedTo() {
        return passedTo;
    }

    public void setPassedTo(int passedTo) {
        this.passedTo = passedTo;
    }

    public int getPointsBilledTo() {
        return pointsBilledTo;
    }

    public void setPointsBilledTo(int pointsBilledTo) {
        this.pointsBilledTo = pointsBilledTo;
    }

    public void setCardFromString(String rankSuitStr) {
        if (rankSuitStr == null || rankSuitStr.length() < 2 || rankSuitStr.length() > 3) {
            throw new IllegalArgumentException("Invalid rankSuit format: " + rankSuitStr);
        }

        String rankPart = rankSuitStr.substring(0, rankSuitStr.length() - 1);
        String suitPart = rankSuitStr.substring(rankSuitStr.length() - 1);

        this.suit = Suit.valueOf(suitPart);

        for (Rank r : Rank.values()) {
            if (r.toString().equals(rankPart)) {
                this.rank = r;
                updateRankSuit();
                return;
            }
        }

        throw new IllegalArgumentException("Unknown rank: " + rankPart);
    }

    public int getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(int cardHolder) {
        this.cardHolder = cardHolder;
    }

    public int getTrickNumber() {
        return trickNumber;
    }

    public void setTrickNumber(int trickNumber) {
        this.trickNumber = trickNumber;
    }

    public int getTrickLeadBySlot() {
        return trickLeadBySlot;
    }

    public void setTrickLeadBySlot(int trickLeadBySlot) {
        this.trickLeadBySlot = trickLeadBySlot;
    }

    public void setPossibleHolder(int slotNumber) {
        validateSlotNumber(slotNumber);
        int mask = 1 << (slotNumber - 1);
        this.possibleHolders = this.possibleHolders | mask;
    }

    public void clearPossibleHolder(int slotNumber) {
        validateSlotNumber(slotNumber);
        int mask = ~(1 << (slotNumber - 1));
        this.possibleHolders = this.possibleHolders & mask;
    }

    public boolean isPossibleHolder(int slotNumber) {
        validateSlotNumber(slotNumber);
        int mask = 1 << (slotNumber - 1);
        return (this.possibleHolders & mask) != 0;
    }

    public List<Integer> getPossibleHolderList() {
        List<Integer> holders = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            if (isPossibleHolder(i)) {
                holders.add(i);
            }
        }
        return holders;
    }

    private void validateSlotNumber(int slotNumber) {
        if (slotNumber < 1 || slotNumber > 4) {
            throw new IllegalArgumentException("Slot number must be between 1 and 4.");
        }
    }

    public void setOnlyPossibleHolder(int slotNumber) {
        validateSlotNumber(slotNumber);
        int mask = 1 << (slotNumber - 1);
        this.possibleHolders = mask;
    }

}
