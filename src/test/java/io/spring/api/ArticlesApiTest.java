package io.spring.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import static java.util.Arrays.asList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ArticlesApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ArticlesApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleCommandService articleCommandService;

  @Autowired private ObjectMapper objectMapper;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void should_create_article_success() throws Exception {
    String title = "How to train your dragon";
    String slug = "how-to-train-your-dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = asList("reactjs", "angularjs", "dragons");
    Map<String, Object> param = prepareParam(title, description, body, tagList);

    ArticleData articleData =
        new ArticleData(
            "123",
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            new ProfileData("userid", user.getUsername(), user.getBio(), user.getImage(), false));

    when(articleCommandService.createArticle(any(), any()))
        .thenReturn(new Article(title, description, body, tagList, user.getId()));

    when(articleQueryService.findBySlug(eq(Article.toSlug(title)), any()))
        .thenReturn(Optional.empty());

    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    mvc.perform(post("/articles")
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .content(objectMapper.writeValueAsString(param)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.article.title").value(title))
        .andExpect(jsonPath("$.article.favorited").value(false))
        .andExpect(jsonPath("$.article.body").value(body))
        .andExpect(jsonPath("$.article.favoritesCount").value(0))
        .andExpect(jsonPath("$.article.author.username").value(user.getUsername()));

    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  public void should_get_error_message_with_wrong_parameter() throws Exception {
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "";
    String[] tagList = {"reactjs", "angularjs", "dragons"};
    Map<String, Object> param = prepareParam(title, description, body, asList(tagList));

    mvc.perform(post("/articles")
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .content(objectMapper.writeValueAsString(param)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"));
  }

  @Test
  public void should_get_error_message_with_duplicated_title() throws Exception {
    String title = "How to train your dragon";
    String slug = "how-to-train-your-dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    String[] tagList = {"reactjs", "angularjs", "dragons"};
    Map<String, Object> param = prepareParam(title, description, body, asList(tagList));

    ArticleData articleData =
        new ArticleData(
            "123",
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            asList(tagList),
            new ProfileData("userid", user.getUsername(), user.getBio(), user.getImage(), false));

    when(articleQueryService.findBySlug(eq(Article.toSlug(title)), any()))
        .thenReturn(Optional.of(articleData));

    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    mvc.perform(post("/articles")
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .content(objectMapper.writeValueAsString(param)))
        .andExpect(status().isBadRequest());
  }

  private HashMap<String, Object> prepareParam(
      final String title, final String description, final String body, final List<String> tagList) {
    return new HashMap<String, Object>() {
      {
        put(
            "article",
            new HashMap<String, Object>() {
              {
                put("title", title);
                put("description", description);
                put("body", body);
                put("tagList", tagList);
              }
            });
      }
    };
  }
}
