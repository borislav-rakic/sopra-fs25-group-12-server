package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;

@Entity
@Table(name = "MATCH_PLAYER_CARDS")
public class MatchPlayerCards {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchPlayerCardsId;

    @Column
    private String card;

    @ManyToOne
    @JoinColumn(name = "match_player_id")
    private MatchPlayer matchPlayer;

    public void setMatchPlayerCardsId(Long matchPlayerCardsId) {
        this.matchPlayerCardsId = matchPlayerCardsId;
    }

    public Long getMatchPlayerCardsId() {
        return matchPlayerCardsId;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getCard() {
        return card;
    }

    public void setMatchPlayer(MatchPlayer matchPlayer) {
        this.matchPlayer = matchPlayer;
    }

    public MatchPlayer getMatchPlayer() {
        return matchPlayer;
    }
}
