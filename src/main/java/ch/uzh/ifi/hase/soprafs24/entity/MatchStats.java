package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;

@Entity
@Table(name = "MATCH_STATS")
public class MatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User player;

    @Column(nullable = false)
    private int malusPoints;

    @Column(nullable = false)
    private int perfectGames;

    @Column(nullable = false)
    private int shotTheMoonCount;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public User getPlayer() {
        return player;
    }

    public void setPlayer(User player) {
        this.player = player;
    }

    public int getMalusPoints() {
        return malusPoints;
    }

    public void setMalusPoints(int malusPoints) {
        this.malusPoints = malusPoints;
    }

    public int getPerfectGames() {
        return perfectGames;
    }

    public void setPerfectGames(int perfectGames) {
        this.perfectGames = perfectGames;
    }

    public int getShotTheMoonCount() {
        return shotTheMoonCount;
    }

    public void setShotTheMoonCount(int shotTheMoonCount) {
        this.shotTheMoonCount = shotTheMoonCount;
    }

    // Optional: Increment helper methods
    public void addMalusPoints(int points) {
        this.malusPoints += points;
    }

    public void incrementPerfectGames() {
        this.perfectGames++;
    }

    public void incrementShotTheMoon() {
        this.shotTheMoonCount++;
    }
}
