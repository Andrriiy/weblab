package com.blog;

import com.blog.entity.Comment;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.repository.CommentRepository;
import com.blog.repository.PostRepository;
import com.blog.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PostRepository postRepository,
                      CommentRepository commentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User alice = new User("alice", "alice@example.com", passwordEncoder.encode("password123"));
        User bob = new User("bob", "bob@example.com", passwordEncoder.encode("password123"));
        userRepository.save(alice);
        userRepository.save(bob);

        Post post1 = new Post("Getting Started with GraphQL", "GraphQL is a query language for your API. It provides a complete description of the data in your API and gives clients the power to ask for exactly what they need. This makes it easier to evolve APIs over time and enables powerful developer tools.", alice);
        Post post2 = new Post("Spring Boot Best Practices", "Spring Boot makes it easy to create stand-alone, production-grade Spring-based Applications. Here are some best practices to follow when building Spring Boot applications for better maintainability and performance.", alice);
        Post post3 = new Post("Introduction to React Hooks", "React Hooks allow you to use state and other React features without writing a class. useState and useEffect are the most commonly used hooks that every React developer should know.", bob);
        postRepository.save(post1);
        postRepository.save(post2);
        postRepository.save(post3);

        commentRepository.save(new Comment("Great introduction! Very helpful for beginners.", bob, post1));
        commentRepository.save(new Comment("This really helped me understand GraphQL. Thanks!", bob, post2));
        commentRepository.save(new Comment("Hooks changed the way I write React. Amazing post.", alice, post3));
    }
}
