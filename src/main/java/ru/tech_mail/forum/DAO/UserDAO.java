package ru.tech_mail.forum.DAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface UserDAO {

    void create(HttpServletRequest request, HttpServletResponse response);

    void details(HttpServletRequest request, HttpServletResponse response);

    void follow(HttpServletRequest request, HttpServletResponse response);

    void listFollowers(HttpServletRequest request, HttpServletResponse response);

    void listFollowing(HttpServletRequest request, HttpServletResponse response);

    void listPosts(HttpServletRequest request, HttpServletResponse response);

    void unfollow(HttpServletRequest request, HttpServletResponse response);

    void updateProfile(HttpServletRequest request, HttpServletResponse response);
}
