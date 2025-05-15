package ch.uzh.ifi.hase.soprafs24.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExceptionControllerTest.class)
@Import(GlobalExceptionAdvice.class)
public class GlobalExceptionAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testIllegalArgumentHandled() throws Exception {
        mockMvc.perform(get("/test-exception/illegal-argument"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This is illegal"))
                .andExpect(jsonPath("$.exception").value("IllegalArgumentException"));
    }

    @Test
    public void testGenericExceptionHandled() throws Exception {
        mockMvc.perform(get("/test-exception/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected error"))
                .andExpect(jsonPath("$.exception").value("RuntimeException"));
    }

    @Test
    public void testResponseStatusExceptionHandled() throws Exception {
        mockMvc.perform(get("/test-exception/response-status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad input"))
                .andExpect(jsonPath("$.exception").value("ResponseStatusException"));
    }

    @Test
    public void testGameplayExceptionHandled() throws Exception {
        mockMvc.perform(get("/test-exception/gameplay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.reason").value("You can't play that card"));
    }
}