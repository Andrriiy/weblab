package com.blog.repository;

import com.blog.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p WHERE (:filter IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :filter, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :filter, '%')))")
    Page<Post> findAllFiltered(@Param("filter") String filter, Pageable pageable);
}
