package ru.tech_mail.forum.responses;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Forum<user> {
    private int id;
    private String name;
    private String short_name;
    private user user;

    public Forum(int id, String name, String short_name, user user) {
        this.id = id;
        this.name = name;
        this.short_name = short_name;
        this.user = user;
    }

    public Forum(ResultSet resultSet, user user) throws SQLException {
        this.id = resultSet.getInt("f.id");
        this.name = resultSet.getString("f.name");
        this.short_name = resultSet.getString("f.short_name");
        this.user = user;
    }

    public Forum(String short_name) {
        this.short_name = short_name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShort_name() {
        return short_name;
    }

    public user getUser() {
        return user;
    }

}
