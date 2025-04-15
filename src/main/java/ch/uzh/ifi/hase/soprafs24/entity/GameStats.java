package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;

@Entity
@Table(name = "GAME_STATS")
public class GameStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(length = 3, nullable = false)
    private String rankSuit;

    @Column(nullable = false)
    private int playOrder;

    @Column(nullable = false)
    private int playedBy; // 0 = not yet played; 1–4 = player number

    @Column(nullable = false)
    private boolean allowedToPlay;

    @Column(nullable = false)
    private int possibleHolders;

    @Column(nullable = false)
    private int pointsBilledTo;

    @Column(nullable = false)
    private int cardHolder = 0; // 1–4 for players, or 0 for unassigned

    // Enums
    public enum Suit {
        H, S, D, C
    }

    public enum Rank {
        A("A"), _2("2"), _3("3"), _4("4"), _5("5"), _6("6"), _7("7"),
        _8("8"), _9("9"), _0("10"), J("J"), Q("Q"), K("K");

        private final String label;

        Rank(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

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

    public boolean isAllowedToPlay() {
        return allowedToPlay;
    }

    public void setAllowedToPlay(boolean allowedToPlay) {
        this.allowedToPlay = allowedToPlay;
    }

    public int getPossibleHolders() {
        return possibleHolders;
    }

    public void setPossibleHolders(int possibleHolders) {
        this.possibleHolders = possibleHolders;
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
}
