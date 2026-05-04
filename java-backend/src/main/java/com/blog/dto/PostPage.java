package com.blog.dto;

import com.blog.entity.Post;
import java.util.List;

public class PostPage {
    private List<Post> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;

    public PostPage(List<Post> content, long totalElements, int totalPages, int currentPage) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public List<Post> getContent() { return content; }
    public void setContent(List<Post> content) { this.content = content; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
}
