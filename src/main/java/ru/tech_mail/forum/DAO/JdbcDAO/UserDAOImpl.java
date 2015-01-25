package ru.tech_mail.forum.DAO.JdbcDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tech_mail.forum.DAO.UserDAO;
import ru.tech_mail.forum.exceptions.DuplicateDataException;
import ru.tech_mail.forum.exceptions.WrongDataException;
import ru.tech_mail.forum.responses.BaseResponse;
import ru.tech_mail.forum.responses.PostFull;
import ru.tech_mail.forum.responses.User;
import ru.tech_mail.forum.responses.UserFull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.text.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserDAOImpl implements UserDAO {
    private static final Logger LOG = LoggerFactory.getLogger(UserDAOImpl.class);
    private final ConnectionPool connectionPool;

    public UserDAOImpl(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public void create(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("username") && params.containsKey("about") && params.containsKey("name") && params.containsKey("email")) {
            Connection connection = connectionPool.getConnection();
            try {
                String value;
                String query = String.format("INSERT INTO User(email, username, name, about, isAnonymous) VALUES(\"%s\", %s, %s, %s, %b)",
                        Common.escapeInjections((String) params.get("email")),
                        (value = Common.escapeInjections((String) params.get("username"))) == null ?
                                null
                                : "\"" + value +"\"",
                        (value = Common.escapeInjections((String) params.get("name"))) == null ?
                                null
                                : "\"" + value +"\"",
                        (value = Common.escapeInjections((String) params.get("about"))) == null ?
                                null
                                : "\"" + value +"\"",
                        params.containsKey("isAnonymous") ? (Boolean)params.get("isAnonymous") : false);
                Integer userId;
                if ((userId = TExecutor.execUpdateGetId(connection, query)) != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0,
                            new User(userId, (String) params.get("email"), (String) params.get("username"),
                                    (String) params.get("name"), (String) params.get("about"),
                                    params.containsKey("isAnonymous") ? (Boolean)params.get("isAnonymous") : false)));
                }  else {
                    Common.addNotCorrect(response);
                }
            } catch (WrongDataException e) {
                LOG.error("WrongDataException exception!");
                Common.addNotCorrect(response);
            } catch (DuplicateDataException e) {
                LOG.error("DuplicateDataException exception!");
                Common.addExists(response);
            } catch (Exception e) {
                LOG.error("Some exception!", e);
                Common.addNotCorrect(response);
            } finally {
                connectionPool.returnConnection(connection);
            }
        } else {
            Common.addNotCorrect(response);
        }
    }

    @Override
    public void details(HttpServletRequest request, HttpServletResponse response) {
        String userEmail = Common.escapeInjections(request.getParameter("user"));
        if (userEmail == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        UserFull user = null;
        try {
            user = getUserDetails(connection, userEmail);
        } catch (WrongDataException e) {
            Common.addNotFound(response);
        } finally {
            connectionPool.returnConnection(connection);
        }
        if (user != null) {
            Common.addToResponse(response, new BaseResponse<>((byte) 0, user));
        } else {
            Common.addNotCorrect(response);
        }
    }

    public static UserFull getUserDetails(Connection connection, String email) throws WrongDataException {
        UserFull user = TExecutor.execQuery(connection,
                String.format("SELECT * FROM User u WHERE u.email = \"%s\"", email),
                (resultSet) -> resultSet.next() ? new UserFull(resultSet) : null);
        if (user != null) {
            addAdvancedLists(connection, user);
        }
        return user;
    }

    private static List<String> getFollowers(Connection connection, String email) {
        List<String> followersEmails;
        try {
            followersEmails = TExecutor.execQuery(connection,
                    String.format("SELECT follower FROM Follow  WHERE followee = \"%s\"", email),
                    (resultSet) -> {
                        List<String> data = new ArrayList<>();
                        while (resultSet.next()) {
                            data.add(resultSet.getString(1));
                        }
                        return data;
                    });
        } catch (WrongDataException e) {
            return null;
        }
        return followersEmails;
    }

    private static List<String> getFollowees(Connection connection, String email) {
        List<String> followersEmails;
        try {
            followersEmails = TExecutor.execQuery(connection,
                    String.format("SELECT followee FROM Follow WHERE follower = \"%s\"", email),
                    (resultSet) -> {
                        List<String> data = new ArrayList<>();
                        while (resultSet.next()) {
                            data.add(resultSet.getString(1));
                        }
                        return data;
                    });
        } catch (WrongDataException e) {
            return null;
        }
        return followersEmails;
    }

    private static List<Integer> getSubscriptions(Connection connection, String email) {
        List<Integer> subscriptions;
        try {
            subscriptions = TExecutor.execQuery(connection,
                    String.format("SELECT thread FROM Subscribe WHERE user = \"%s\"", email),
                    (resultSet) -> {
                        List<Integer> data = new ArrayList<>();
                        while (resultSet.next()) {
                            data.add(resultSet.getInt(1));
                        }
                        return data;
                    });
        } catch (WrongDataException e) {
            return null;
        }
        return subscriptions;
    }

    public static void addAdvancedLists(Connection connection, UserFull user) {
        List<String> usersList = getFollowers(connection, user.getEmail());
        user.setFollowers(usersList == null ? new ArrayList<>() : usersList);
        usersList = getFollowees(connection, user.getEmail());
        user.setFollowing(usersList == null ? new ArrayList<>() : usersList);
        List<Integer> subscriptions = getSubscriptions(connection, user.getEmail());
        user.setSubscriptions(subscriptions == null ? new ArrayList<>() : subscriptions);
    }

    @Override
    public void follow(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("follower") && params.containsKey("followee")) {
            Connection connection = connectionPool.getConnection();
            try {
                TExecutor.execUpdate(connection, String.format("INSERT INTO Follow(followee, follower) VALUES(\"%s\", \"%s\")",
                        Common.escapeInjections((String)params.get("followee")),
                        Common.escapeInjections((String) params.get("follower"))));
                UserFull user = getUserDetails(connection, Common.escapeInjections((String)params.get("followee")));
                if (user != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, user));
                } else {
                    Common.addNotFound(response);
                }
            } catch (WrongDataException e) {
                Common.addNotCorrect(response);
            } catch (DuplicateDataException e) {
                Common.addExists(response);
            } catch (Exception e) {
                LOG.error("Some exception!", e);
                Common.addNotCorrect(response);
            } finally {
                connectionPool.returnConnection(connection);
            }
        } else {
            Common.addNotCorrect(response);
        }
    }

    @Override
    public void listFollowers(HttpServletRequest request, HttpServletResponse response) {
        sendUsersList(request, response, "SELECT * FROM User u INNER JOIN Follow fo ON u.email = fo.follower WHERE fo.followee = \"%s\"");
    }

    @Override
    public void listFollowing(HttpServletRequest request, HttpServletResponse response) {
        sendUsersList(request, response, "SELECT * FROM User u INNER JOIN Follow fo ON u.email = fo.followee WHERE fo.follower = \"%s\"");
    }

    private void sendUsersList(HttpServletRequest request, HttpServletResponse response, String selectQuery) {
        String userEmail = Common.escapeInjections(request.getParameter("user"));
        if (userEmail == null) {
            Common.addNotValid(response);
            return;
        }
        String query = String.format(selectQuery, userEmail);
        query = addOptionalUsesParams(request, response, query);
        if (query == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        List<UserFull> userList;
        try {
            userList = TExecutor.execQuery(connection, query, (resultSet) -> {
                List<UserFull> data = new ArrayList<>();
                while (resultSet.next()) {
                    data.add(new UserFull(resultSet));
                }
                return data;
            });
        } catch (WrongDataException e) {
            LOG.error("Can't get list of followers/followee!", e);
            Common.addNotCorrect(response);
            connectionPool.returnConnection(connection);
            return;
        }
        for (UserFull user : userList) {
            addAdvancedLists(connection, user);
        }
        connectionPool.returnConnection(connection);
        Common.addToResponse(response, new BaseResponse<>((byte) 0, userList));
    }

    private String addOptionalUsesParams(HttpServletRequest request, HttpServletResponse response, String query) {
        String sinceId = Common.escapeInjections(request.getParameter("since_id"));
        if (sinceId != null) {
            try {
                query += String.format(" AND u.id >= %d", Integer.valueOf(sinceId));
            } catch (NumberFormatException e) {
                LOG.error("Can't parse parameter \"since_id\" : " + sinceId, e);
                Common.addNotValid(response);
                return null;
            }
        }
        String sorting = request.getParameter("order");
        if (sorting != null) {
            switch (sorting) {
                case "asc" :
                    query += " ORDER BY u.name ASC";
                    break;
                case "desc" :
                    query += " ORDER BY u.name DESC";
                    break;
                default :
                    Common.addNotValid(response);
                    return null;
            }
        } else {
            query += " ORDER BY u.name DESC";
        }
        String limit = request.getParameter("limit");
        if (limit != null) {
            try {
                query += String.format(" LIMIT %d", Integer.valueOf(limit));
            } catch (NumberFormatException e) {
                LOG.error("Can't parse parameter \"limit\" : " + limit, e);
                Common.addNotValid(response);
                return null;
            }
        }
        return query;
    }

    @Override
    public void listPosts(HttpServletRequest request, HttpServletResponse response) {
        String userEmail = Common.escapeInjections(request.getParameter("user"));
        if (userEmail == null) {
            Common.addNotValid(response);
            return;
        }
        String query = String.format("SELECT * FROM Post p WHERE p.user=\"%s\"", userEmail);
        query = addOptionalPostsParams(request, response, query);
        if (query == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        try {
            List<PostFull> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                List<PostFull> data = new ArrayList<>();
                while (resultSet.next()) {
                    data.add(new PostFull<>(resultSet, resultSet.getString("p.forum"), userEmail, resultSet.getInt("p.thread")));
                }
                return data;
            });
            Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
        } catch (WrongDataException e) {
            LOG.error("Can't get list of posts by user!", e);
            Common.addNotCorrect(response);
        } finally {
            connectionPool.returnConnection(connection);
        }
    }

    private String addOptionalPostsParams(HttpServletRequest request, HttpServletResponse response, String query) {
        String sinceDate = request.getParameter("since");
        if (sinceDate != null) {
            try {
                new SimpleDateFormat("YYYY-MM-DD hh:mm:ss").parse(sinceDate); //only for check valid format
                query += String.format(" AND p.date >= \"%s\"", sinceDate);
            } catch (ParseException e) {
                LOG.error("Can't parse parameter \"since\" : " + sinceDate, e);
                Common.addNotValid(response);
                return null;
            }
        }
        String sorting = request.getParameter("order");
        if (sorting != null) {
            switch (sorting) {
                case "asc" :
                    query += " ORDER BY p.date ASC";
                    break;
                case "desc" :
                    query += " ORDER BY p.date DESC";
                    break;
                default :
                    Common.addNotValid(response);
                    return null;
            }
        } else {
            query += " ORDER BY p.date DESC";
        }
        String limit = request.getParameter("limit");
        if (limit != null) {
            try {
                query += String.format(" LIMIT %d", Integer.valueOf(limit));
            } catch (NumberFormatException e) {
                LOG.error("Can't parse parameter \"limit\" : " + limit, e);
                Common.addNotValid(response);
                return null;
            }
        }
        return query;
    }

    @Override
    public void unfollow(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("follower") && params.containsKey("followee")) {
            Connection connection = connectionPool.getConnection();
            try {
                TExecutor.execUpdate(connection, String.format("DELETE FROM Follow WHERE followee = \"%s\" AND follower = \"%s\"",
                        Common.escapeInjections((String)params.get("followee")),
                        Common.escapeInjections((String) params.get("follower"))));
                UserFull user = getUserDetails(connection, Common.escapeInjections((String)params.get("followee")));
                if (user != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, user));
                } else {
                    Common.addNotFound(response);
                }
            } catch (WrongDataException e) {
                Common.addNotCorrect(response);
            } catch (DuplicateDataException e) {
                Common.addExists(response);
            } catch (Exception e) {
                LOG.error("Some exception!", e);
                Common.addNotCorrect(response);
            } finally {
                connectionPool.returnConnection(connection);
            }
        } else {
            Common.addNotCorrect(response);
        }
    }

    @Override
    public void updateProfile(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("about") && params.containsKey("name") && params.containsKey("user")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("UPDATE User SET about = \"%s\", name = \"%s\" WHERE email = \"%s\"",
                        Common.escapeInjections((String)params.get("about")),
                        Common.escapeInjections((String)params.get("name")),
                        Common.escapeInjections((String)params.get("user")));
                TExecutor.execUpdate(connection, query);
                UserFull user = getUserDetails(connection, Common.escapeInjections((String)params.get("user")));
                if (user != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, user));
                } else {
                    Common.addNotFound(response);
                }
            } catch (WrongDataException e) {
                Common.addNotCorrect(response);
            } catch (DuplicateDataException e) {
                Common.addExists(response);
            } catch (Exception e) {
                LOG.error("Some exception!", e);
                Common.addNotCorrect(response);
            } finally {
                connectionPool.returnConnection(connection);
            }
        } else {
            Common.addNotCorrect(response);
        }
    }
}
