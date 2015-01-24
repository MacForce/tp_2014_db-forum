package ru.tech_mail.forum.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Post {
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
    private String forum;
    private int thread;
    private String user;

    /*
    Не забудь, что нету полей likes, dislikes, points !!! В ResultSet они будут, поэтому их нужно пропустить!!!
     */
    public Post(int id, String message, String forumShortName, String userEmail, Integer parent,
                Integer threadId, boolean isDeleted, boolean isSpam, boolean isEdited, boolean isApproved,
                boolean isHighlighted, String date) {
        this.id = id;
        this.message = message;
        this.forum = forumShortName;
        this.user = userEmail;
        this.parent = (parent != null && parent == 0) ? null : parent;
        this.thread = threadId;
        this.isDeleted = isDeleted;
        this.isSpam = isSpam;
        this.isEdited = isEdited;
        this.isApproved = isApproved;
        this.isHighlighted = isHighlighted;
        this.date = date.lastIndexOf('.') != -1 ? date.substring(0,date.lastIndexOf('.')) : date;
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

    public String getMessage() {
        return message;
    }

    public Integer getParent() {
        return parent;
    }

    public String getForum() {
        return forum;
    }

    public int getThread() {
        return thread;
    }

    public String getUser() {
        return user;
    }

}
