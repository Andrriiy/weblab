package com.blog.service;

import com.blog.dto.PostPage;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public PostPage getPosts(int page, int size, String sortBy, String filterBy) {
        Sort sort = switch (sortBy != null ? sortBy : "createdAt") {
            case "title" -> Sort.by("title").ascending();
            case "updatedAt" -> Sort.by("updatedAt").descending();
            default -> Sort.by("createdAt").descending();
        };
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Post> result = postRepository.findAllFiltered(filterBy, pageable);
        return new PostPage(result.getContent(), result.getTotalElements(), result.getTotalPages(), page);
    }

    public Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public Post createPost(String title, String content, User author) {
        Post post = new Post(title, content, author);
        return postRepository.save(post);
    }

    public Post updatePost(Long id, String title, String content, User currentUser) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to update this post");
        }
        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    public boolean deletePost(Long id, User currentUser) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to delete this post");
        }
        postRepository.delete(post);
        return true;
    }
}
