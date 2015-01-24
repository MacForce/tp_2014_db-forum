package ru.tech_mail.forum.server;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tech_mail.forum.DAO.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Servlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(Servlet.class);
    private final Server server;
    private final ClearDAO clearDAO;
    private final ForumDAO forumDAO;
    private final PostDAO postDAO;
    private final ThreadDAO threadDAO;
    private final UserDAO userDAO;

    public Servlet(Server server, ClearDAO clearDAO, ForumDAO forumDAO, PostDAO postDAO, ThreadDAO threadDAO, UserDAO userDAO) {
        this.server = server;
        this.clearDAO = clearDAO;
        this.forumDAO = forumDAO;
        this.postDAO = postDAO;
        this.threadDAO = threadDAO;
        this.userDAO = userDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getRequestURI()) {
            case "/db/api/forum/details/" :
                forumDAO.details(request, response);
                break;
            case "/db/api/forum/listPosts/" :
                forumDAO.listPosts(request, response);
                break;
            case "/db/api/forum/listThreads/" :
                forumDAO.listThreads(request, response);
                break;
            case "/db/api/forum/listUsers/" :
                forumDAO.listUsers(request, response);
                break;
            case "/db/api/post/details/" :
                postDAO.details(request, response);
                break;
            case "/db/api/post/list/" :
                postDAO.list(request, response);
                break;
            case "/db/api/user/details/" :
                userDAO.details(request, response);
                break;
            case "/db/api/user/listFollowers/" :
                userDAO.listFollowers(request, response);
                break;
            case "/db/api/user/listFollowing/" :
                userDAO.listFollowing(request, response);
                break;
            case "/db/api/user/listPosts/" :
                userDAO.listPosts(request, response);
                break;
            case "/db/api/thread/details/" :
                threadDAO.details(request, response);
                break;
            case "/db/api/thread/list/" :
                threadDAO.list(request, response);
                break;
            case "/db/api/thread/listPosts/" :
                threadDAO.listPosts(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                LOG.debug("BAD REQUEST at Post-request!\n" + request.getRequestURI());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getRequestURI()) {
            case "db/api/shutdown_server" :
                try {
                    server.stop();
                } catch (Exception e) {
                    LOG.error("Can't correctly stop server!!!", e);
                    System.exit(1);
                }
            case "/db/api/clear/" :
                clearDAO.clear(request, response);
                break;
            case "/db/api/forum/create/" :
                forumDAO.create(request, response);
                break;
            case "/db/api/post/create/" :
                postDAO.create(request, response);
                break;
            case "/db/api/post/remove/" :
                postDAO.remove(request, response);
                break;
            case "/db/api/post/restore/" :
                postDAO.restore(request, response);
                break;
            case "/db/api/post/update/" :
                postDAO.update(request, response);
                break;
            case "/db/api/post/vote/" :
                postDAO.vote(request, response);
                break;
            case "/db/api/user/create/" :
                userDAO.create(request, response);
                break;
            case "/db/api/user/follow/" :
                userDAO.follow(request, response);
                break;
            case "/db/api/user/unfollow/" :
                userDAO.unfollow(request, response);
                break;
            case "/db/api/user/updateProfile/" :
                userDAO.updateProfile(request, response);
                break;
            case "/db/api/thread/close/" :
                threadDAO.close(request, response);
                break;
            case "/db/api/thread/create/" :
                threadDAO.create(request, response);
                break;
            case "/db/api/thread/open/" :
                threadDAO.open(request, response);
                break;
            case "/db/api/thread/remove/" :
                threadDAO.remove(request, response);
                break;
            case "/db/api/thread/restore/" :
                threadDAO.restore(request, response);
                break;
            case "/db/api/thread/subscribe/" :
                threadDAO.subscribe(request, response);
                break;
            case "/db/api/thread/unsubscribe/" :
                threadDAO.unsubscribe(request, response);
                break;
            case "/db/api/thread/update/" :
                threadDAO.update(request, response);
                break;
            case "/db/api/thread/vote/" :
                threadDAO.vote(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                LOG.debug("BAD REQUEST at Post-request!\n" + request.getRequestURI());
        }
    }
}