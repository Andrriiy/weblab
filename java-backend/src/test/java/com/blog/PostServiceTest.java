package com.blog;

import com.blog.dto.PostPage;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.repository.PostRepository;
import com.blog.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    private PostService postService;
    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository);
        testUser = new User("testuser", "test@example.com", "encoded");
        testUser.setId(1L);
        testPost = new Post("Test Title", "Test Content", testUser);
        testPost.setId(1L);
    }

    @Test
    void getPosts_returnsPagedResults() {
        Page<Post> page = new PageImpl<>(List.of(testPost), PageRequest.of(0, 10), 1);
        when(postRepository.findAllFiltered(any(), any())).thenReturn(page);

        PostPage result = postService.getPosts(0, 10, null, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(0, result.getCurrentPage());
    }

    @Test
    void getPost_existingId_returnsPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        Post result = postService.getPost(1L);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
    }

    @Test
    void getPost_nonExistingId_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> postService.getPost(99L));
    }

    @Test
    void createPost_savesAndReturnsPost() {
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        Post result = postService.createPost("Test Title", "Test Content", testUser);

        assertNotNull(result);
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    void deletePost_notOwner_throwsException() {
        User anotherUser = new User("another", "another@example.com", "encoded");
        anotherUser.setId(2L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertThrows(RuntimeException.class, () -> postService.deletePost(1L, anotherUser));
    }

    @Test
    void deletePost_owner_deletesSuccessfully() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        doNothing().when(postRepository).delete(testPost);

        boolean result = postService.deletePost(1L, testUser);

        assertTrue(result);
        verify(postRepository, times(1)).delete(testPost);
    }

    @Test
    void updatePost_notOwner_throwsException() {
        User anotherUser = new User("another", "another@example.com", "encoded");
        anotherUser.setId(2L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertThrows(RuntimeException.class, () -> postService.updatePost(1L, "New Title", "New Content", anotherUser));
    }
}
