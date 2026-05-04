package com.blog;

import com.blog.entity.Comment;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.repository.CommentRepository;
import com.blog.repository.PostRepository;
import com.blog.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    private CommentService commentService;
    private User testUser;
    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postRepository);
        testUser = new User("alice", "alice@example.com", "encoded");
        testUser.setId(1L);
        testPost = new Post("Test Post", "Test Content", testUser);
        testPost.setId(1L);
        testComment = new Comment("Test comment", testUser, testPost);
        testComment.setId(1L);
    }

    @Test
    void getComments_returnsListForPost() {
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testComment));

        List<Comment> result = commentService.getComments(1L);

        assertEquals(1, result.size());
        assertEquals("Test comment", result.get(0).getContent());
    }

    @Test
    void createComment_postExists_savesComment() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        Comment result = commentService.createComment(1L, "Test comment", testUser);

        assertNotNull(result);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void createComment_postNotFound_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> commentService.createComment(99L, "Test comment", testUser));
    }

    @Test
    void deleteComment_notOwner_throwsException() {
        User anotherUser = new User("bob", "bob@example.com", "encoded");
        anotherUser.setId(2L);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        assertThrows(RuntimeException.class, () -> commentService.deleteComment(1L, anotherUser));
    }

    @Test
    void deleteComment_owner_deletesSuccessfully() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        doNothing().when(commentRepository).delete(testComment);

        boolean result = commentService.deleteComment(1L, testUser);

        assertTrue(result);
        verify(commentRepository, times(1)).delete(testComment);
    }
}
