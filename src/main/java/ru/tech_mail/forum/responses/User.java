package ru.tech_mail.forum.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
    private String about;
    private String email;
    private int id;
    @JsonProperty(value = "isAnonymous")
    private boolean isAnonymous;
    private String name;
    private String username;

    public User(int id, String email, String username, String name, String about, boolean isAnonymous) {
        this.id = id;
        this.username = "null".equals(username) ? null : username;
        this.name = "null".equals(name) ? null : name;
        this.about = "null".equals(about) ? null : about;
        this.isAnonymous = isAnonymous;
        this.email = email;
    }

    public User(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt("u.id");
        this.username = "null".equals(resultSet.getString("u.username")) ? null : resultSet.getString("u.username");
        this.name = "null".equals(resultSet.getString("u.name")) ? null : resultSet.getString("u.name");
        this.about = "null".equals(resultSet.getString("u.about")) ? null : resultSet.getString("u.about");
        this.isAnonymous = resultSet.getBoolean("u.isAnonymous");
        this.email = resultSet.getString("u.email");
    }

    public User(String email) {
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getAbout() {
        return about;
    }

//    public boolean isAnonymous() {
//        return isAnonymous;
//    }

    public void setAnonymous(boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public String getEmail() {
        return email;
    }

}
