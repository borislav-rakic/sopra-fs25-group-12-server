package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.MatchMessageRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchMessageDTO;

@Service
public class MatchMessageService {

    @Autowired
    private MatchMessageRepository matchMessageRepository;

    public List<MatchMessageDTO> messages(Match match, Game game, MatchPlayer matchPlayer) {
        List<MatchMessage> allMessages = matchMessageRepository.findByMatch(match);

        List<MatchMessageDTO> unseen = new ArrayList<>();

        for (MatchMessage msg : allMessages) {
            if (!msg.hasSeen(matchPlayer.getMatchPlayerSlot())) {
                msg.markSeen(matchPlayer.getMatchPlayerSlot());
                unseen.add(new MatchMessageDTO(msg));
            }
        }

        matchMessageRepository.saveAll(allMessages);
        return unseen;
    }

    public void addMessage(Match match, MatchMessageType type, String content) {
        MatchMessage msg = new MatchMessage();
        msg.setMatch(match);
        msg.setType(type);
        msg.setContent(content);
        matchMessageRepository.save(msg);
    }

    private final Random random = new Random();

    public String getFunMessage(MatchMessageType type) {
        return switch (type) {
            case QUEEN_WARNING -> randomPick(
                    "The Queen of Spades is coming to town!",
                    "She has struck again: the killer Queen of Spades!",
                    "Beware... the Queen is on the prowl!",
                    "The dark lady rides: Queen of Spades played!",
                    "Queen of Spades: chaos mode activated.",
                    "Heads up! The Queen just made her move.",
                    "The Queen of Doom has entered the arena.",
                    "She’s back — and she’s not happy.",
                    "A royal disaster is unfolding!",
                    "The Queen’s kiss is deadly… and it’s been given.",
                    "Brace yourselves... the Queen walks among us.",
                    "An icy chill — the Queen of Spades has been played.",
                    "Boom! The Queen just wrecked someone’s game.",
                    "The dark monarch shows no mercy.",
                    "And just like that, the Queen strikes again!",
                    "Spades? Yes. Queen? Definitely. Trouble? Oh yeah.",
                    "Who dares play the Queen? May luck be with you.",
                    "The royal pain has arrived.",
                    "The Queen has chosen her victim.",
                    "All hail... or maybe fear... the Queen of Spades!");
            case HEARTS_BROKEN -> randomPick(
                    "Hearts broken!",
                    "The floodgates are open — hearts are live.",
                    "You can bleed now: hearts are fair game.",
                    "Someone just broke the love. Hearts in play!");
            case PLAYER_LEFT -> randomPick(
                    "A player has bailed. AI's taking over.",
                    "Somebody rage quit. Say hello to AI Joe.",
                    "Player down. AI Joe steps in.",
                    "Human out. Robot in.");
            // Add more fun cases as needed
            default -> type.name().replace('_', ' ').toLowerCase(); // fallback
        };
    }

    private String randomPick(String... options) {
        return options[random.nextInt(options.length)];
    }

}
