package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;

@Service
public class HtmlSummaryService {

    public String buildGameResultHtml(Match match, Game game) {
        String html = "<div>GAME OVER</div>";
        return html;
    }

    public String buildMatchResultHtml(Match match, Game game) {
        String html = "<div>MATCH OVER</div>";
        return html;
    }
}
