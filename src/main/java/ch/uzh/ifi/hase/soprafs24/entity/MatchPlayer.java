package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
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

    @OneToMany(mappedBy = "matchPlayer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchPlayerCards> cardsInHand;

    @Column(name = "score")
    private int score;

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

    public User getPlayerId() {
        return user;
    }

    public void setPlayerId(User user) {
        this.user = user;
    }

    public List<MatchPlayerCards> getCardsInHand() {
        return cardsInHand;
    }

    public void setCardsInHand(List<MatchPlayerCards> cardsInHand) {
        this.cardsInHand = cardsInHand;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }
}
