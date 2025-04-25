package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class GameResultDTO {

    private Long matchId;
    private int gameNumber;
    private List<PlayerScore> playerScores;

    public static class PlayerScore {
        private String username;
        private int totalScore;
        private int pointsThisGame;

        // getters and setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
        }

        public int getPointsThisGame() {
            return pointsThisGame;
        }

        public void setPointsThisGame(int pointsThisGame) {
            this.pointsThisGame = pointsThisGame;
        }
    }

    // getters and setters
    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public int getGameNumber() {
        return gameNumber;
    }

    public void setGameNumber(int gameNumber) {
        this.gameNumber = gameNumber;
    }

    public List<PlayerScore> getPlayerScores() {
        return playerScores;
    }

    public void setPlayerScores(List<PlayerScore> playerScores) {
        this.playerScores = playerScores;
    }
}
