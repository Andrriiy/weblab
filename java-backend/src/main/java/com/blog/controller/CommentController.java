package com.blog.controller;

import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.service.CommentService;
import com.blog.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @QueryMapping
    public List<Comment> comments(@Argument Long postId) {
        return commentService.getComments(postId);
    }

    @MutationMapping
    public Comment createComment(@Argument Long postId, @Argument String content, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }
        User user = userService.findByUsername(authentication.getName());
        return commentService.createComment(postId, content, user);
    }

    @MutationMapping
    public Boolean deleteComment(@Argument Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }
        User user = userService.findByUsername(authentication.getName());
        return commentService.deleteComment(id, user);
    }
}
