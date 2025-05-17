package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class MatchSummary {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String matchSummaryHtml; // This is where the lengthy text (HTML) will be stored.

    @Column(columnDefinition = "TEXT")
    private String gameSummaryHtml; // This is where the lengthy text (HTML) will be stored.

    @OneToOne(mappedBy = "matchSummary")
    private Match match;

    @Column
    String matchSummaryMatchPlayerSlot1;

    @Column
    String matchSummaryMatchPlayerSlot2;

    @Column
    String matchSummaryMatchPlayerSlot3;

    @Column
    String matchSummaryMatchPlayerSlot4;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMatchSummaryHtml() {
        return matchSummaryHtml;
    }

    public void setMatchSummaryHtml(String matchSummaryHtml) {
        this.matchSummaryHtml = matchSummaryHtml;
    }

    public String getGameSummaryHtml() {
        return gameSummaryHtml;
    }

    public void setGameSummaryHtml(String gameSummaryHtml) {
        this.gameSummaryHtml = gameSummaryHtml;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public String getMatchSummaryMatchPlayerSlot1() {
        return matchSummaryMatchPlayerSlot1;
    }

    public void setMatchSummaryMatchPlayerSlot1(String matchSummaryMatchPlayerSlot1) {
        this.matchSummaryMatchPlayerSlot1 = matchSummaryMatchPlayerSlot1;
    }

    public String getMatchSummaryMatchPlayerSlot2() {
        return matchSummaryMatchPlayerSlot2;
    }

    public void setMatchSummaryMatchPlayerSlot2(String matchSummaryMatchPlayerSlot2) {
        this.matchSummaryMatchPlayerSlot2 = matchSummaryMatchPlayerSlot2;
    }

    public String getMatchSummaryMatchPlayerSlot3() {
        return matchSummaryMatchPlayerSlot3;
    }

    public void setMatchSummaryMatchPlayerSlot3(String matchSummaryMatchPlayerSlot3) {
        this.matchSummaryMatchPlayerSlot3 = matchSummaryMatchPlayerSlot3;
    }

    public String getMatchSummaryMatchPlayerSlot4() {
        return matchSummaryMatchPlayerSlot4;
    }

    public void setMatchSummaryMatchPlayerSlot4(String matchSummaryMatchPlayerSlot4) {
        this.matchSummaryMatchPlayerSlot4 = matchSummaryMatchPlayerSlot4;
    }
}
