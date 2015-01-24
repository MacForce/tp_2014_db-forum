package ru.tech_mail.forum.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
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

    public void setEmail(String email) {
        this.email = email;
    }
}
