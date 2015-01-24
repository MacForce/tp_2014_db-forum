package ru.tech_mail.forum.DAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ClearDAO {

    void clear(HttpServletRequest request, HttpServletResponse response);
}
