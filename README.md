# DevBlog — GraphQL Blog Platform

A full-stack blog application built with **Java Spring Boot** (GraphQL API) and **React** (frontend). Users can register, log in, write posts, and comment on them.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 19, Spring Boot 3.1, Spring for GraphQL |
| Database | H2 (in-memory), Spring Data JPA / Hibernate |
| Auth | Spring Security + JWT (jjwt 0.11.5) |
| Frontend | React 18, Vite, Apollo Client, Tailwind CSS |
| Routing | wouter |
| Forms | react-hook-form |
| Testing | JUnit 5, Mockito, HtmlUnit, JaCoCo |
| Deployment | Docker, Docker Compose |

---

## Project Structure

```
/
├── artifacts/
│   ├── java-backend/          # Spring Boot GraphQL API
│   │   ├── src/main/java/com/blog/
│   │   │   ├── entity/        # User, Post, Comment
│   │   │   ├── repository/    # JPA repositories
│   │   │   ├── service/       # Business logic
│   │   │   ├── controller/    # GraphQL resolvers
│   │   │   ├── security/      # JWT filter, Spring Security config
│   │   │   ├── dto/           # AuthPayload, PostPage
│   │   │   └── DataSeeder.java
│   │   ├── src/main/resources/
│   │   │   ├── graphql/schema.graphqls
│   │   │   └── application.yml
│   │   ├── src/test/java/com/blog/
│   │   │   ├── integration/   # GraphQL integration tests
│   │   │   ├── performance/   # Load & complex scenario tests
│   │   │   ├── scraping/      # HtmlUnit web scraping tests
│   │   │   ├── PostServiceTest.java
│   │   │   ├── UserServiceTest.java
│   │   │   └── CommentServiceTest.java
│   │   ├── TEST_SCENARIOS.md  # Test descriptions
│   │   ├── pom.xml
│   │   └── Dockerfile
│   └── frontend/              # React + Vite SPA
│       ├── src/
│       │   ├── context/       # AuthContext (global state)
│       │   ├── lib/           # Apollo client, GraphQL queries
│       │   ├── pages/         # PostsPage, PostDetailPage, etc.
│       │   └── components/    # Navbar, ProtectedRoute, UI
│       ├── Dockerfile
│       └── nginx.conf
├── docker-compose.yml
└── README.md
```

---

## Entities & Relationships

```
User ──< Post ──< Comment
     └──────────────────^
```

- **User** — id, username, email, password, createdAt
- **Post** — id, title, content, author (User), createdAt, updatedAt
- **Comment** — id, content, author (User), post (Post), createdAt

---

## GraphQL API

**Endpoint:** `POST /graphql`
**GraphiQL IDE:** `GET /graphiql` (interactive browser)

### Queries

```graphql
me: User
posts(page: Int, size: Int, sortBy: String, filterBy: String): PostPage!
post(id: ID!): Post
comments(postId: ID!): [Comment!]!
```

### Mutations

```graphql
register(username: String!, email: String!, password: String!): AuthPayload!
login(username: String!, password: String!): AuthPayload!
createPost(title: String!, content: String!): Post!
updatePost(id: ID!, title: String!, content: String!): Post!
deletePost(id: ID!): Boolean!
createComment(postId: ID!, content: String!): Comment!
deleteComment(id: ID!): Boolean!
```

### Authentication

Include the JWT token from `login`/`register` in subsequent requests:

```
Authorization: Bearer <token>
```

### Example: Register and fetch posts

```graphql
# 1. Register
mutation {
  register(username: "alice", email: "alice@example.com", password: "secret123") {
    token
    user { id username }
  }
}

# 2. Fetch posts (paginated, filtered)
query {
  posts(page: 0, size: 5, sortBy: "createdAt", filterBy: "GraphQL") {
    content { id title author { username } commentCount createdAt }
    totalElements totalPages currentPage
  }
}
```

---

## Frontend Pages

| Path | Description | Auth required |
|---|---|---|
| `/` | Posts list — search, pagination | No |
| `/posts/:id` | Post detail + comments | No |
| `/post/new` | Create new post | Yes |
| `/post/:id/edit` | Edit existing post | Yes |
| `/login` | Login | No |
| `/register` | Register | No |

---

## Running Locally

### Backend (Java)

Requires Java 17+ and Maven.

```bash
cd artifacts/java-backend
mvn spring-boot:run
```

Server starts at `http://localhost:8000`.
GraphiQL available at `http://localhost:8000/graphiql`.

### Frontend (React)

Requires Node.js 20+ and pnpm.

```bash
pnpm install
pnpm --filter @workspace/frontend run dev
```

Frontend starts at `http://localhost:<PORT>`.

---

## Running with Docker

```bash
docker-compose up --build
```

- Frontend: `http://localhost:80`
- Backend: `http://localhost:8081`

---

## Tests

Run all tests and generate coverage:

```bash
cd artifacts/java-backend
mvn test
```

Coverage report: `target/site/jacoco/index.html`

### Test Summary

| Suite | Tests | Type |
|---|---|---|
| UserServiceTest | 7 | Unit |
| PostServiceTest | 7 | Unit |
| CommentServiceTest | 5 | Unit |
| GraphQLIntegrationTest | 8 | Integration |
| PerformanceTest | 3 | Performance |
| WebScrapingTest | 4 | Scraping (HtmlUnit) |
| **Total** | **34** | |

**Coverage:** 77% instructions, 45% branches

See [`artifacts/java-backend/TEST_SCENARIOS.md`](artifacts/java-backend/TEST_SCENARIOS.md) for detailed scenario descriptions.

---

## Seed Data

The app seeds two users on startup (password: `password123`):

| Username | Email |
|---|---|
| alice | alice@example.com |
| bob | bob@example.com |

Three sample posts and three comments are also seeded.

---

## H2 Console

The in-memory database is accessible at `/h2-console` during development.

- **JDBC URL:** `jdbc:h2:mem:blogdb`
- **Username:** `sa`
- **Password:** `password`
