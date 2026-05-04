package com.blog.controller;

import com.blog.dto.PostPage;
import com.blog.entity.Comment;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.service.CommentService;
import com.blog.service.PostService;
import com.blog.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final CommentService commentService;

    public PostController(PostService postService, UserService userService, CommentService commentService) {
        this.postService = postService;
        this.userService = userService;
        this.commentService = commentService;
    }

    @QueryMapping
    public PostPage posts(
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String filterBy) {
        return postService.getPosts(
                page != null ? page : 0,
                size != null ? size : 10,
                sortBy,
                filterBy
        );
    }

    @QueryMapping
    public Post post(@Argument Long id) {
        return postService.getPost(id);
    }

    @MutationMapping
    public Post createPost(@Argument String title, @Argument String content, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }
        User user = userService.findByUsername(authentication.getName());
        return postService.createPost(title, content, user);
    }

    @MutationMapping
    public Post updatePost(@Argument Long id, @Argument String title, @Argument String content, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }
        User user = userService.findByUsername(authentication.getName());
        return postService.updatePost(id, title, content, user);
    }

    @MutationMapping
    public Boolean deletePost(@Argument Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }
        User user = userService.findByUsername(authentication.getName());
        return postService.deletePost(id, user);
    }

    @SchemaMapping(typeName = "Post", field = "comments")
    public List<Comment> comments(Post post) {
        return commentService.getComments(post.getId());
    }

    @SchemaMapping(typeName = "Post", field = "commentCount")
    public int commentCount(Post post) {
        return commentService.getComments(post.getId()).size();
    }
}
