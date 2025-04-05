package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchCreateDTO;
import ch.uzh.ifi.hase.soprafs24.service.MatchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.contains;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MatchControllerTest
 * This is a WebMvcTest which allows to test the MatchController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the MatchController works.
 */
@WebMvcTest(MatchController.class)
public class MatchControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @Test
    public void testCreateMatch() throws Exception {
        System.out.println("TESTINGCONTROLLER");
        //given
        List<Long> playerIdList = new ArrayList<>();
        playerIdList.add(1L);
        playerIdList.add(null);
        playerIdList.add(null);
        playerIdList.add(null);

        Match match = new Match();
        match.setMatchId(1L);
        match.setStarted(false);
        match.setPlayerIds(playerIdList);
        match.setHost("User");
        match.setLength(100);
        match.setInvites(new HashMap<>());
        match.setAiPlayers(new ArrayList<>());

        MatchCreateDTO matchCreateDTO = new MatchCreateDTO();
        matchCreateDTO.setPlayerToken("1234");

        given(matchService.createNewMatch(Mockito.any())).willReturn(match);

        MockHttpServletRequestBuilder postRequest = post("/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(matchCreateDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$.started", is(match.getStarted())))
                .andExpect(jsonPath("$.playerIds", contains(is(1), Matchers.nullValue(), Matchers.nullValue(), Matchers.nullValue())));
    }

    @Test
    public void testGetMatchesInformation() throws Exception {
        //given
        List<Long> playerIdList = new ArrayList<>();
        playerIdList.add(1L);
        playerIdList.add(null);
        playerIdList.add(null);
        playerIdList.add(null);

        Match match = new Match();
        match.setMatchId(1L);
        match.setStarted(false);
        match.setPlayerIds(playerIdList);

        List<Match> matches = new ArrayList<>();
        matches.add(match);

        given(matchService.getMatchesInformation()).willReturn(matches);

        MockHttpServletRequestBuilder getRequest = get("/matches");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$[0].started", is(match.getStarted())))
                .andExpect(jsonPath("$[0].playerIds", contains(is(1), Matchers.nullValue(), Matchers.nullValue(), Matchers.nullValue())));
    }

    @Test
    public void testGetMatchInformation() throws Exception {
        //given
        List<Long> playerIdList = new ArrayList<>();
        playerIdList.add(1L);
        playerIdList.add(null);
        playerIdList.add(null);
        playerIdList.add(null);

        Match match = new Match();
        match.setMatchId(1L);
        match.setStarted(false);
        match.setPlayerIds(playerIdList);

        given(matchService.getMatchInformation(Mockito.any())).willReturn(match);

        MockHttpServletRequestBuilder getRequest = get("/matches/1");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$.started", is(match.getStarted())))
                .andExpect(jsonPath("$.playerIds", contains(is(1), Matchers.nullValue(), Matchers.nullValue(), Matchers.nullValue())));
    }

    /**
     * Helper Method to convert userPostDTO into a JSON string such that the input
     * can be processed
     * Input will look like this: {"playerToken": "1234"}
     *
     * @param object Input
     * @return string
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
