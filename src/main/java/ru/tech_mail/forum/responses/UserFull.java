package ru.tech_mail.forum.responses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UserFull extends User {
    private List<String> followers;
    private List<String> following;
    private List<Integer> subscriptions;

    public UserFull(ResultSet resultSet) throws SQLException {
        super(resultSet);
    }

    public UserFull(String email) {
        super(email);
    }

    public List<String> getFollowers() {
        return followers;
    }

    public void setFollowers(List<String> followers) {
        this.followers = followers;
    }

    public List<String> getFollowing() {
        return following;
    }

    public void setFollowing(List<String> following) {
        this.following = following;
    }

    public List<Integer> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Integer> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
