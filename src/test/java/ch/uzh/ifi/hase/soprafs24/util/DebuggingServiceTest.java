package ch.uzh.ifi.hase.soprafs24.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;

import org.apache.tomcat.jni.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;

@ExtendWith(MockitoExtension.class)
class DebuggingServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @BeforeEach
    void setup() throws Exception {
        // Inject mocks into static fields via reflection (hacky, but needed due to
        // @PostConstruct)
        Field matchField = DebuggingService.class.getDeclaredField("matchRepository");
        matchField.setAccessible(true);
        matchField.set(null, matchRepository);

        Field gameField = DebuggingService.class.getDeclaredField("gameRepository");
        gameField.setAccessible(true);
        gameField.set(null, gameRepository);

        Field statsField = DebuggingService.class.getDeclaredField("gameStatsRepository");
        statsField.setAccessible(true);
        statsField.set(null, gameStatsRepository);
    }

}
