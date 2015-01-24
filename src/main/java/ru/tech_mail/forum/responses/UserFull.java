package ru.tech_mail.forum.responses;

import java.util.List;

public class UserFull extends User {
    private List<String> followers;
    private List<String> following;
    private List<Integer> subscriptions;

    public UserFull(int id, String email, String username, String name, String about, boolean isAnonymous) {
        super(id, email, username, name, about, isAnonymous);
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
