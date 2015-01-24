package ru.tech_mail.forum.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Date;

public class PostAdvanced<forum, thread, user> {
    private String date;
    private int id;
    @JsonProperty(value = "isApproved")
    private boolean isApproved;
    @JsonProperty(value = "isDeleted")
    private boolean isDeleted;
    @JsonProperty(value = "isEdited")
    private boolean isEdited;
    @JsonProperty(value = "isHighlighted")
    private boolean isHighlighted;
    @JsonProperty(value = "isSpam")
    private boolean isSpam;
    private int likes;
    private int dislikes;
    private int points;
    private String message;
    private Integer parent; // it can be null
    private forum forum;
    private thread thread;
    private user user;

    public PostAdvanced(int id, String message, Integer parent,
                        Integer likes, Integer dislikes, Integer points, boolean isDeleted, boolean isSpam,
                        boolean isEdited, boolean isApproved, boolean isHighlighted, String date) {
        this.id = id;
        this.message = message;
        this.parent = (parent != null && parent == 0) ? null : parent;
        this.likes = likes;
        this.dislikes = dislikes;
        this.points = points;
        this.isDeleted = isDeleted;
        this.isSpam = isSpam;
        this.isEdited = isEdited;
        this.isApproved = isApproved;
        this.isHighlighted = isHighlighted;
        this.date = date.lastIndexOf('.') != -1 ? date.substring(0,date.lastIndexOf('.')) : date;
    }

    public void setForum(forum forum) {
        this.forum = forum;
    }

    public void setThread(thread thread) {
        this.thread = thread;
    }

    public void setUser(user user) {
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public int getId() {
        return id;
    }

//    public boolean isApproved() {
//        return isApproved;
//    }

//    public boolean isDeleted() {
//        return isDeleted;
//    }

//    public boolean isEdited() {
//        return isEdited;
//    }

//    public boolean isHighlighted() {
//        return isHighlighted;
//    }

//    public boolean isSpam() {
//        return isSpam;
//    }

    public int getLikes() {
        return likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public int getPoints() {
        return points;
    }

    public String getMessage() {
        return message;
    }

    public Integer getParent() {
        return parent;
    }

    public forum getForum() {
        return forum;
    }

    public thread getThread() {
        return thread;
    }

    public user getUser() {
        return user;
    }
}
