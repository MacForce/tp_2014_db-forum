package ru.tech_mail.forum.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class Thread {
    private String date;
    private String forum;
    private int id;
    @JsonProperty(value = "isClosed")
    private boolean isClosed;
    @JsonProperty(value = "isDeleted")
    private boolean isDeleted;
    private String message;
    private String slug;
    private String title;
    private String user;

    public Thread(int id, String title, String slug, String forum, String user,
                  boolean isDeleted, boolean isClosed, String date, String message) {
        this.date = date.lastIndexOf('.') != -1 ? date.substring(0,date.lastIndexOf('.')) : date;
        this.forum = forum;
        this.id = id;
        this.isClosed = isClosed;
        this.isDeleted = isDeleted;
        this.message = message;
        this.slug = slug;
        this.title = title;
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public String getForum() {
        return forum;
    }

    public int getId() {
        return id;
    }

//    public boolean isClosed() {
//        return isClosed;
//    }

//    public boolean isDeleted() {
//        return isDeleted;
//    }

    public String getMessage() {
        return message;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getUser() {
        return user;
    }
}
