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
            int slot = matchPlayer.getMatchPlayerSlot();

            if (!msg.hasSeen(slot)) {
                msg.markSeen(slot);

                String content = msg.getContent();
                if (content != null && !content.trim().isEmpty()) {
                    unseen.add(new MatchMessageDTO(msg));
                }
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
                    "Someone just broke the love. Hearts in play!",
                    "The age of kindness is over. Let the pain begin.",
                    "No more playing nice — hearts are now in season.",
                    "Somebody just shattered the peace.",
                    "Let the heartbreak begin.",
                    "Hope you’re ready — hearts are now scoring.",
                    "The gloves are off. Hearts are hot.",
                    "Cupid’s crying... hearts are broken.",
                    "Someone broke the seal — hearts unleashed!",
                    "The red storm has been released.",
                    "Brace yourselves. The heartbreak is real.",
                    "A heart has been played — and it’s just the beginning.",
                    "The love is gone. Score damage incoming.",
                    "Here come the heartbreakers!",
                    "Somebody just turned love into points.",
                    "The bleeding starts now — hearts are in.",
                    "Romance is over. It’s every player for themselves!");
            case GAME_STARTED -> randomPick(
                    "Shuffling complete — let the madness begin!",
                    "The cards have spoken. It’s time to duel!",
                    "Brace yourselves... the heartache begins.",
                    "Let the wrangling commence!",
                    "First trick incoming. Eyes on the prize.",
                    "Grab your cards — it’s go time!",
                    "The deck is ready. Are you?",
                    "Game on — may the odds be ever in your favor.",
                    "Shuffle up, hearts down — let’s ride.",
                    "The table is hot. Who will strike first?",
                    "All players ready. Battle begins!",
                    "Deal me in. Let’s do this.",
                    "It’s hearts season — let the hunt begin.",
                    "They came for blood. You came for points.",
                    "The showdown begins. Watch your back.",
                    "Let the strategy — and the sabotage — begin!",
                    "War of hearts has begun!",
                    "Fate is shuffled. Let’s play some cards!");
            case PLAYER_JOINED -> randomPick(
                    "A new Player has joined.");
            case PLAYER_LEFT -> randomPick(
                    "A Player has deided to leave the game.");
            // Add more fun cases as needed
            default -> ""; // fallback
        };
    }

    public String getFunMessage(MatchMessageType type, String who) {
        return switch (type) {
            case PLAYER_JOINED -> randomPick(
                    String.format("%s has joined the ranks!", who),
                    String.format("No worries, %s is ready to fight the good fight!", who),
                    String.format("Get ready, %s just entered the game!", who),
                    String.format("Make room — %s is here to play!", who),
                    String.format("A new contender appears: %s!", who),
                    String.format("Brace yourselves — %s has arrived.", who),
                    String.format("%s has entered the battlefield!", who),
                    String.format("Watch out! %s means business.", who),
                    String.format("%s has joined the table. Let the games begin!", who),
                    String.format("Fresh blood! Welcome, %s.", who),
                    String.format("Who’s that? Oh — just %s showing up to win.", who),
                    String.format("And here comes %s, bold and ready!", who),
                    String.format("Deal ’em in! %s is sitting down.", who),
                    String.format("A wild %s appears!", who),
                    String.format("%s is stepping into the arena.", who),
                    String.format("All eyes on %s — the newcomer.", who),
                    String.format("Don’t underestimate %s — rookie with style!", who),
                    String.format("Looks like %s brought their A-game.", who),
                    String.format("New challenger unlocked: %s!", who));
            case PLAYER_LEFT -> randomPick(
                    String.format("%s has decided to step aside.", who),
                    String.format("%s has left the table — the game goes on!", who),
                    String.format("%s took their cards and walked away.", who),
                    String.format("Poof! %s has vanished into the mist.", who),
                    String.format("%s has bowed out of the game.", who),
                    String.format("%s has left the building.", who),
                    String.format("%s has quit — maybe they’ll be back.", who),
                    String.format("Looks like %s has called it a day.", who),
                    String.format("And just like that, %s is gone.", who),
                    String.format("%s folded... in spirit.", who),
                    String.format("Goodbye, %s — thanks for the cards!", who),
                    String.format("We lost a player — %s has exited.", who),
                    String.format("%s has left. Was it the Queen of Spades?", who),
                    String.format("The table feels emptier without %s.", who),
                    String.format("%s rage quit? Who knows...", who),
                    String.format("%s hit the eject button!", who),
                    String.format("Another one bites the dust — goodbye, %s.", who),
                    String.format("%s has logged out of the madness.", who),
                    String.format("%s walked away. Maybe it’s for the best.", who),
                    String.format("One less rival — %s has departed.", who));
            case QUEEN_WARNING -> randomPick(
                    "Queen of Spades has been played!");
            case HEARTS_BROKEN -> randomPick(
                    "Hearts is broken!");
            case GAME_STARTED -> randomPick(
                    "The Game is on!");
            // Add more cases as needed
            default -> ""; // Fallback if type not supported
        };
    }

    private String randomPick(String... options) {
        return options[random.nextInt(options.length)];
    }

}
