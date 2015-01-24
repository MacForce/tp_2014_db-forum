package ru.tech_mail.forum.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class ThreadAdvanced<Forum, User> {
    private String date;
    private Forum forum;
    private int id;
    @JsonProperty(value = "isClosed")
    private boolean isClosed;
    @JsonProperty(value = "isDeleted")
    private boolean isDeleted;
    private String message;
    private String slug;
    private String title;
    private int dislikes;
    private int likes;
    private int points;
    private int posts;
    private User user;

    public ThreadAdvanced(int id, String title, String slug, int posts, int likes, int dislikes,
                          int points, boolean isDeleted, boolean isClosed, String date, String message) {
        this.date = date.lastIndexOf('.') != -1 ? date.substring(0,date.lastIndexOf('.')) : date;
        this.id = id;
        this.isClosed = isClosed;
        this.isDeleted = isDeleted;
        this.message = message;
        this.slug = slug;
        this.title = title;
        this.posts = isDeleted ? 0 : posts;
        this.likes = likes;
        this.dislikes = dislikes;
        this.points = points;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

//    public boolean isClosed() {
//        return isClosed;
//    }

    public void setClosed(boolean isClosed) {
        this.isClosed = isClosed;
    }

//    public boolean isDeleted() {
//        return isDeleted;
//    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Forum getForum() {
        return forum;
    }

    public void setForum(Forum forum) {
        this.forum = forum;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getDislikes() {
        return dislikes;
    }

    public int getLikes() {
        return likes;
    }

    public int getPoints() {
        return points;
    }

    public int getPosts() {
        return posts;
    }
}
