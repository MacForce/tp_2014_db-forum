package ru.tech_mail.forum.responses;

import java.util.Date;

public class ThreadFull extends Thread {
    private int dislikes;
    private int likes;
    private int points;
    private int posts;

    public ThreadFull(int id, String title, String slug, String forum, String user,
                      int posts, int likes, int dislikes, int points, boolean isDeleted,
                      boolean isClosed, String date, String message) {
        super(id, title, slug, forum, user, isDeleted, isClosed, date, message);
        this.posts = isDeleted ? 0 : posts;
        this.likes = likes;
        this.dislikes = dislikes;
        this.points = points;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getPosts() {
        return posts;
    }

    public void setPosts(int posts) {
        this.posts = posts;
    }
}
