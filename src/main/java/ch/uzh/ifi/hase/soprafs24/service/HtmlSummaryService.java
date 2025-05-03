package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;

@Service
public class HtmlSummaryService {

    public String buildGameResultHtml(Match match, Game game) {
        String html = """
                <div class="class="modalMessage modalMessageGameResult"">
                      <table>
                        <thead>
                          <tr>
                            <th>GAME</th>
                            <th>1</th>
                            <th>2</th>
                            <th>3</th>
                            <th>4</th>
                            <th>| Match</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>Jane</td>
                            <td>7</td>
                            <td>2</td>
                            <td>26</td>
                            <td>9</td>
                            <td>44</td>
                          </tr>
                          <tr>
                            <td>Peter</td>
                            <td>14</td>
                            <td>8</td>
                            <td>26</td>
                            <td>10</td>
                            <td>32</td>
                          </tr>
                          <tr>
                            <td>Lisa</td>
                            <td>0</td>
                            <td>6</td>
                            <td>26</td>
                            <td>0</td>
                            <td>6</td>
                          </tr>
                          <tr>
                            <td>AI Lee (medium)</td>
                            <td>5</td>
                            <td>10</td>
                            <td>0</td>
                            <td>7</td>
                            <td>22</td>
                          </tr>
                        </tbody>
                      </table>
                      <div>
                        In Game 4 nobody was able to shoot the moon, but Lisa scored a perfect game, congrats!
                      </div>
                    </div>
                    """;
        return html;
    }

    public String buildMatchResultHtml(Match match, Game game) {
        String html = """
                <div class="modalMessage modalMessageMatchFinished">
                      <table>
                        <thead>
                          <tr>
                            <th>GAME</th>
                            <th>1</th>
                            <th>2</th>
                            <th>3</th>
                            <th>4</th>
                            <th>5</th>
                            <th>6</th>
                            <th>7</th>
                            <th>8</th>
                            <th>9</th>
                            <th>| Match</th>
                            <th>Perfect<br/>Rounds</th>
                            <th>Shooting<br/>the Moon</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>Jane</td>
                            <td>7</td>
                            <td>2</td>
                            <td>4</td>
                            <td>9</td>
                            <td>7</td>
                            <td>0</td>
                            <td>5</td>
                            <td>26</td>
                            <td>3</td>
                            <td>63</td>
                            <td>1</td>
                            <td>0</td>
                          </tr>
                          <tr>
                            <td>Peter</td>
                            <td>14</td>
                            <td>0</td>
                            <td>16</td>
                            <td>10</td>
                            <td>13</td>
                            <td>11</td>
                            <td>0</td>
                            <td>26</td>
                            <td>12</td>
                            <td>102</td>
                            <td>2</td>
                            <td>0</td>
                          </tr>
                          <tr>
                            <td>Lisa</td>
                            <td>5</td>
                            <td>6</td>
                            <td>2</td>
                            <td>0</td>
                            <td>2</td>
                            <td>2</td>
                            <td>0</td>
                            <td>26</td>
                            <td>2</td>
                            <td>45</td>
                            <td>2</td>
                            <td>0</td>
                          </tr>
                          <tr>
                            <td>AI Lee (medium)</td>
                            <td>0</td>
                            <td>18</td>
                            <td>4</td>
                            <td>7</td>
                            <td>4</td>
                            <td>13</td>
                            <td>21</td>
                            <td>0</td>
                            <td>9</td>
                            <td>67</td>
                            <td>2</td>
                            <td>1</td>
                          </tr>
                        </tbody>
                      </table>
                      <div>
                        Lisa wins, CONGRATS!
                      </div>
                    </div>
                    """;

        return html;
    }
}
