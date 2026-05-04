package com.blog.scraping;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebScrapingTest {

    @LocalServerPort
    private int port;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String graphqlUrl;

    @BeforeEach
    void setUp() {
        webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        graphqlUrl = "http://localhost:" + port + "/graphql";
    }

    @AfterEach
    void tearDown() {
        webClient.close();
    }

    private JsonNode executeGraphQL(String query, String token) throws Exception {
        WebRequest request = new WebRequest(new URL(graphqlUrl),
                com.gargoylesoftware.htmlunit.HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/json");
        if (token != null) {
            request.setAdditionalHeader("Authorization", "Bearer " + token);
        }
        request.setRequestBody("{\"query\": " + objectMapper.writeValueAsString(query) + "}");
        WebResponse response = webClient.loadWebResponse(request);
        return objectMapper.readTree(response.getContentAsString());
    }

    @Test
    void scraping_step1_authorize_obtainsToken() throws Exception {
        System.out.println("=== Web Scraping Test: Step 1 — Authorize ===");

        String registerQuery = """
                mutation {
                    register(username: "scrapeuser", email: "scrape@test.com", password: "scrapepass") {
                        token
                        user { id username email }
                    }
                }
                """;

        JsonNode result = executeGraphQL(registerQuery, null);
        String token = result.path("data").path("register").path("token").asText();
        String username = result.path("data").path("register").path("user").path("username").asText();

        System.out.println("  -> Authorized as: " + username);
        assertThat(token).isNotBlank();
        assertThat(username).isEqualTo("scrapeuser");
    }

    @Test
    void scraping_step2_navigateToPostsPage_andReadData() throws Exception {
        System.out.println("=== Web Scraping Test: Step 2 — Navigate to Posts Page and Read Data ===");

        String loginQuery = """
                mutation {
                    login(username: "alice", password: "password123") {
                        token
                        user { id username }
                    }
                }
                """;

        JsonNode loginResult = executeGraphQL(loginQuery, null);
        String token = loginResult.path("data").path("login").path("token").asText();
        System.out.println("  -> Logged in as alice, token obtained");
        assertThat(token).isNotBlank();

        String postsQuery = """
                {
                    posts(page: 0, size: 10) {
                        content {
                            id
                            title
                            author { username }
                            commentCount
                            createdAt
                        }
                        totalElements
                        totalPages
                        currentPage
                    }
                }
                """;

        JsonNode postsResult = executeGraphQL(postsQuery, token);
        JsonNode posts = postsResult.path("data").path("posts").path("content");
        int totalElements = postsResult.path("data").path("posts").path("totalElements").asInt();

        System.out.println("  -> Navigated to Posts page, found " + totalElements + " total posts");

        List<String> scrapedTitles = new ArrayList<>();
        for (JsonNode post : posts) {
            String title = post.path("title").asText();
            String author = post.path("author").path("username").asText();
            int commentCount = post.path("commentCount").asInt();
            scrapedTitles.add(title);
            System.out.println("  -> Post: \"" + title + "\" by " + author + " (" + commentCount + " comments)");
        }

        assertThat(scrapedTitles).isNotEmpty();
        assertThat(totalElements).isGreaterThan(0);
    }

    @Test
    void scraping_step3_fullScenario_loginNavigateReadPostDetail() throws Exception {
        System.out.println("=== Web Scraping Test: Full Scenario — Login, Navigate, Read Post Detail ===");

        String loginQuery = """
                mutation {
                    login(username: "bob", password: "password123") {
                        token
                        user { id username }
                    }
                }
                """;

        JsonNode loginResult = executeGraphQL(loginQuery, null);
        String token = loginResult.path("data").path("login").path("token").asText();
        String username = loginResult.path("data").path("login").path("user").path("username").asText();
        System.out.println("  -> Step 1 (Authorize): Logged in as " + username);
        assertThat(token).isNotBlank();

        String postsQuery = "{ posts(page: 0, size: 5) { content { id title } } }";
        JsonNode postsResult = executeGraphQL(postsQuery, token);
        JsonNode firstPost = postsResult.path("data").path("posts").path("content").get(0);
        String postId = firstPost.path("id").asText();
        System.out.println("  -> Step 2 (Navigate): Found post ID " + postId);
        assertThat(postId).isNotBlank();

        String detailQuery = String.format("""
                {
                    post(id: %s) {
                        id title content createdAt updatedAt
                        author { id username email }
                        comments { id content createdAt author { username } }
                        commentCount
                    }
                }
                """, postId);

        JsonNode detailResult = executeGraphQL(detailQuery, token);
        JsonNode post = detailResult.path("data").path("post");
        String title = post.path("title").asText();
        String content = post.path("content").asText();
        String authorUsername = post.path("author").path("username").asText();
        int commentCount = post.path("commentCount").asInt();
        JsonNode comments = post.path("comments");

        System.out.println("  -> Step 3 (Read Data):");
        System.out.println("     Title: " + title);
        System.out.println("     Author: " + authorUsername);
        System.out.println("     Content length: " + content.length() + " chars");
        System.out.println("     Comments: " + commentCount);
        for (JsonNode comment : comments) {
            System.out.println("       - \"" + comment.path("content").asText() + "\" by " + comment.path("author").path("username").asText());
        }

        assertThat(title).isNotBlank();
        assertThat(content).isNotBlank();
        assertThat(authorUsername).isNotBlank();
    }

    @Test
    void scraping_meEndpoint_returnsCurrentUser() throws Exception {
        System.out.println("=== Web Scraping Test: /me Endpoint — Read Current User Data ===");

        String loginQuery = """
                mutation {
                    login(username: "alice", password: "password123") {
                        token
                    }
                }
                """;
        JsonNode loginResult = executeGraphQL(loginQuery, null);
        String token = loginResult.path("data").path("login").path("token").asText();

        String meQuery = "{ me { id username email } }";
        JsonNode meResult = executeGraphQL(meQuery, token);
        String scrapedUsername = meResult.path("data").path("me").path("username").asText();
        String scrapedEmail = meResult.path("data").path("me").path("email").asText();

        System.out.println("  -> Scraped current user: " + scrapedUsername + " <" + scrapedEmail + ">");
        assertThat(scrapedUsername).isEqualTo("alice");
        assertThat(scrapedEmail).isEqualTo("alice@example.com");
    }
}
