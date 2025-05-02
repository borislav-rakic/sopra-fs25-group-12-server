package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;

@Service
public class HtmlSummaryService {

    public String buildGameResultHtml(Match match, Game game) {
        String html = """
                <div><style>body {font-family: Arial, sans-serif;background-color: #e6f2e6;padding: 20px;}.scoreboard {width: fit-content;border-collapse: collapse;border: 2px solid #4a773c;border-radius: 8px;overflow: hidden;background-color: #ffffff;}.scoreboard th {background-color: #2e5c1f;color: white;padding: 10px 15px;text-align: center;}.scoreboard td {border-top: 1px solid #cfcfcf;padding: 8px 12px;text-align: center;}.player-name {text-align: left;}.total {font-weight: bold;}.note {margin-top: 15px;font-style: italic;background-color: #f0f8f0;padding: 10px;border-left: 4px solid #4a773c;max-width: 500px;}</style><table class=\"scoreboard\"><thead><tr><th>GAME</th><th>1</th><th>2</th><th>3</th><th>4</th><th>| Match</th></tr></thead><tbody><tr><td class=\"player-name\">Jane</td><td>7</td><td>2</td><td>26</td><td>9</td><td class=\"total\">44</td></tr><tr><td class=\"player-name\">Peter</td><td>14</td><td>8</td><td>26</td><td>10</td><td class=\"total\">32</td></tr><tr><td class=\"player-name\">Lisa</td><td>0</td><td>6</td><td>26</td><td>0</td><td class=\"total\">6</td></tr><tr><td class=\"player-name\">AI Lee (medium)</td><td>5</td><td>10</td><td>0</td><td>7</td><td class=\"total\">22</td></tr></tbody></table><div class=\"note\">In Game 4 nobody was able to shoot the moon, but Lisa scored a perfect game, congrats!</div></div>
                """;
        return html;
    }

    public String buildMatchResultHtml(Match match, Game game) {
        String html = """
                <div><style>body{font-family:Arial,sans-serif;background-color:#e6f2e6;padding:20px}.scoreboard{width:fit-content;border-collapse:collapse;border:2px solid #4a773c;border-radius:8px;overflow:hidden;background-color:#fff}.scoreboard th{background-color:#2e5c1f;color:#fff;padding:10px 15px;text-align:center}.scoreboard td{border-top:1px solid #cfcfcf;padding:8px 12px;text-align:center}.player-name{text-align:left}.total{font-weight:bold}.note{margin-top:15px;font-style:italic;background-color:#f0f8f0;padding:10px;border-left:4px solid #4a773c;max-width:600px}</style><table class="scoreboard"><thead><tr><th>GAME</th><th>1</th><th>2</th><th>3</th><th>4</th><th>5</th><th>6</th><th>7</th><th>8</th><th>9</th><th>| Match</th><th>Perfect<br/>Rounds</th><th>Shooting<br/>the Moon</th></tr></thead><tbody><tr><td class="player-name">Jane</td><td>7</td><td>2</td><td>4</td><td>9</td><td>7</td><td>0</td><td>5</td><td>26</td><td>3</td><td class="total">63</td><td>1</td><td>0</td></tr><tr><td class="player-name">Peter</td><td>14</td><td>0</td><td>16</td><td>10</td><td>13</td><td>11</td><td>0</td><td>26</td><td>12</td><td class="total">102</td><td>2</td><td>0</td></tr><tr><td class="player-name">Lisa</td><td>5</td><td>6</td><td>2</td><td>0</td><td>2</td><td>2</td><td>0</td><td>26</td><td>2</td><td class="total">45</td><td>2</td><td>0</td></tr><tr><td class="player-name">AI Lee (medium)</td><td>0</td><td>18</td><td>4</td><td>7</td><td>4</td><td>13</td><td>21</td><td>0</td><td>9</td><td class="total">67</td><td>2</td><td>1</td></tr></tbody></table><div class="note">Lisa wins, CONGRATS!</div></div>
                """;

        return html;
    }
}
