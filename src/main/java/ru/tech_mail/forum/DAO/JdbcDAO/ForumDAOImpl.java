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
                        String.format("INSERT INTO forum(name, short_name, user) VALUES(\"%s\", \"%s\", \"%s\")",
                                Common.escapeInjections((String)params.get("name")),
                                Common.escapeInjections((String)params.get("short_name")),
                                Common.escapeInjections((String)params.get("user"))))) != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0,
                            new Forum<>(forumId, Common.escapeInjections((String)params.get("name")),
                                    Common.escapeInjections((String)params.get("short_name")),
                                    Common.escapeInjections((String) params.get("user")))));
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
                    query = String.format("SELECT * FROM forum f INNER JOIN user u ON f.user=u.email WHERE f.short_name=\"%s\"", shortName);
                    Forum<UserFull> forum = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                            new Forum<>(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                    new UserFull(resultSet.getInt(5), resultSet.getString(6),
                                    resultSet.getString(7), resultSet.getString(8),
                                    resultSet.getString(9), resultSet.getBoolean(10)))
                            : null);
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
                query = String.format("SELECT * FROM forum WHERE short_name = \"%s\"", shortName);
                Forum<String> forum = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new Forum<>(resultSet.getInt(1), resultSet.getString(2),
                                resultSet.getString(3), resultSet.getString(4))
                        : null);
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
        String query;
        String[] optionalParams = request.getParameterValues("related");
        try {
            if (optionalParams != null) {
                if (optionalParams.length == 1) {
                    if (optionalParams[0].equals("user")) {
                        query = String.format("SELECT * FROM post p INNER JOIN user u ON p.user=u.email WHERE p.forum=\"%s\"", forum);
                        query = addOptionalParams(request, response, "p.date", query);
                        if (query == null) {
                            Common.addNotValid(response);
                            return;
                        }
                        List<PostAdvanced<String, Integer, UserFull>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostAdvanced<String, Integer, UserFull>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                PostAdvanced<String, Integer, UserFull> data = new PostAdvanced<>(resultSet.getInt(1),
                                        resultSet.getString(2), resultSet.getInt(5), resultSet.getInt(7),
                                        resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10),
                                        resultSet.getBoolean(11), resultSet.getBoolean(12), resultSet.getBoolean(13),
                                        resultSet.getBoolean(14), resultSet.getString(15));
                                data.setForum(resultSet.getString(3));
                                data.setThread(resultSet.getInt(6));
                                data.setUser(new UserFull(resultSet.getInt(16), resultSet.getString(17),
                                        resultSet.getString(18), resultSet.getString(19),
                                        resultSet.getString(20), resultSet.getBoolean(21)));
                                posts.add(data);
                            }
                            return posts;
                        });
                        for (PostAdvanced<String, Integer, UserFull> post : postList) {
                            UserDAOImpl.addAdvancedLists(connection, post.getUser());
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    if (optionalParams[0].equals("forum")) {
                        query = String.format("SELECT * FROM post p INNER JOIN forum f ON p.forum=f.short_name WHERE p.forum=\"%s\"", forum);
                        query = addOptionalParams(request, response, "p.date", query);
                        if (query == null) {
                            Common.addNotValid(response);
                            return;
                        }
                        List<PostAdvanced<Forum<String>, Integer, String>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostAdvanced<Forum<String>, Integer, String>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                PostAdvanced<Forum<String>, Integer, String> data = new PostAdvanced<>(resultSet.getInt(1),
                                        resultSet.getString(2), resultSet.getInt(5), resultSet.getInt(7),
                                        resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10),
                                        resultSet.getBoolean(11), resultSet.getBoolean(12), resultSet.getBoolean(13),
                                        resultSet.getBoolean(14), resultSet.getString(15));
                                data.setForum(new Forum<>(resultSet.getInt(16), resultSet.getString(17),
                                        resultSet.getString(18), resultSet.getString(19)));
                                data.setThread(resultSet.getInt(6));
                                data.setUser(resultSet.getString(4));
                                posts.add(data);
                            }
                            return posts;
                        });
                        if (postList != null) {
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (optionalParams[0].equals("thread")) {
                        query = String.format("SELECT * FROM post p INNER JOIN thread t ON p.thread=t.id WHERE p.forum=\"%s\"", forum);
                        query = addOptionalParams(request, response, "p.date", query);
                        if (query == null) {
                            Common.addNotValid(response);
                            return;
                        }
                        List<PostAdvanced<String, ThreadFull, String>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostAdvanced<String, ThreadFull, String>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                PostAdvanced<String, ThreadFull, String> data = new PostAdvanced<>(resultSet.getInt(1),
                                        resultSet.getString(2), resultSet.getInt(5), resultSet.getInt(7),
                                        resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10),
                                        resultSet.getBoolean(11), resultSet.getBoolean(12), resultSet.getBoolean(13),
                                        resultSet.getBoolean(14), resultSet.getString(15));
                                data.setForum(resultSet.getString(3));
                                data.setThread(new ThreadFull(resultSet.getInt(16), resultSet.getString(17), resultSet.getString(18),
                                        resultSet.getString(19), resultSet.getString(20), resultSet.getInt(21), resultSet.getInt(22),
                                        resultSet.getInt(23), resultSet.getInt(24), resultSet.getBoolean(25), resultSet.getBoolean(26),
                                        resultSet.getString(27), resultSet.getString(28)));
                                data.setUser(resultSet.getString(4));
                                posts.add(data);
                            }
                            return posts;
                        });
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    Common.addNotValid(response);
                    return;
                }
                List<String> relatedParams = new ArrayList<>(Arrays.asList(optionalParams));
                if (optionalParams.length == 2) {
                    if (relatedParams.contains("user") && relatedParams.contains("forum")) {
                        query = String.format("SELECT * FROM forum f INNER JOIN (post p INNER JOIN user u ON p.user=u.email) ON f.short_name=p.forum WHERE p.forum=\"%s\"", forum);
                        query = addOptionalParams(request, response, "p.date", query);
                        if (query == null) {
                            Common.addNotValid(response);
                            return;
                        }
                        List<PostAdvanced<Forum<String>, Integer, UserFull>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostAdvanced<Forum<String>, Integer, UserFull>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                PostAdvanced<Forum<String>, Integer, UserFull> data = new PostAdvanced<>(resultSet.getInt(5),
                                        resultSet.getString(6), resultSet.getInt(9), resultSet.getInt(11),
                                        resultSet.getInt(12), resultSet.getInt(13), resultSet.getBoolean(14),
                                        resultSet.getBoolean(15), resultSet.getBoolean(16), resultSet.getBoolean(17),
                                        resultSet.getBoolean(18), resultSet.getString(19));
                                data.setForum(new Forum<>(resultSet.getInt(1), resultSet.getString(2),
                                        resultSet.getString(3), resultSet.getString(4)));
                                data.setThread(resultSet.getInt(7));
                                data.setUser(new UserFull(resultSet.getInt(20), resultSet.getString(21),
                                        resultSet.getString(22), resultSet.getString(23),
                                        resultSet.getString(24), resultSet.getBoolean(25)));
                                posts.add(data);
                            }
                            return posts;
                        });
                        for (PostAdvanced<Forum<String>, Integer, UserFull> post : postList) {
                            UserDAOImpl.addAdvancedLists(connection, post.getUser());
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    if (relatedParams.contains("user") && relatedParams.contains("thread")) {
                        query = String.format("SELECT * FROM thread t INNER JOIN (post p INNER JOIN user u ON p.user=u.email) ON t.id=p.thread WHERE p.forum=\"%s\"", forum);
                        query = addOptionalParams(request, response, "p.date", query);
                        if (query == null) {
                            Common.addNotValid(response);
                            return;
                        }
                        List<PostAdvanced<String, ThreadFull, UserFull>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostAdvanced<String, ThreadFull, UserFull>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                PostAdvanced<String, ThreadFull, UserFull> data = new PostAdvanced<>(resultSet.getInt(14),
                                        resultSet.getString(15), resultSet.getInt(18), resultSet.getInt(20),
                                        resultSet.getInt(21), resultSet.getInt(22), resultSet.getBoolean(23),
                                        resultSet.getBoolean(24), resultSet.getBoolean(25), resultSet.getBoolean(26),
                                        resultSet.getBoolean(27), resultSet.getString(28));
                                data.setForum(resultSet.getString(16));
                                data.setThread(new ThreadFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                        resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6), resultSet.getInt(7),
                                        resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                                        resultSet.getString(12), resultSet.getString(13)));
                                data.setUser(new UserFull(resultSet.getInt(29), resultSet.getString(30),
                                        resultSet.getString(31), resultSet.getString(32),
                                        resultSet.getString(33), resultSet.getBoolean(34)));
                                posts.add(data);
                            }
                            return posts;
                        });
                        for (PostAdvanced<String, ThreadFull, UserFull> post : postList) {
                            UserDAOImpl.addAdvancedLists(connection, post.getUser());
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    if (relatedParams.contains("forum") && relatedParams.contains("thread")) {
                        query = String.format("SELECT * FROM forum f INNER JOIN (post p INNER JOIN thread t ON p.thread=t.id) ON f.short_name=p.forum WHERE p.forum=\"%s\"", forum);
                        query = addOptionalParams(request, response, "p.date", query);
                        if (query == null) {
                            Common.addNotValid(response);
                            return;
                        }
                        List<PostAdvanced<Forum<String>, ThreadFull, String>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<PostAdvanced<Forum<String>, ThreadFull, String>> posts = new ArrayList<>();
                            while (resultSet.next()) {
                                PostAdvanced<Forum<String>, ThreadFull, String> data = new PostAdvanced<>(resultSet.getInt(5),
                                        resultSet.getString(6), resultSet.getInt(9), resultSet.getInt(11),
                                        resultSet.getInt(12), resultSet.getInt(13), resultSet.getBoolean(14),
                                        resultSet.getBoolean(15), resultSet.getBoolean(16), resultSet.getBoolean(17),
                                        resultSet.getBoolean(18), resultSet.getString(19));
                                data.setForum(new Forum<>(resultSet.getInt(1), resultSet.getString(2),
                                        resultSet.getString(3), resultSet.getString(4)));
                                data.setThread(new ThreadFull(resultSet.getInt(20), resultSet.getString(21), resultSet.getString(22),
                                        resultSet.getString(23), resultSet.getString(24), resultSet.getInt(25), resultSet.getInt(26),
                                        resultSet.getInt(27), resultSet.getInt(28), resultSet.getBoolean(29), resultSet.getBoolean(30),
                                        resultSet.getString(31), resultSet.getString(32)));
                                data.setUser(resultSet.getString(8));
                                posts.add(data);
                            }
                            return posts;
                        });
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                        return;
                    }
                    Common.addNotValid(response);
                    return;
                }
                if (optionalParams.length == 3 && relatedParams.contains("user") &&
                        relatedParams.contains("forum") && relatedParams.contains("thread")) {
                    query = String.format("SELECT * FROM forum f INNER JOIN ( thread t INNER JOIN " +
                            "(post p INNER JOIN user u ON p.user=u.email) ON p.thread=t.id) ON p.forum=f.short_name WHERE p.forum=\"%s\"", forum);
                    query = addOptionalParams(request, response, "p.date", query);
                    if (query == null) {
                        Common.addNotValid(response);
                        return;
                    }
                    List<PostAdvanced<Forum<String>, ThreadFull, UserFull>> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                        List<PostAdvanced<Forum<String>, ThreadFull, UserFull>> posts = new ArrayList<>();
                        while (resultSet.next()) {
                            PostAdvanced<Forum<String>, ThreadFull, UserFull> data = new PostAdvanced<>(resultSet.getInt(18),
                                    resultSet.getString(19), resultSet.getInt(22), resultSet.getInt(24),
                                    resultSet.getInt(25), resultSet.getInt(26), resultSet.getBoolean(27),
                                    resultSet.getBoolean(28), resultSet.getBoolean(29), resultSet.getBoolean(30),
                                    resultSet.getBoolean(31), resultSet.getString(32));
                            data.setForum(new Forum<>(resultSet.getInt(1), resultSet.getString(2),
                                    resultSet.getString(3), resultSet.getString(4)));
                            data.setThread(new ThreadFull(resultSet.getInt(5), resultSet.getString(6), resultSet.getString(7),
                                    resultSet.getString(8), resultSet.getString(9), resultSet.getInt(10), resultSet.getInt(11),
                                    resultSet.getInt(12), resultSet.getInt(13), resultSet.getBoolean(14), resultSet.getBoolean(15),
                                    resultSet.getString(16), resultSet.getString(17)));
                            data.setUser(new UserFull(resultSet.getInt(33), resultSet.getString(34),
                                    resultSet.getString(35), resultSet.getString(36),
                                    resultSet.getString(37), resultSet.getBoolean(38)));
                            posts.add(data);
                        }
                        return posts;
                    });
                    for (PostAdvanced<Forum<String>, ThreadFull, UserFull> post : postList) {
                        UserDAOImpl.addAdvancedLists(connection, post.getUser());
                    }
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
                    return;
                }
                Common.addNotValid(response);
            } else {
                query = String.format("SELECT * FROM post WHERE forum=\"%s\"", forum);
                query = addOptionalParams(request, response, "date", query);
                if (query == null) {
                    Common.addNotValid(response);
                    return;
                }
                List<PostFull> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                    List<PostFull> posts = new ArrayList<>();
                    while (resultSet.next()) {
                        posts.add(new PostFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                resultSet.getString(4), resultSet.getInt(5), resultSet.getInt(6), resultSet.getInt(7),
                                resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                                resultSet.getBoolean(12), resultSet.getBoolean(13), resultSet.getBoolean(14), resultSet.getString(15)));
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
    public void listThreads(HttpServletRequest request, HttpServletResponse response) {
        String forum = Common.escapeInjections(request.getParameter("forum"));
        if (forum == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        String query;
        String[] optionalParams = request.getParameterValues("related");
        try {
            if (optionalParams != null && optionalParams.length > 0) {
                if (optionalParams.length == 1) {
                    if (optionalParams[0].equals("user")) {
                        query = String.format("SELECT * FROM thread t INNER JOIN user u ON t.user=u.email WHERE t.forum=\"%s\"", forum);
                        query = addOptionalParams(request, response, "t.date", query);
                        if (query == null) {
                            Common.addNotValid(response);
                            return;
                        }
                        List<ThreadAdvanced<String, UserFull>> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<ThreadAdvanced<String, UserFull>> threads = new ArrayList<>();
                            while (resultSet.next()) {
                                ThreadAdvanced<String, UserFull> thread = new ThreadAdvanced<>(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                        resultSet.getInt(6), resultSet.getInt(7), resultSet.getInt(8), resultSet.getInt(9),
                                        resultSet.getBoolean(10), resultSet.getBoolean(11), resultSet.getString(12), resultSet.getString(13));
                                thread.setForum(resultSet.getString(4));
                                thread.setUser(new UserFull(resultSet.getInt(14), resultSet.getString(15),
                                        resultSet.getString(16), resultSet.getString(17),
                                        resultSet.getString(18), resultSet.getBoolean(19)));
                                threads.add(thread);
                            }
                            return threads;
                        });
                        for (ThreadAdvanced<String, UserFull> thread : threadList) {
                            UserDAOImpl.addAdvancedLists(connection, thread.getUser());
                        }
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
                    } else {
                        if (optionalParams[0].equals("forum")) {
                            query = String.format("SELECT * FROM thread t INNER JOIN forum f ON t.forum=f.short_name WHERE t.forum=\"%s\"", forum);
                            query = addOptionalParams(request, response, "t.date", query);
                            if (query == null) {
                                Common.addNotValid(response);
                                return;
                            }
                            List<ThreadAdvanced<Forum<String>, String>> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                                List<ThreadAdvanced<Forum<String>, String>> threads = new ArrayList<>();
                                while (resultSet.next()) {
                                    ThreadAdvanced<Forum<String>, String> thread = new ThreadAdvanced<>(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                            resultSet.getInt(6), resultSet.getInt(7), resultSet.getInt(8), resultSet.getInt(9),
                                            resultSet.getBoolean(10), resultSet.getBoolean(11), resultSet.getString(12), resultSet.getString(13));
                                    thread.setUser(resultSet.getString(5));
                                    thread.setForum(new Forum<>(resultSet.getInt(14), resultSet.getString(15),
                                            resultSet.getString(16), resultSet.getString(17)));
                                    threads.add(thread);
                                }
                                return threads;
                            });
//                            if (!threadList.isEmpty()) {
                                Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
//                            } else {
//                                Common.addNotFound(response);
//                            }
                        } else {
                            Common.addNotValid(response);
                        }
                    }
                } else {
                    if (optionalParams.length == 2 &&
                            ((optionalParams[0].equals("user") && optionalParams[1].equals("forum")) ||
                                    (optionalParams[1].equals("user") && optionalParams[0].equals("forum")))) {
                        query = String.format("SELECT * FROM forum f INNER JOIN (user u INNER JOIN thread t ON t.user=u.email) ON t.forum=f.short_name WHERE t.forum=\"%s\"", forum);
                        query = addOptionalParams(request, response, "t.date", query);
                        if (query == null) {
                            Common.addNotValid(response);
                            return;
                        }
                        List<ThreadAdvanced<Forum<String>, UserFull>> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                            List<ThreadAdvanced<Forum<String>, UserFull>> threads = new ArrayList<>();
                            while (resultSet.next()) {
                                ThreadAdvanced<Forum<String>, UserFull> data = new ThreadAdvanced<>(resultSet.getInt(11), resultSet.getString(12), resultSet.getString(13),
                                        resultSet.getInt(16), resultSet.getInt(17), resultSet.getInt(18), resultSet.getInt(19),
                                        resultSet.getBoolean(20), resultSet.getBoolean(21), resultSet.getString(22), resultSet.getString(23));
                                data.setForum(new Forum<>(resultSet.getInt(1), resultSet.getString(2),
                                        resultSet.getString(3), resultSet.getString(4)));
                                data.setUser(new UserFull(resultSet.getInt(5), resultSet.getString(6),
                                        resultSet.getString(7), resultSet.getString(8),
                                        resultSet.getString(9), resultSet.getBoolean(10)));
                                threads.add(data);
                            }
                            return threads;
                        });
//                        if (!threadList.isEmpty()) {
                            for (ThreadAdvanced<Forum<String>, UserFull> thread : threadList) {
                                UserDAOImpl.addAdvancedLists(connection, thread.getUser());
                            }
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
//                        } else {
//                            Common.addNotFound(response);
//                        }
                        return;
                    }
                    Common.addNotValid(response);
                }
            } else {
                query = String.format("SELECT * FROM thread WHERE forum=\"%s\"", forum);
                query = addOptionalParams(request, response, "date", query);
                if (query == null) {
                    Common.addNotValid(response);
                    return;
                }
                List<ThreadFull> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                    List<ThreadFull> threads = new ArrayList<>();
                    while (resultSet.next()) {
                    threads.add(new ThreadFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6), resultSet.getInt(7),
                            resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                            resultSet.getString(12), resultSet.getString(13)));
                    }
                    return threads;
                });
//                if (!threadList.isEmpty()) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
//                } else {
//                    Common.addNotFound(response);
//                }
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
        String query = String.format("SELECT DISTINCT u.id, u.email, u.username, u.name, u.about, u.isAnonymous " +
                "FROM post p INNER JOIN user u ON p.user=u.email WHERE p.forum=\"%s\"", forumShortName);
        query = addOptionalUsersParams(request, response, query);
        if (query == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        try {
            List<UserFull> userList = TExecutor.execQuery(connection, query, (resultSet) -> {
                List<UserFull> data = new ArrayList<>();
                while (resultSet.next()) {
                    data.add(new UserFull(resultSet.getInt(1), resultSet.getString(2),
                            resultSet.getString(3), resultSet.getString(4),
                            resultSet.getString(5), resultSet.getBoolean(6)));
                }
                return data;
            });
//            if (!userList.isEmpty()) {
                for (UserFull user : userList) {
                    UserDAOImpl.addAdvancedLists(connection, user);
                }
                Common.addToResponse(response, new BaseResponse<>((byte) 0, userList));
//            } else {
//                Common.addNotFound(response);
//            }
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
}
