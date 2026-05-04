# Test Scenarios — Blog GraphQL API

## Code Coverage

JaCoCo is configured in `pom.xml`. Run:
```bash
cd artifacts/java-backend
mvn test
```
The coverage report is generated at `target/site/jacoco/index.html`.

---

## Unit Tests (`src/test/java/com/blog/`)

### UserServiceTest (7 tests)

| Test | Scenario |
|------|----------|
| `register_newUser_returnsAuthPayload` | Register with unique username and email → receives JWT token |
| `register_existingUsername_throwsException` | Register with taken username → RuntimeException |
| `register_existingEmail_throwsException` | Register with taken email → RuntimeException |
| `login_validCredentials_returnsAuthPayload` | Login with correct password → JWT token returned |
| `login_wrongPassword_throwsException` | Login with wrong password → RuntimeException |
| `login_nonExistingUser_throwsException` | Login with unknown username → RuntimeException |
| `findByUsername_existing_returnsUser` | Find user by username → User entity returned |

### PostServiceTest (7 tests)

| Test | Scenario |
|------|----------|
| `getPosts_returnsPagedResults` | Fetch page 0 with size 10 → PostPage with results |
| `getPost_existingId_returnsPost` | Get post by valid ID → Post entity returned |
| `getPost_nonExistingId_throwsException` | Get post by unknown ID → RuntimeException |
| `createPost_savesAndReturnsPost` | Create post with valid author → Post saved and returned |
| `deletePost_owner_deletesSuccessfully` | Delete own post → returns true, repository called |
| `deletePost_notOwner_throwsException` | Delete another user's post → RuntimeException |
| `updatePost_notOwner_throwsException` | Update another user's post → RuntimeException |

### CommentServiceTest (5 tests)

| Test | Scenario |
|------|----------|
| `getComments_returnsListForPost` | Fetch comments for post → ordered list returned |
| `createComment_postExists_savesComment` | Add comment to existing post → Comment saved |
| `createComment_postNotFound_throwsException` | Add comment to missing post → RuntimeException |
| `deleteComment_owner_deletesSuccessfully` | Delete own comment → returns true |
| `deleteComment_notOwner_throwsException` | Delete another user's comment → RuntimeException |

---

## Integration Tests (`src/test/java/com/blog/integration/`)

Tests run against a real Spring Boot application context (MOCK web environment) using `HttpGraphQlTester`.

| Test | Scenario |
|------|----------|
| `registerAndLogin_fullFlow_returnsToken` | Register a new user, verify token returned; then login with same credentials, verify username in response |
| `getPosts_returnsPagedResults` | Query posts with pagination → response contains `totalElements` field |
| `getPosts_withFilter_returnsFilteredResults` | Query posts filtering by "GraphQL" → response is valid |
| `getPosts_withSortByTitle_returnsResults` | Query posts sorted by title → currentPage is 0 |
| `createPostWithoutAuth_returnsError` | Attempt to create post without authentication → GraphQL error returned |
| `meQuery_withoutAuth_returnsNull` | Query `me` without token → returns null |
| `getComments_forPost_returnsEmpty` | Query comments for non-existent post ID → empty list |
| `registerDuplicateUsername_returnsError` | Register same username twice → second call returns GraphQL error |

---

## Performance Tests (`src/test/java/com/blog/performance/`)

Tests run against a real server on a random port using `TestRestTemplate`.

### Test 1 — Sequential Load: Posts Endpoint
- Sends **20 sequential requests** to `posts(page:0, size:10)`
- Measures average and max response time per request
- **Assertion**: average response time < 2000 ms
- **Output**: prints total time, average, and max response times

### Test 2 — Concurrent Load: 10 Parallel Threads
- Sends **10 concurrent requests** from separate threads simultaneously
- All requests query the posts endpoint
- **Assertion**: all 10 responses return HTTP 200

### Test 3 — Complex Scenario: Login → Posts → Detail (chain of calls)
> Uses the result of one call as input for the next.

1. **Step 1** — `register` mutation: creates user `perfuser`, captures JWT token from response
2. **Step 2** — `posts` query using the token from Step 1: fetches post list, captures the **first post ID**
3. **Step 3** — `post(id: <id from Step 2>)` query: fetches full detail (title, content, author, comments, commentCount) using the ID from Step 2
- **Assertion**: each step completes with HTTP 200 and returns non-empty data
- **Output**: prints timing for each step

---

## Web Scraping Tests (`src/test/java/com/blog/scraping/`)

Uses **HtmlUnit** (headless browser / HTTP client, analog of Selenium) to simulate a real browser session against a running server on a random port.

### Test 1 — Authorize
- Sends `register` mutation via HtmlUnit WebClient (simulating a browser HTTP POST)
- Extracts JWT token from JSON response
- **Verifies**: token is present, username matches

### Test 2 — Navigate to Posts Page and Read Data
- Logs in as `alice` via `login` mutation → receives token
- Uses token to query the posts page (`posts` query)
- **Scrapes**: title, author, comment count for each post
- **Prints**: all scraped post data to console
- **Verifies**: at least one post returned, totalElements > 0

### Test 3 — Full Scenario: Login → Navigate → Read Post Detail
- **Step 1 (Authorize)**: login as `bob` → token captured
- **Step 2 (Navigate)**: use token to fetch posts list → captures first post ID
- **Step 3 (Read Data)**: fetch full post detail using the ID from Step 2
- **Scrapes**: title, content, author, all comments with their authors
- **Prints**: complete scraped data tree to console

### Test 4 — Read Current User via `/me`
- Logs in as `alice` → token captured
- Queries `me { id username email }` with the token
- **Verifies**: returned username is `alice`, email matches seed data
