package ru.tech_mail.forum.responses;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ThreadFull<forum, user> extends Thread {
    private int dislikes;
    private int likes;
    private int points;
    private int posts;

    public ThreadFull(ResultSet resultSet,forum forum, user user) throws SQLException {
        super(resultSet, forum, user);
        this.posts = resultSet.getBoolean("t.isDeleted") ? 0 : resultSet.getInt("t.posts");
        this.likes = resultSet.getInt("t.likes");
        this.dislikes = resultSet.getInt("t.dislikes");
        this.points = resultSet.getInt("t.points");
    }

    public ThreadFull(int id) {
        super(id);
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
