package ru.tech_mail.forum.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.ResultSet;
import java.sql.SQLException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Post<forum, thread, user> {
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
    private String message;
    private Integer parent; // it can be null
    private forum forum;
    private thread thread;
    private user user;

    public Post(int id, String message, forum forumShortName, user userEmail, Integer parent,
                thread threadId, boolean isDeleted, boolean isSpam, boolean isEdited, boolean isApproved,
                boolean isHighlighted, String date) {
        this.id = id;
        this.message = message;
        this.forum = forumShortName;
        this.user = userEmail;
        if (parent != null) {
            if (parent == 0) {
                this.parent = null;
            } else {
                this.parent = parent;
            }
        } else {
            this.parent = parent;
        }
        this.thread = threadId;
        this.isDeleted = isDeleted;
        this.isSpam = isSpam;
        this.isEdited = isEdited;
        this.isApproved = isApproved;
        this.isHighlighted = isHighlighted;
        this.date = date.lastIndexOf('.') != -1 ? date.substring(0,date.lastIndexOf('.')) : date;
    }

    public Post(ResultSet resultSet, forum forum, user user, thread thread) throws SQLException {
        this.id = resultSet.getInt("p.id");
        this.message = resultSet.getString("p.message");
        this.forum = forum;
        this.user = user;
        this.thread = thread;
        this.parent = (resultSet.getInt("p.parent") == 0) ? null : resultSet.getInt("p.parent");
        this.isDeleted = resultSet.getBoolean("p.isDeleted");
        this.isSpam = resultSet.getBoolean("p.isSpam");
        this.isEdited = resultSet.getBoolean("p.isEdited");
        this.isApproved = resultSet.getBoolean("p.isApproved");
        this.isHighlighted = resultSet.getBoolean("p.isHighlighted");
        String postDate = resultSet.getString("p.date");
        this.date = postDate.lastIndexOf('.') != -1 ?
                postDate.substring(0, postDate.lastIndexOf('.')) : postDate;
    }

    public Post() {}

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

    public void setForum(forum forum) {
        this.forum = forum;
    }

    public void setThread(thread thread) {
        this.thread = thread;
    }

    public void setUser(user user) {
        this.user = user;
    }
}
