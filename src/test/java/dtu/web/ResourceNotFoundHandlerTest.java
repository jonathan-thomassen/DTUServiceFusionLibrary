package dtu.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import dtu.services.library.errors.ResourceNotFoundException;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResourceNotFoundHandlerTest
{
  private MockMvc mockMvc;

  @BeforeEach
  void setUp()
  {
    mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
        .setControllerAdvice(new ResourceNotFoundHandler()).build();
  }

  @Test
  void noResourceFoundExceptionReturns404() throws Exception
  {
    mockMvc.perform(get("/throws-not-found")).andExpect(status().isNotFound());
  }

  @Test
  void resourceNotFoundExceptionReturns404WithErrorBody() throws Exception
  {
    mockMvc.perform(get("/throws-resource-not-found")).andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.error").value("Employee not found: 99"));
  }

  @Test
  void illegalArgumentExceptionReturns400WithErrorBody() throws Exception
  {
    mockMvc.perform(get("/throws-illegal-arg")).andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.error").value("invalid parameter value"));
  }

  @RestController
  static class ThrowingController
  {
    @GetMapping("/throws-not-found")
    public void handle() throws NoResourceFoundException
    {
      throw new NoResourceFoundException(HttpMethod.GET, "/favicon.ico", "No static resource favicon.ico.");
    }

    @GetMapping("/throws-resource-not-found")
    public void handleResourceNotFound()
    {
      throw new ResourceNotFoundException("Employee not found: 99");
    }

    @GetMapping("/throws-illegal-arg")
    public void handleIllegalArg()
    {
      throw new IllegalArgumentException("invalid parameter value");
    }
  }
}
