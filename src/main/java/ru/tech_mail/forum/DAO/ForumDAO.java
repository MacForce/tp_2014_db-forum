package ru.tech_mail.forum.DAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ForumDAO {

    void create(HttpServletRequest request, HttpServletResponse response);

    void details(HttpServletRequest request, HttpServletResponse response);

    void listPosts(HttpServletRequest request, HttpServletResponse response);

    void listThreads(HttpServletRequest request, HttpServletResponse response);

    void listUsers(HttpServletRequest request, HttpServletResponse response);
}
