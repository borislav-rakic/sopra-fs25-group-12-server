package ch.uzh.ifi.hase.soprafs24.util;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.PostConstruct;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class DebuggingService {

    @Autowired
    private MatchRepository injectedMatchRepository;

    @Autowired
    private GameRepository injectedGameRepository;

    @Autowired
    private GameStatsRepository injectedGameStatsRepository;

    private static MatchRepository matchRepository;
    private static GameRepository gameRepository;
    private static GameStatsRepository gameStatsRepository;

    @PostConstruct
    private void initStaticRepositories() {
        matchRepository = injectedMatchRepository;
        gameRepository = injectedGameRepository;
        gameStatsRepository = injectedGameStatsRepository;
    }

    private static final String DUMP_FOLDER = "debug_dumps";

    private static final Logger log = LoggerFactory.getLogger(DebuggingService.class);

    private DebuggingService() {
        // Private constructor to prevent instantiation
    }

    // DebuggingService.makeFatDump(String dumpTitle, String info, Long userId,
    // Long matchId, Long gameId);

    public static String makeFatDump(String dumpTitle, String info, Long userId, Long matchId, Long gameId) {
        try {
            File folder = new File(DUMP_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String safeTitle = (dumpTitle == null ? "noTitle" : dumpTitle.replaceAll("[^a-zA-Z0-9-_]", "_"));
            String filename = String.format("%s/fatDump_%s_%s.html", DUMP_FOLDER, timestamp, safeTitle);

            File dumpFile = new File(filename);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(dumpFile))) {
                writer.write(buildHtml(info, userId, matchId, gameId));
            }
            System.out.println("Fat dump created: " + dumpFile.getAbsolutePath());

            cleanupOldDumps(folder);
            return filename;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    private static void cleanupOldDumps(File folder) {
        File[] dumpFiles = folder.listFiles((dir, name) -> name.startsWith("fatDump_") && name.endsWith(".html"));

        if (dumpFiles == null || dumpFiles.length <= 12) {
            return; // Nothing to do
        }

        // Sort files by last modified date (oldest first)
        Arrays.sort(dumpFiles, Comparator.comparingLong(File::lastModified));

        int filesToDelete = dumpFiles.length - 12;
        for (int i = 0; i < filesToDelete; i++) {
            try {
                dumpFiles[i].delete();
            } catch (Exception e) {
                // Best effort deletion, not fatal
                e.printStackTrace();
            }
        }
    }

    private static String buildSection(String title, String content) {
        return "<div class=\"section\">\n<div class=\"title\">" + title + "</div>\n<div class=\"content\">" + content
                + "</div>\n</div>";
    }

    private static String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String buildHtml(String info, Long userId, Long matchId, Long gameId) {
        return """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Fat Dump</title>
                        <style>
                            body { font-family: Arial, sans-serif; margin: 20px; }
                            .section { margin-bottom: 20px; }
                            .title { font-size: 1.5em; font-weight: bold; }
                            .content { background: #f4f4f4; padding: 10px; border-radius: 5px; white-space: pre-wrap; }

                            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                            th, td { border: 1px solid #ccc; padding: 10px; text-align: center; }
                            th { background-color: #f2f2f2; }
                            tr:nth-child(even) { background-color: #fafafa; }
                            img { height: 80px; }
                        </style>

                    </head>
                    <body>
                """
                + buildSection("Info", escapeHtml(info))
                + buildSection("User ID", userId != null ? userId.toString() : "N/A")
                + buildSection("Match ID", matchId != null ? matchId.toString() : "N/A")
                + buildSection("Game ID", gameId != null ? gameId.toString() : "N/A")
                + buildCurrentHands(matchId, gameId)
                + buildSection("Trick Overview", escapeHtml(buildTrickStrings(matchId, gameId)))
                + buildGameVisualization(matchId, gameId) +
                """
                            </body>
                            </html>
                        """;
    }

    private static String buildGameVisualization(Long matchId, Long gameId) {
        // Load entities
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with id " + matchId));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with id " + gameId));

        List<GameStats> allStats = gameStatsRepository.findByGame(game);

        // Now reuse the original HTML builder
        return buildGameVisualizationFromEntities(match, game, allStats);
    }

    // ====== Start of Visualization code inside DebuggingService ======

    private static String buildGameVisualizationFromEntities(Match match, Game game, List<GameStats> allStats) {

        StringBuilder html = new StringBuilder();

        html.append("""
                    <div class="game-visualization">
                        <h2>Game Visualization</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>matchPlayerSlot 1</th>
                                    <th>matchPlayerSlot 2</th>
                                    <th>matchPlayerSlot 3</th>
                                    <th>matchPlayerSlot 4</th>
                                    <th>matchPlayerSlot 1</th>
                                    <th>matchPlayerSlot 2</th>
                                    <th>matchPlayerSlot 3</th>
                                    <th>Summary</th>
                                </tr>
                            </thead>
                            <tbody>
                """);

        Map<Integer, List<GameStats>> tricks = allStats.stream()
                .filter(gs -> gs.isPlayed())
                .collect(Collectors.groupingBy(GameStats::getTrickNumber));

        List<Integer> sortedTricks = new ArrayList<>(tricks.keySet());
        Collections.sort(sortedTricks);

        for (Integer trickNum : sortedTricks) {
            List<GameStats> cardsInTrick = tricks.get(trickNum);
            cardsInTrick.sort(Comparator.comparingInt(GameStats::getPlayOrder));

            String[] matchPlayerSlots = { "", "", "", "", "", "", "" }; // 7 matchPlayerSlots

            int leaderSlot = findTrickLeaderMatchPlayerSlot(game, trickNum, cardsInTrick); // 1-based (1..4)

            // Map leaderSlot to starting index
            int startIndex;
            switch (leaderSlot) {
                case 1 -> startIndex = 0;
                case 2 -> startIndex = 1;
                case 3 -> startIndex = 2;
                case 4 -> startIndex = 3;
                default -> startIndex = 0;
            }

            for (int i = 0; i < cardsInTrick.size(); i++) {
                int matchPlayerSlotIndex = (startIndex + i) % 7; // wrap around 0-6
                matchPlayerSlots[matchPlayerSlotIndex] = renderCard(cardsInTrick.get(i).getRankSuit());
            }

            int totalPoints = cardsInTrick.stream().mapToInt(GameStats::getPointsWorth).sum();
            int billedMatchPlayerSlot = cardsInTrick.stream()
                    .filter(card -> card.getPointsBilledTo() > 0)
                    .findFirst()
                    .map(GameStats::getPointsBilledTo)
                    .orElse(0);

            String summary = totalPoints > 0
                    ? totalPoints + " points to matchPlayerMatchPlayerSlot " + billedMatchPlayerSlot
                    : "No points";

            html.append("<tr>");
            for (String matchPlayerSlot : matchPlayerSlots) {
                html.append("<td>").append(matchPlayerSlot.isEmpty() ? "-" : matchPlayerSlot).append("</td>");
            }
            html.append("<td>").append(summary).append("</td>");
            html.append("</tr>");
        }

        html.append("""
                            </tbody>
                        </table>
                    </div>
                """);

        return html.toString();
    }

    private static String renderCard(String cardCode) {
        if (cardCode == null || cardCode.isEmpty()) {
            return "-";
        }
        return "<img src=\"offline_playing_cards_for_debugging/" + cardCode + ".png\" alt=\"" + cardCode
                + "\" height=\"60\">";
    }

    private static int findTrickLeaderMatchPlayerSlot(Game game, Integer trickNum, List<GameStats> cardsInTrick) {
        return cardsInTrick.stream()
                .filter(gs -> gs.getPlayOrder() == 1)
                .map(GameStats::getPlayedBy)
                .findFirst()
                .orElse(1);
    }

    private static String buildTrickStringsFromEntities(Match match, Game game, List<GameStats> allStats) {
        StringBuilder tricksInfo = new StringBuilder();

        Map<Integer, List<GameStats>> tricks = allStats.stream()
                .filter(GameStats::isPlayed)
                .collect(Collectors.groupingBy(GameStats::getTrickNumber));

        List<Integer> sortedTricks = new ArrayList<>(tricks.keySet());
        Collections.sort(sortedTricks);

        for (Integer trickNum : sortedTricks) {
            List<GameStats> cardsInTrick = tricks.get(trickNum);
            cardsInTrick.sort(Comparator.comparingInt(GameStats::getPlayOrder));

            int leaderMatchPlayerSlot = findTrickLeaderMatchPlayerSlot(game, trickNum, cardsInTrick);

            tricksInfo.append("Trick=").append(trickNum)
                    .append(", LeadingMatchPlayerSlot=").append(leaderMatchPlayerSlot)
                    .append(", playedCards: ");

            List<String> cardDescriptions = new ArrayList<>();

            for (GameStats card : cardsInTrick) {
                String desc = card.getRankSuit() + " (matchPlayerMatchPlayerSlot" + card.getPlayedBy() + ")";
                cardDescriptions.add(desc);
            }

            tricksInfo.append(String.join(", ", cardDescriptions)).append("\n");
        }

        return tricksInfo.toString();
    }

    private static String buildTrickStrings(Long matchId, Long gameId) {
        // Load entities
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with id " + matchId));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with id " + gameId));

        List<GameStats> allStats = gameStatsRepository.findByGame(game);

        return buildTrickStringsFromEntities(match, game, allStats);
    }

    private static String buildCurrentHands(Long matchId, Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with id " + gameId));

        List<GameStats> allStats = gameStatsRepository.findByGame(game);

        // Cards still in players' hands
        Map<Integer, List<String>> handsByMatchPlayerSlot = allStats.stream()
                .filter(gs -> gs.getPlayedBy() == 0) // not played yet
                .collect(Collectors.groupingBy(
                        GameStats::getCardHolder,
                        Collectors.mapping(GameStats::getRankSuit, Collectors.toList())));

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"game-visualization\">\n<h2>Current Hands</h2>\n<table>\n<thead><tr>");
        html.append("<th>MatchPlayerSlot</th><th>Cards in Hand</th>");
        html.append("</tr></thead>\n<tbody>\n");

        for (int matchPlayerSlot = 1; matchPlayerSlot <= 4; matchPlayerSlot++) {
            List<String> cards = handsByMatchPlayerSlot.getOrDefault(matchPlayerSlot, Collections.emptyList());

            // Sort cards nicely (optional)
            Collections.sort(cards);

            html.append("<tr>");
            html.append("<td>").append("MatchPlayerSlot ").append(matchPlayerSlot).append("</td>");
            html.append("<td>");
            for (String card : cards) {
                html.append(renderCard(card)).append(" ");
            }
            html.append("</td>");
            html.append("</tr>\n");
        }

        html.append("</tbody>\n</table>\n</div>");

        return html.toString();
    }

    /**
     * Logs rich contextual information about the game state.
     *
     * @param user        the user (can be null)
     * @param match       the match (can be null)
     * @param game        the game (can be null)
     * @param matchPlayer the player (can be null)
     * @param cardCode    the card played (can be null)
     * @param contextMsg  optional message to include
     */
    public static void richLog(User user, Match match, Game game, MatchPlayer matchPlayer, String cardCode,
            String contextMsg) {
        StringBuilder sb = new StringBuilder();

        // Caller context
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement caller = (stackTrace.length > 2) ? stackTrace[2] : null;

        sb.append("\n🎮 === RICH LOG START ===\n");

        if (contextMsg != null && !contextMsg.isBlank()) {
            sb.append("📝 Context     : ").append(contextMsg).append("\n");
        }

        if (caller != null) {
            sb.append("📍 Called From : ")
                    .append(caller.getClassName()).append(".")
                    .append(caller.getMethodName()).append("() : line ")
                    .append(caller.getLineNumber()).append("\n");
        }

        // User
        if (user != null) {
            sb.append("👤 User        : ").append(user.getUsername())
                    .append(" (ID: ").append(user.getId()).append(")\n");
        } else {
            sb.append("👤 User        : null\n");
        }

        // Card
        sb.append("🃏 Card Played : ").append(cardCode != null ? cardCode : "null").append("\n");

        // Match
        if (match != null) {
            sb.append("🎯 Match       : ID=").append(match.getMatchId())
                    .append(", Phase=").append(match.getPhase())
                    .append(", Started=").append(match.getStarted()).append("\n");
        } else {
            sb.append("🎯 Match       : null\n");
        }

        // Game
        if (game != null) {
            sb.append("🎲 Game        : ID=").append(game.getGameId())
                    .append(", Number=").append(game.getGameNumber())
                    .append(", Phase=").append(game.getPhase())
                    .append(", Current MatchPlayerSlot=").append(game.getCurrentMatchPlayerSlot())
                    .append(", Trick Size=").append(game.getCurrentTrickSize()).append("\n");
            sb.append("📈 Trick       : ").append(game.getCurrentTrick()).append("\n");
        } else {
            sb.append("🎲 Game        : null\n");
        }

        // Player
        if (matchPlayer != null) {
            sb.append("🎮 MatchPlayer : MatchPlayerSlot=").append(matchPlayer.getMatchPlayerSlot())
                    .append(", Score=").append(matchPlayer.getGameScore())
                    .append(", Hand=").append(matchPlayer.getHand()).append("\n");
        } else {
            sb.append("🎮 MatchPlayer : null\n");
        }

        sb.append("🎮 === RICH LOG END ===\n");

        log.info(sb.toString());
    }

    /**
     * Overloaded version with no context message.
     */
    public static void richLog(User user, Match match, Game game, MatchPlayer matchPlayer, String cardCode) {
        richLog(user, match, game, matchPlayer, cardCode, null);
    }

    public static int matchPlayerSlotToPlayerSlot(int matchPlayerSlot) {
        if (matchPlayerSlot < 1 || matchPlayerSlot > 3) {
            return -1;
        }
        int playerSlot = matchPlayerSlot - 1;
        return playerSlot;
    }
}
