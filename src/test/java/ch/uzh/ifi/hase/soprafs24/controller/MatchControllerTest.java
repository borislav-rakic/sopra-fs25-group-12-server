package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteRequestDTO;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        Match match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(new User());
        matchPlayer.setMatch(match);
        matchPlayers.add(matchPlayer);

        match.setMatchId(1L);
        match.setStarted(false);
        match.setMatchPlayers(matchPlayers);
        match.setHost("User");
        match.setLength(100);
        match.setInvites(new HashMap<>());
        match.setAiPlayers(new ArrayList<>());
        match.setPlayer1(new User());

        List<Long> matchPlayerIds = new ArrayList<>();
        matchPlayerIds.add(match.getMatchPlayers().get(0).getMatchPlayerId());

        MatchCreateDTO matchCreateDTO = new MatchCreateDTO();
        matchCreateDTO.setPlayerToken("1234");

        given(matchService.createNewMatch(Mockito.any())).willReturn(match);

        MockHttpServletRequestBuilder postRequest = post("/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(matchCreateDTO))
                .header("Authorization", "Bearer 1234");

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$.started", is(match.getStarted())))
                .andExpect(jsonPath("$.matchPlayerIds", is(matchPlayerIds)));
    }

    @Test
    public void testGetMatchesInformation() throws Exception {
        Match match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(new User());
        matchPlayer.setMatch(match);
        matchPlayers.add(matchPlayer);

        match.setMatchId(1L);
        match.setStarted(false);
        match.setMatchPlayers(matchPlayers);

        List<Long> matchPlayerIds = new ArrayList<>();
        matchPlayerIds.add(match.getMatchPlayers().get(0).getMatchPlayerId());

        List<Match> matches = new ArrayList<>();
        matches.add(match);

        given(matchService.getMatchesInformation()).willReturn(matches);

        MockHttpServletRequestBuilder getRequest = get("/matches");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$[0].started", is(match.getStarted())))
                .andExpect(jsonPath("$[0].matchPlayerIds", is(matchPlayerIds)));
    }

    @Test
    public void testGetMatchInformation() throws Exception {
        Match match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(new User());
        matchPlayer.setMatch(match);
        matchPlayers.add(matchPlayer);

        match.setMatchId(1L);
        match.setStarted(false);
        match.setMatchPlayers(matchPlayers);

        List<Long> matchPlayerIds = new ArrayList<>();
        matchPlayerIds.add(match.getMatchPlayers().get(0).getMatchPlayerId());

        given(matchService.getMatchInformation(Mockito.any())).willReturn(match);

        MockHttpServletRequestBuilder getRequest = get("/matches/1");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$.started", is(match.getStarted())))
                .andExpect(jsonPath("$.matchPlayerIds", is(matchPlayerIds)));
    }

    @Test
    public void testDeleteMatch() throws Exception {
        MockHttpServletRequestBuilder deleteRequest = delete("/matches/1")
                .header("Authorization", "Bearer 1234");

        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void testInvitePlayerToMatch() throws Exception {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(0);
        inviteRequestDTO.setUserId(1L);

        MockHttpServletRequestBuilder postRequest = post("/matches/1/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(inviteRequestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
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
