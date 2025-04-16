package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;

@Entity
@Table(name = "passed_card", uniqueConstraints = @UniqueConstraint(columnNames = { "game_id", "rank_suit" }))
public class PassedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "game_id")
    private Game game;

    @Column(name = "rank_suit", length = 2, nullable = false)
    private String rankSuit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_player")
    private User fromPlayer;

    @Column(name = "game_number", nullable = false)
    private int gameNumber;

    public PassedCard() {
    }

    public PassedCard(Game game, String rankSuit, User fromPlayer, int gameNumber) {
        this.game = game;
        this.rankSuit = rankSuit;
        this.fromPlayer = fromPlayer;
        this.gameNumber = gameNumber;
    }

    // === Getters and Setters ===

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

    public String getRankSuit() {
        return rankSuit;
    }

    public void setRankSuit(String rankSuit) {
        this.rankSuit = rankSuit;
    }

    public User getFromPlayer() {
        return fromPlayer;
    }

    public void setFromPlayer(User fromPlayer) {
        this.fromPlayer = fromPlayer;
    }

    public int getGameNumber() {
        return gameNumber;
    }

    public void setGameNumber(int gameNumber) {
        this.gameNumber = gameNumber;
    }
}
