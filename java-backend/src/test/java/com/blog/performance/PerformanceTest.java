package com.blog.performance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PerformanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/graphql";
    }

    private ResponseEntity<String> executeGraphQL(String query, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        Map<String, String> body = Map.of("query", query);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(baseUrl, request, String.class);
    }

    @Test
    void performanceTest_postsEndpoint_respondsUnder2Seconds() throws Exception {
        String query = "{ posts(page: 0, size: 10) { content { id title } totalElements } }";

        long start = System.currentTimeMillis();
        int requestCount = 20;
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            long reqStart = System.currentTimeMillis();
            ResponseEntity<String> response = executeGraphQL(query, null);
            long reqEnd = System.currentTimeMillis();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            responseTimes.add(reqEnd - reqStart);
        }

        long totalTime = System.currentTimeMillis() - start;
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("=== Performance Test: Posts Endpoint ===");
        System.out.println("Total requests: " + requestCount);
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Average response time: " + avgResponseTime + " ms");
        System.out.println("Max response time: " + maxResponseTime + " ms");

        assertThat(avgResponseTime).isLessThan(2000);
    }

    @Test
    void performanceTest_concurrentRequests_allSucceed() throws Exception {
        String query = "{ posts(page: 0, size: 5) { content { id title } totalElements } }";

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    ResponseEntity<String> response = executeGraphQL(query, null);
                    return response.getStatusCode().value();
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - start;
        executor.shutdown();

        System.out.println("=== Performance Test: Concurrent Requests ===");
        System.out.println("Concurrent threads: " + threadCount);
        System.out.println("Total time: " + totalTime + " ms");

        for (Future<Integer> future : futures) {
            assertThat(future.get()).isEqualTo(200);
        }
    }

    @Test
    void performanceTest_complexScenario_loginThenGetPostsThenGetDetail() throws Exception {
        System.out.println("=== Performance Test: Complex Scenario ===");
        System.out.println("Step 1: Register a new user");

        long step1Start = System.currentTimeMillis();
        String registerQuery = """
                mutation {
                    register(username: "perfuser", email: "perf@test.com", password: "perfpass123") {
                        token
                        user { id username }
                    }
                }
                """;
        ResponseEntity<String> registerResponse = executeGraphQL(registerQuery, null);
        long step1Time = System.currentTimeMillis() - step1Start;
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode registerData = objectMapper.readTree(registerResponse.getBody());
        String token = registerData.path("data").path("register").path("token").asText();
        assertThat(token).isNotBlank();
        System.out.println("  -> Register completed in " + step1Time + " ms, token obtained");

        System.out.println("Step 2: Get posts list using the token");
        long step2Start = System.currentTimeMillis();
        String postsQuery = "{ posts(page: 0, size: 10) { content { id title } totalElements } }";
        ResponseEntity<String> postsResponse = executeGraphQL(postsQuery, token);
        long step2Time = System.currentTimeMillis() - step2Start;
        assertThat(postsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode postsData = objectMapper.readTree(postsResponse.getBody());
        JsonNode content = postsData.path("data").path("posts").path("content");

        System.out.println("  -> Posts fetched in " + step2Time + " ms, found " + content.size() + " posts");

        if (content.size() > 0) {
            String firstPostId = content.get(0).path("id").asText();
            System.out.println("Step 3: Get detail of post ID " + firstPostId + " using result from step 2");

            long step3Start = System.currentTimeMillis();
            String postDetailQuery = String.format("""
                    {
                        post(id: %s) {
                            id title content createdAt
                            author { id username }
                            comments { id content author { username } }
                            commentCount
                        }
                    }
                    """, firstPostId);
            ResponseEntity<String> detailResponse = executeGraphQL(postDetailQuery, token);
            long step3Time = System.currentTimeMillis() - step3Start;
            assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode detailData = objectMapper.readTree(detailResponse.getBody());
            String postTitle = detailData.path("data").path("post").path("title").asText();
            assertThat(postTitle).isNotBlank();

            System.out.println("  -> Post detail fetched in " + step3Time + " ms, title: " + postTitle);
            System.out.println("=== Complex Scenario Total: Step1=" + step1Time + "ms, Step2=" + step2Time + "ms, Step3=" + step3Time + "ms ===");
        }
    }
}
