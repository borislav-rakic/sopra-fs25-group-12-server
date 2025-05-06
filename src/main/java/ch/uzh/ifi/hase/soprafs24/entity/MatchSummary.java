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
}
