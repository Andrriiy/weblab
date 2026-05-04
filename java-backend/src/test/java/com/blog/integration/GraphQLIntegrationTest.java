package com.blog.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureGraphQlTester
class GraphQLIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void registerAndLogin_fullFlow_returnsToken() {
        String registerQuery = """
                mutation {
                    register(username: "integrationuser", email: "integration@test.com", password: "testpass123") {
                        token
                        user { id username email }
                    }
                }
                """;

        graphQlTester.document(registerQuery)
                .execute()
                .path("register.token")
                .entity(String.class)
                .satisfies(token -> assertThat(token).isNotBlank());

        String loginQuery = """
                mutation {
                    login(username: "integrationuser", password: "testpass123") {
                        token
                        user { username }
                    }
                }
                """;

        graphQlTester.document(loginQuery)
                .execute()
                .path("login.user.username")
                .entity(String.class)
                .isEqualTo("integrationuser");
    }

    @Test
    void getPosts_returnsPagedResults() {
        String query = """
                query {
                    posts(page: 0, size: 10) {
                        content { id title }
                        totalElements
                        totalPages
                        currentPage
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .path("posts.totalElements")
                .entity(Integer.class)
                .satisfies(count -> assertThat(count).isGreaterThanOrEqualTo(0));
    }

    @Test
    void getPosts_withFilter_returnsFilteredResults() {
        String query = """
                query {
                    posts(page: 0, size: 10, filterBy: "GraphQL") {
                        content { id title }
                        totalElements
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .path("posts")
                .hasValue();
    }

    @Test
    void getPosts_withSortByTitle_returnsResults() {
        String query = """
                query {
                    posts(page: 0, size: 10, sortBy: "title") {
                        content { id title }
                        currentPage
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .path("posts.currentPage")
                .entity(Integer.class)
                .isEqualTo(0);
    }

    @Test
    void createPostWithoutAuth_returnsError() {
        String mutation = """
                mutation {
                    createPost(title: "Unauthorized Post", content: "Should fail") {
                        id title
                    }
                }
                """;

        graphQlTester.document(mutation)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void meQuery_withoutAuth_returnsNullOrError() {
        String query = """
                query {
                    me { id username }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .errors()
                .filter(e -> true)
                .verify()
                .path("me")
                .valueIsNull();
    }

    @Test
    void getComments_forPost_returnsEmpty() {
        String query = """
                query {
                    comments(postId: 999) {
                        id content
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .path("comments")
                .entityList(Object.class)
                .hasSize(0);
    }

    @Test
    void registerDuplicateUsername_returnsError() {
        String firstRegister = """
                mutation {
                    register(username: "duplicateuser2", email: "dup1b@test.com", password: "pass123") {
                        token
                    }
                }
                """;

        graphQlTester.document(firstRegister).execute();

        String secondRegister = """
                mutation {
                    register(username: "duplicateuser2", email: "dup2b@test.com", password: "pass123") {
                        token
                    }
                }
                """;

        graphQlTester.document(secondRegister)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
