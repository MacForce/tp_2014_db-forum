package ru.tech_mail.forum.responses;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostFull<forum, thread, user> extends Post {
    private int likes;
    private int dislikes;
    private int points;

//    public PostFull(int id, String message, String forumShortName, String userEmail, Integer parent,
//                    Integer threadId, Integer likes, Integer dislikes, Integer points, boolean isDeleted,
//                    boolean isSpam, boolean isEdited, boolean isApproved, boolean isHighlighted, String date) {
//        super(id, message, forumShortName, userEmail, parent, threadId, isDeleted, isSpam,
//                isEdited, isApproved, isHighlighted, date);
//        this.likes = likes;
//        this.dislikes = dislikes;
//        this.points = points;
//    }

    public PostFull(ResultSet resultSet, forum forum, user user, thread thread) throws SQLException {
        super(resultSet, forum, user, thread);
        this.likes = resultSet.getInt("p.likes");
        this.dislikes = resultSet.getInt("p.dislikes");
        this.points = resultSet.getInt("p.points");
    }

    public PostFull() {}

    public int getLikes() {
        return likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public int getPoints() {
        return points;
    }

}
