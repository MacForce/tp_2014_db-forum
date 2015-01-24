package ru.tech_mail.forum.DAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ThreadDAO {

    void close(HttpServletRequest request, HttpServletResponse response);

    void create(HttpServletRequest request, HttpServletResponse response);

    void details(HttpServletRequest request, HttpServletResponse response);

    void list(HttpServletRequest request, HttpServletResponse response);

    void listPosts(HttpServletRequest request, HttpServletResponse response);

    void open(HttpServletRequest request, HttpServletResponse response);

    void remove(HttpServletRequest request, HttpServletResponse response);

    void restore(HttpServletRequest request, HttpServletResponse response);

    void subscribe(HttpServletRequest request, HttpServletResponse response);

    void unsubscribe(HttpServletRequest request, HttpServletResponse response);

    void update(HttpServletRequest request, HttpServletResponse response);

    void vote(HttpServletRequest request, HttpServletResponse response);
}
