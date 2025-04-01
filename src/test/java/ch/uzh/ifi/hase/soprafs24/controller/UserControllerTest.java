package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserCreateDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    user.setPassword("ThisIsMyBigSecret");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        // .andExpect(jsonPath("$[0].name", is(user.getName())))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setPassword("ThisIsMyBigSecret");
    user.setUsername("testUsername");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    UserCreateDTO userCreateDTO = new UserCreateDTO();
    userCreateDTO.setUsername("testUsername");
    userCreateDTO.setPassword("ThisIsMyBigSecret");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userCreateDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
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

  // Added test @diderot5038, 2025-03-29.
  @Test
  public void createUser_validInput() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setPassword("testPassword");
    user.setUsername("testUsername");
    user.setStatus(UserStatus.ONLINE);
    user.setToken("sampleToken123");

    UserCreateDTO userCreateDTO = new UserCreateDTO();
    userCreateDTO.setPassword("testPassword");
    userCreateDTO.setUsername("testUsername");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userCreateDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())))
        .andExpect(jsonPath("$.token", is(user.getToken())))

    ;
  }

  // Testing Username conflict when adding user through POST call.
  // fail .... 409 isConflict . adding taken username with POST to /users
  @Test
  public void test2_createUser_existingUsername() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setPassword("testPassword");
    user.setUsername("testUsername");
    user.setToken("testingToken 12345");
    user.setStatus(UserStatus.ONLINE);

    UserCreateDTO userCreateDTO = new UserCreateDTO();
    userCreateDTO.setPassword("testPassword");
    userCreateDTO.setUsername("testUsername");

    // The first call of createUser works.
    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userCreateDTO));
    // then

    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.token", is(user.getToken())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
    // The second call fails, since the username is already taken.
    given(userService.createUser(Mockito.any()))
        .willThrow(new ResponseStatusException(HttpStatus.CONFLICT,
            "Username already exists"));

    MockHttpServletRequestBuilder postAdditionalRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userCreateDTO));
    mockMvc.perform(postAdditionalRequest)
        .andExpect(status().isConflict());
  }

  // Tests GET request at /users/1
  // success . 200 isOk ....... when retrieving user w GET to /users/5
  @Test
  public void getUserInformation_success() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setPassword("password");
    user.setUsername("testUsername");
    user.setToken("5L");
    user.setStatus(UserStatus.ONLINE);

    given(userService.getUserById(Mockito.any())).willReturn(user);

    MockHttpServletRequestBuilder getRequest = get("/users/5").contentType(MediaType.APPLICATION_JSON);

    mockMvc.perform(getRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.birthday", is(user.getBirthday())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  // Try to GET /users/99 when userId 99 is not in DB.
  // fail .... 404 isNotFound . when retrieving user w GET to /users/99
  @Test
  public void getUserInformation_noUserWithIdExists() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setUsername("testUsername");
    user.setPassword("testPassword");
    user.setToken("69eca0c3-6a53-47eb-8aab-20e3800d6771");
    user.setStatus(UserStatus.ONLINE);

    given(userService.getUserById(Mockito.any()))
        .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no user with id=99."));

    MockHttpServletRequestBuilder getRequest = get("/users/99").contentType(MediaType.APPLICATION_JSON);

    mockMvc.perform(getRequest)
        .andExpect(status().isNotFound());
  }

}