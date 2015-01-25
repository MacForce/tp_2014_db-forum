package ru.tech_mail.forum.DAO.JdbcDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tech_mail.forum.DAO.ForumDAO;
import ru.tech_mail.forum.exceptions.DuplicateDataException;
import ru.tech_mail.forum.exceptions.WrongDataException;
import ru.tech_mail.forum.responses.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ForumDAOImpl implements ForumDAO {
    private static final Logger LOG = LoggerFactory.getLogger(ClearDAOImpl.class);
    private final ConnectionPool connectionPool;

    public ForumDAOImpl(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public void create(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("name") && params.containsKey("short_name") && params.containsKey("user")) {
            Connection connection = connectionPool.getConnection();
            Integer forumId;
            try {
                if ((forumId = TExecutor.execUpdateGetId(connection,
                        String.format("INSERT INTO Forum(name, short_name, user) VALUES(\"%s\", \"%s\", \"%s\")",
                                Common.escapeInjections((String)params.get("name")),
                                Common.escapeInjections((String)params.get("short_name")),
                                Common.escapeInjections((String)params.get("user"))))) != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0,
                            new Forum<>(forumId, Common.escapeInjections((String)params.get("name")),
                                    Common.escapeInjections((String)params.get("short_name")),
                                    Common.escapeInjections((String)params.get("user")))));
                } else {
                    Common.addNotCorrect(response);
                }
            } catch (WrongDataException e) {
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

    public static Forum getForumDetails(Connection connection, String shortName) throws WrongDataException {
        return TExecutor.execQuery(connection,
                String.format("SELECT * FROM Forum f WHERE f.short_name = \"%s\"", shortName),
                (resultSet) -> resultSet.next() ? new Forum<>(resultSet, resultSet.getString("f.user")) : null);
    }

    @Override
    public void details(HttpServletRequest request, HttpServletResponse response) {
        String shortName = Common.escapeInjections(request.getParameter("forum"));
        if (shortName == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        String related = request.getParameter("related");
        String query;
        try {
            if (related != null) {
                if (related.equals("user")) {
                    query = String.format("SELECT * FROM Forum f INNER JOIN User u ON f.user=u.email WHERE f.short_name=\"%s\"", shortName);
                    Forum<UserFull> forum = TExecutor.execQuery(connection, query,
                            (resultSet) -> resultSet.next() ? new Forum<>(resultSet, new UserFull(resultSet)) : null);
                    if (forum != null) {
                        UserDAOImpl.addAdvancedLists(connection, forum.getUser());
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, forum));
                    } else {
                        Common.addNotFound(response);
                    }
                } else {
                    Common.addNotValid(response);
                }
            } else {
                query = String.format("SELECT * FROM Forum f WHERE f.short_name = \"%s\"", shortName);
                Forum<String> forum = TExecutor.execQuery(connection, query,
                        (resultSet) -> resultSet.next() ? new Forum<>(resultSet, resultSet.getString("f.user")) : null);
                if (forum != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, forum));
                }
            }
        } catch (WrongDataException e) {
            Common.addNotCorrect(response);
        } catch (Exception e) {
            LOG.error("Something error!", e);
            Common.addNotCorrect(response);
        } finally {
            connectionPool.returnConnection(connection);
        }
    }

    @Override
    public void listPosts(HttpServletRequest request, HttpServletResponse response) {
        String forum = Common.escapeInjections(request.getParameter("forum"));
        if (forum == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        String query = String.format("SELECT * FROM Post p WHERE p.forum=\"%s\"", forum);
        if ((query = addOptionalParams(request, response, "p.date", query)) == null) {
            Common.addNotValid(response);
            return;
        }
        String[] optionalParams = request.getParameterValues("related");
        try {
            if (optionalParams != null) {
                if (optionalParams.length == 1) {
                    if (optionalParams[0].equals("user")) {
                        List<PostFull<String, Integer, UserFull>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostFull<String, Integer, UserFull>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                posts.add(new PostFull<>(resultSet, forum, new UserFull(resultSet.getString("p.user")), resultSet.getInt("p.thread")));
                            }
                            return posts;
                        });
                        if (!postList.isEmpty() && postList.size() < 20) {
                            for (PostFull<String, Integer, UserFull> post : postList) {
                                post.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull)post.getUser()).getEmail()));
                            }
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    if (optionalParams[0].equals("forum")) {
                        List<PostFull<Forum<String>, Integer, String>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostFull<Forum<String>, Integer, String>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                posts.add( new PostFull<>(resultSet, new Forum<>(resultSet.getString("p.forum")),
                                        resultSet.getString("p.user"), resultSet.getInt("p.thread")));
                            }
                            return posts;
                        });
                        if (!postList.isEmpty() && postList.size() < 20) {
                            for (PostFull<Forum<String>, Integer, String> post : postList) {
                                post.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum<String>)post.getForum()).getShort_name()));
                            }
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    if (optionalParams[0].equals("thread")) {
                        List<PostFull<String, ThreadFull, String>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostFull<String, ThreadFull, String>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                posts.add(new PostFull<>(resultSet, resultSet.getString("p.forum"),
                                        resultSet.getString("p.user"), new ThreadFull<>(resultSet.getInt("p.thread"))));
                            }
                            return posts;
                        });
                        if (!postList.isEmpty() && postList.size() < 20) {
                            for (PostFull<String, ThreadFull, String> post : postList) {
                                post.setThread(ThreadDAOImpl.getThreadDetails(connection, ((ThreadFull)post.getThread()).getId()));
                            }
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    Common.addNotValid(response);
                    return;
                }
                List<String> relatedParams = new ArrayList<>(Arrays.asList(optionalParams));
                if (optionalParams.length == 2) {
                    if (relatedParams.contains("user") && relatedParams.contains("forum")) {
                        List<PostFull<Forum<String>, Integer, UserFull>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostFull<Forum<String>, Integer, UserFull>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                posts.add(new PostFull<>(resultSet, new Forum<>(resultSet.getString("p.forum")),
                                        new UserFull(resultSet.getString("p.user")), resultSet.getInt("p.thread")));
                            }
                            return posts;
                        });
                        if (!postList.isEmpty() && postList.size() < 20) {
                            for (PostFull<Forum<String>, Integer, UserFull> post : postList) {
                                post.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum<String>) post.getForum()).getShort_name()));
                                post.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull)post.getUser()).getEmail()));
                            }
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    if (relatedParams.contains("user") && relatedParams.contains("thread")) {
                        List<PostFull<String, ThreadFull, UserFull>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostFull<String, ThreadFull, UserFull>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                posts.add(new PostFull<>(resultSet, resultSet.getString("p.forum"),
                                        new UserFull(resultSet.getString("p.user")), new ThreadFull<>(resultSet.getInt("p.thread"))));
                            }
                            return posts;
                        });
                        if (!postList.isEmpty() && postList.size() < 20) {
                            for (PostFull<String, ThreadFull, UserFull> post : postList) {
                                post.setThread(ThreadDAOImpl.getThreadDetails(connection, ((ThreadFull) post.getThread()).getId()));
                                post.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull)post.getUser()).getEmail()));
                            }
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    if (relatedParams.contains("forum") && relatedParams.contains("thread")) {
                        List<PostFull<Forum<String>, ThreadFull, String>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostFull<Forum<String>, ThreadFull, String>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                posts.add(new PostFull<>(resultSet, new Forum<>(resultSet.getString("p.forum")),
                                        resultSet.getString("p.user"), new ThreadFull<>(resultSet.getInt("p.thread"))));
                            }
                            return posts;
                        });
                        if (!postList.isEmpty() && postList.size() < 20) {
                            for (PostFull<Forum<String>, ThreadFull, String> post : postList) {
                                post.setThread(ThreadDAOImpl.getThreadDetails(connection, ((ThreadFull) post.getThread()).getId()));
                                post.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum<String>) post.getForum()).getShort_name()));
                            }
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    Common.addNotValid(response);
                    return;
                }
                if (optionalParams.length == 3 && relatedParams.contains("user") &&
                        relatedParams.contains("forum") && relatedParams.contains("thread")) {
                    List<PostFull<Forum<String>, ThreadFull, UserFull>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                        List<PostFull<Forum<String>, ThreadFull, UserFull>> posts = new ArrayList<>();
                        while (resultSet.next()) {
                            posts.add(new PostFull<>(resultSet, new Forum<>(resultSet.getString("p.forum")),
                                    new UserFull(resultSet.getString("p.user")), new ThreadFull<>(resultSet.getInt("p.thread"))));
                        }
                        return posts;
                    });
                    if (!postList.isEmpty() && postList.size() < 20) {
                        for (PostFull<Forum<String>, ThreadFull, UserFull> post : postList) {
                            post.setThread(ThreadDAOImpl.getThreadDetails(connection, ((ThreadFull) post.getThread()).getId()));
                            post.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum<String>) post.getForum()).getShort_name()));
                            post.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull)post.getUser()).getEmail()));
                        }
                    }
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                    return;
                }
                Common.addNotValid(response);
            } else {
                List<PostFull> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                    List<PostFull> posts = new ArrayList<>();
                    while (resultSet.next()) {
                        posts.add(new PostFull<>(resultSet, resultSet.getString("p.forum"), resultSet.getString("p.user"), resultSet.getInt("p.thread")));
                    }
                    return posts;
                });
                Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
            }
        } catch (WrongDataException e) {
            LOG.error("Can't get list of posts by user!", e);
            Common.addNotCorrect(response);
        } finally {
            connectionPool.returnConnection(connection);
        }
    }

    private String addOptionalParams(HttpServletRequest request, HttpServletResponse response, String dateField, String query) {
        String sinceDate = request.getParameter("since");
        if (sinceDate != null) {
            try {
                new SimpleDateFormat("YYYY-MM-DD hh:mm:ss").parse(sinceDate); //only for check valid format
                query += String.format(" AND " + dateField + " >= \"%s\"", sinceDate);
            } catch (ParseException e) {
                LOG.error("Can't parse parameter \"since\" : " + sinceDate, e);
                Common.addNotValid(response);
                return null;
            }
        }
        String sorting = request.getParameter("order");
        if (sorting != null) {
            switch (sorting) {
                case "asc":
                    query += " ORDER BY " + dateField + " ASC";
                    break;
                case "desc":
                    query += " ORDER BY " + dateField + " DESC";
                    break;
                default:
                    Common.addNotValid(response);
                    return null;
            }
        } else {
            query += " ORDER BY " + dateField + " DESC";
        }
//        String limit = request.getParameter("limit");
//        if (limit != null) {
//            try {
//                query += String.format(" LIMIT %d", Integer.valueOf(limit));
//            } catch (NumberFormatException e) {
//                LOG.error("Can't parse parameter \"limit\" : " + limit, e);
//                Common.addNotValid(response);
//                return null;
//            }
//        } else {
            query += " LIMIT 30";
//        }
        return query;
    }

    @Override
    public void listThreads(HttpServletRequest request, HttpServletResponse response) {
        String forum = Common.escapeInjections(request.getParameter("forum"));
        if (forum == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        String query = String.format("SELECT * FROM Thread t WHERE t.forum=\"%s\"", forum);
        if ((query= addOptionalParams(request, response, "t.date", query)) == null) {
            Common.addNotValid(response);
            return;
        }
        String[] optionalParams = request.getParameterValues("related");
        try {
            if (optionalParams != null && optionalParams.length > 0) {
                if (optionalParams.length == 1) {
                    if (optionalParams[0].equals("user")) {
                        List<ThreadFull<String, UserFull>> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<ThreadFull<String, UserFull>> threads = new ArrayList<>();
                            while (resultSet.next()) {
                                threads.add(new ThreadFull<>(resultSet, resultSet.getString("t.forum"), new UserFull(resultSet.getString("t.user"))));
                            }
                            return threads;
                        });
                        if (!threadList.isEmpty() && threadList.size() < 20) {
                            for (ThreadFull<String, UserFull> thread : threadList) {
                                thread.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull)thread.getUser()).getEmail()));
                            }
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
                    } else {
                        if (optionalParams[0].equals("forum")) {
                            List<ThreadFull<Forum<String>, String>> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                                List<ThreadFull<Forum<String>, String>> threads = new ArrayList<>();
                                while (resultSet.next()) {
                                    threads.add(new ThreadFull<>(resultSet, new Forum<>(resultSet.getString("t.forum")), resultSet.getString("t.user")));
                                }
                                return threads;
                            });
                            if (!threadList.isEmpty() && threadList.size() < 20) {
                                for (ThreadFull<Forum<String>, String> thread : threadList) {
                                    thread.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum)thread.getForum()).getShort_name()));
                                }
                            }
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
                        } else {
                            Common.addNotValid(response);
                        }
                    }
                } else {
                    if (optionalParams.length == 2 &&
                            ((optionalParams[0].equals("user") && optionalParams[1].equals("forum")) ||
                                    (optionalParams[1].equals("user") && optionalParams[0].equals("forum")))) {
                        List<ThreadFull<Forum<String>, UserFull>> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<ThreadFull<Forum<String>, UserFull>> threads = new ArrayList<>();
                            while (resultSet.next()) {
                                threads.add(new ThreadFull<>(resultSet, new Forum<>(resultSet.getString("t.forum")), new UserFull(resultSet.getString("t.user"))));
                            }
                            return threads;
                        });
                        if (!threadList.isEmpty() && threadList.size() < 20) {
                            for (ThreadFull<Forum<String>, UserFull> thread : threadList) {
                                thread.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum)thread.getForum()).getShort_name()));
                                thread.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull)thread.getUser()).getEmail()));
                            }
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
                        return;
                    }
                    Common.addNotValid(response);
                }
            } else {
                List<ThreadFull<String, String>> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                    List<ThreadFull<String, String>> threads = new ArrayList<>();
                    while (resultSet.next()) {
                    threads.add(new ThreadFull<>(resultSet, resultSet.getString("t.forum"), resultSet.getString("t.user")));
                    }
                    return threads;
                });
                Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
            }
        } catch (WrongDataException e) {
            LOG.error("Can't get list of posts by user!", e);
            Common.addNotCorrect(response);
        } catch (Exception e) {
            LOG.error("Something error!", e);
            Common.addNotCorrect(response);
        } finally {
            connectionPool.returnConnection(connection);
        }
    }

    @Override
    public void listUsers(HttpServletRequest request, HttpServletResponse response) {
        String forumShortName = Common.escapeInjections(request.getParameter("forum"));
        if (forumShortName == null) {
            Common.addNotValid(response);
            return;
        }
        String query = String.format("SELECT DISTINCT u.id, u.email, u.username, u.name, u.about, u.isAnonymous" +
                " FROM Post p INNER JOIN User u ON p.user=u.email WHERE p.forum=\"%s\"", forumShortName);
        if ((query = addOptionalUsersParams(request, response, query)) == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        try {
            List<UserFull> userList = TExecutor.execQuery(connection, query, (resultSet) -> {
                List<UserFull> data = new ArrayList<>();
                while (resultSet.next()) {
                    data.add(new UserFull(resultSet));
                }
                return data;
            });
            if (!userList.isEmpty() && userList.size() < 20) {
                for (UserFull user : userList) {
                    UserDAOImpl.addAdvancedLists(connection, user);
                }
            }
            Common.addToResponse(response, new BaseResponse<>((byte) 0, userList));
        } catch (WrongDataException e) {
            LOG.error("Can't get list of users for forum!", e);
            Common.addNotCorrect(response);
        } catch (Exception e) {
            LOG.error("Something error!", e);
            Common.addNotCorrect(response);
        } finally {
            connectionPool.returnConnection(connection);
        }
    }

    private String addOptionalUsersParams(HttpServletRequest request, HttpServletResponse response, String query) {
        String sinceId = request.getParameter("since_id");
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
//        String limit = request.getParameter("limit");
//        if (limit != null) {
//            try {
//                query += String.format(" LIMIT %d", Integer.valueOf(limit));
//            } catch (NumberFormatException e) {
//                LOG.error("Can't parse parameter \"limit\" : " + limit, e);
//                Common.addNotValid(response);
//                return null;
//            }
//        }
        query +=" LIMIT 30";
        return query;
    }
}
