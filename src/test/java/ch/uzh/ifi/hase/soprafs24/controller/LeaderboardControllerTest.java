package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaderboardController.class)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private User sampleUser;
    private LeaderboardDTO sampleDTO;

    @BeforeEach
    void setup() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("player1");
        sampleUser.setScoreTotal(100);

        sampleDTO = new LeaderboardDTO();
        sampleDTO.setUsername("player1");
        sampleDTO.setScoreTotal(100);

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("player1");
        sampleUser.setScoreTotal(100);

        sampleDTO = DTOMapper.INSTANCE.convertToLeaderboardDTO(sampleUser); // use real mapper
    }

    @Test
    void getLeaderboard_success() throws Exception {
        Page<User> userPage = new PageImpl<>(List.of(sampleUser));
        Mockito.when(userService.findUsersForLeaderboard(Mockito.anyString(), Mockito.any(Pageable.class)))
                .thenReturn(userPage);

        mockMvc.perform(get("/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("player1")))
                .andExpect(jsonPath("$.content[0].scoreTotal", is(100)));
    }

    @Test
    void getLeaderboard_withFilterAndPagination_success() throws Exception {
        Page<User> userPage = new PageImpl<>(List.of(sampleUser));
        Mockito.when(userService.findUsersForLeaderboard(Mockito.eq("testFilter"), any(Pageable.class)))
                .thenReturn(userPage);

        mockMvc.perform(get("/leaderboard")
                .param("page", "1")
                .param("pageSize", "5")
                .param("sortBy", "scoreTotal")
                .param("order", "asc")
                .param("filter", "testFilter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("player1")));
    }

    @Test
    void populateLeaderboardIfEmpty_shouldPopulate() throws Exception {
        Mockito.when(userService.getUserCount()).thenReturn(5L);
        Mockito.doNothing().when(userService).populateUsersFromSQL();

        mockMvc.perform(post("/leaderboard/populate"))
                .andExpect(status().isOk());

        Mockito.verify(userService).populateUsersFromSQL();
    }

    @Test
    void populateLeaderboardIfEmpty_shouldNotPopulate() throws Exception {
        Mockito.when(userService.getUserCount()).thenReturn(20L);

        mockMvc.perform(post("/leaderboard/populate"))
                .andExpect(status().isOk());

        Mockito.verify(userService, Mockito.never()).populateUsersFromSQL();
    }
}
