package ru.tech_mail.forum.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class Thread<forum, user> {
    private String date;
    private forum forum;
    private int id;
    @JsonProperty(value = "isClosed")
    private boolean isClosed;
    @JsonProperty(value = "isDeleted")
    private boolean isDeleted;
    private String message;
    private String slug;
    private String title;
    private user user;

    public Thread(int id, String title, String slug, forum forum, user user,
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

    public Thread(ResultSet resultSet, forum forum, user user) throws SQLException {
        String threadDate = resultSet.getString("t.date");
        this.date = threadDate.lastIndexOf('.') != -1 ?
                threadDate.substring(0,threadDate.lastIndexOf('.')) : threadDate;
        this.forum = forum;
        this.user = user;
        this.id = resultSet.getInt("t.id");
        this.isClosed = resultSet.getBoolean("t.isClosed");
        this.isDeleted = resultSet.getBoolean("t.isDeleted");
        this.message = resultSet.getString("t.message");
        this.slug = resultSet.getString("t.slug");
        this.title = resultSet.getString("t.title");
    }

    public Thread(int id) {
        this.id = id;
    }

    public String getDate() {
        return date;
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

    public forum getForum() {
        return forum;
    }

    public user getUser() {
        return user;
    }

    public void setUser(user user) {
        this.user = user;
    }

    public void setForum(forum forum) {
        this.forum = forum;
    }
}
