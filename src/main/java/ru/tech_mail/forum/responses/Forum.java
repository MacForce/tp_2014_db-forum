package ru.tech_mail.forum.responses;

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
