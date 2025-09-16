package io.spring.api;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.UserQueryService;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurrentUserApi.class)
@Import({
  WebSecurityConfig.class,
  JacksonCustomizations.class,
  UserService.class,
  ValidationAutoConfiguration.class,
  BCryptPasswordEncoder.class
})
public class CurrentUserApiTest extends TestWithCurrentUser {

  @Autowired private MockMvc mvc;

  @MockBean private UserQueryService userQueryService;

  @Autowired private ObjectMapper objectMapper;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void should_get_current_user_with_token() throws Exception {
    when(userQueryService.findById(any())).thenReturn(Optional.of(userData));

    mvc.perform(get("/user")
            .header("Authorization", "Token " + token)
            .contentType("application/json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value(email))
        .andExpect(jsonPath("$.user.username").value(username))
        .andExpect(jsonPath("$.user.bio").value(""))
        .andExpect(jsonPath("$.user.image").value(defaultAvatar))
        .andExpect(jsonPath("$.user.token").value(token));
  }

  @Test
  public void should_get_401_without_token() throws Exception {
    mvc.perform(get("/user").contentType("application/json")).andExpect(status().isUnauthorized());
  }

  @Test
  public void should_get_401_with_invalid_token() throws Exception {
    String invalidToken = "asdfasd";
    when(jwtService.getSubFromToken(eq(invalidToken))).thenReturn(Optional.empty());
    mvc.perform(get("/user")
            .contentType("application/json")
            .header("Authorization", "Token " + invalidToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void should_update_current_user_profile() throws Exception {
    String newEmail = "newemail@example.com";
    String newBio = "updated";
    String newUsername = "newusernamee";

    Map<String, Object> param =
        new HashMap<String, Object>() {
          {
            put(
                "user",
                new HashMap<String, Object>() {
                  {
                    put("email", newEmail);
                    put("bio", newBio);
                    put("username", newUsername);
                  }
                });
          }
        };

    when(userRepository.findByUsername(eq(newUsername))).thenReturn(Optional.empty());
    when(userRepository.findByEmail(eq(newEmail))).thenReturn(Optional.empty());

    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    mvc.perform(put("/user")
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .content(objectMapper.writeValueAsString(param)))
        .andExpect(status().isOk());
  }

  @Test
  public void should_get_error_if_email_exists_when_update_user_profile() throws Exception {
    String newEmail = "newemail@example.com";
    String newBio = "updated";
    String newUsername = "newusernamee";

    Map<String, Object> param = prepareUpdateParam(newEmail, newBio, newUsername);

    when(userRepository.findByEmail(eq(newEmail)))
        .thenReturn(Optional.of(new User(newEmail, "username", "123", "", "")));
    when(userRepository.findByUsername(eq(newUsername))).thenReturn(Optional.empty());

    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    mvc.perform(put("/user")
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .content(objectMapper.writeValueAsString(param)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.email[0]").value("email already exist"));
  }

  private HashMap<String, Object> prepareUpdateParam(
      final String newEmail, final String newBio, final String newUsername) {
    return new HashMap<String, Object>() {
      {
        put(
            "user",
            new HashMap<String, Object>() {
              {
                put("email", newEmail);
                put("bio", newBio);
                put("username", newUsername);
              }
            });
      }
    };
  }

  @Test
  public void should_get_401_if_not_login() throws Exception {
    mvc.perform(put("/user")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                new HashMap<String, Object>() {
                  {
                    put("user", new HashMap<String, Object>());
                  }
                })))
        .andExpect(status().isUnauthorized());
  }
}
