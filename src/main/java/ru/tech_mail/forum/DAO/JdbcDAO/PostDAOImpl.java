package ru.tech_mail.forum.DAO.JdbcDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tech_mail.forum.DAO.PostDAO;
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

public class PostDAOImpl implements PostDAO {
    private static final Logger LOG = LoggerFactory.getLogger(ClearDAOImpl.class);
    private final ConnectionPool connectionPool;

    public PostDAOImpl(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public void create(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("date") && params.containsKey("thread") && params.containsKey("message") && params.containsKey("user") && params.containsKey("forum")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("INSERT INTO post(date, thread, message, user, forum, " +
                                "parent, isApproved, isHighlighted, isEdited, isSpam, isDeleted) " +
                                "VALUES(\"%s\", %d, \"%s\", \"%s\", \"%s\", %d, %b, %b, %b, %b, %b)",
                        Common.escapeInjections((String) params.get("date")),
                        (Integer)params.get("thread"),
                        Common.escapeInjections((String) params.get("message")),
                        Common.escapeInjections((String) params.get("user")),
                        Common.escapeInjections((String) params.get("forum")),
                        params.containsKey("parent") ? (Integer)params.get("parent") : null,
                        params.containsKey("isApproved") ? (Boolean)params.get("isApproved") : false,
                        params.containsKey("isHighlighted") ? (Boolean)params.get("isHighlighted") : false,
                        params.containsKey("isEdited") ? (Boolean)params.get("isEdited") : false,
                        params.containsKey("isSpam") ? (Boolean)params.get("isSpam") : false,
                        params.containsKey("isDeleted") ? (Boolean)params.get("isDeleted") : false);
                Integer postId;
                if ((postId = TExecutor.execUpdateGetId(connection, query)) != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0,
                            new Post(postId, (String)params.get("message"), (String)params.get("forum"), (String)params.get("user"),
                                    params.containsKey("parent") ? (Integer)params.get("parent") : null,
                                    (Integer)params.get("thread"),
                                    params.containsKey("isApproved") ? (Boolean)params.get("isApproved") : false,
                                    params.containsKey("isHighlighted") ? (Boolean)params.get("isHighlighted") : false,
                                    params.containsKey("isEdited") ? (Boolean)params.get("isEdited") : false,
                                    params.containsKey("isSpam") ? (Boolean)params.get("isSpam") : false,
                                    params.containsKey("isDeleted") ? (Boolean)params.get("isDeleted") : false,
                                    (String)params.get("date"))));
                    if (!params.containsKey("isDeleted") ||
                            (params.containsKey("isDeleted") && !(Boolean)params.get("isDeleted"))) {
                        TExecutor.execUpdate(connection, String.format("UPDATE thread SET posts=posts+1 WHERE id=%d",
                                (Integer)params.get("thread")));
                    }
                }  else {
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
        Integer postId = request.getParameter("post") == null ? null :
                Integer.valueOf(request.getParameter("post"));
        if (postId == null) {
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
                        query = String.format("SELECT * FROM post p INNER JOIN user u ON p.user=u.email WHERE p.id=%d", postId);
                        PostAdvanced<String, Integer, UserFull> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
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
                                return data;
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            UserDAOImpl.addAdvancedLists(connection, postAdv.getUser());
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (optionalParams[0].equals("forum")) {
                        query = String.format("SELECT * FROM post p INNER JOIN forum f ON p.forum=f.short_name WHERE p.id=%d", postId);
                        PostAdvanced<Forum<String>, Integer, String> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                PostAdvanced<Forum<String>, Integer, String> data = new PostAdvanced<>(resultSet.getInt(1),
                                        resultSet.getString(2), resultSet.getInt(5), resultSet.getInt(7),
                                        resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10),
                                        resultSet.getBoolean(11), resultSet.getBoolean(12), resultSet.getBoolean(13),
                                        resultSet.getBoolean(14), resultSet.getString(15));
                                data.setForum(new Forum<>(resultSet.getInt(16), resultSet.getString(17),
                                        resultSet.getString(18), resultSet.getString(19)));
                                data.setThread(resultSet.getInt(6));
                                data.setUser(resultSet.getString(4));
                                return data;
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (optionalParams[0].equals("thread")) {
                        query = String.format("SELECT * FROM post p INNER JOIN thread t ON p.thread=t.id WHERE p.id=%d", postId);
                        PostAdvanced<String, ThreadFull, String> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
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
                                return data;
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    Common.addNotValid(response);
                    return;
                }
                List<String> relatedParams = new ArrayList<>(Arrays.asList(optionalParams));
                if (optionalParams.length == 2) {
                    if (relatedParams.contains("user") && relatedParams.contains("forum")) {
                        query = String.format("SELECT * FROM forum f INNER JOIN (post p INNER JOIN user u ON p.user=u.email) ON f.short_name=p.forum WHERE p.id=%d", postId);
                        PostAdvanced<Forum<String>, Integer, UserFull> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
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
                                return data;
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            UserDAOImpl.addAdvancedLists(connection, postAdv.getUser());
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (relatedParams.contains("user") && relatedParams.contains("thread")) {
                        query = String.format("SELECT * FROM thread t INNER JOIN (post p INNER JOIN user u ON p.user=u.email) ON t.id=p.thread WHERE p.id=%d", postId);
                        PostAdvanced<String, ThreadFull, UserFull> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
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
                                return data;
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            UserDAOImpl.addAdvancedLists(connection, postAdv.getUser());
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (relatedParams.contains("forum") && relatedParams.contains("thread")) {
                        query = String.format("SELECT * FROM forum f INNER JOIN (post p INNER JOIN thread t ON p.thread=t.id) ON f.short_name=p.forum WHERE p.id=%d", postId);
                        PostAdvanced<Forum<String>, ThreadFull, String> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
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
                                return data;
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    Common.addNotValid(response);
                    return;
                }
                if (optionalParams.length == 3 && relatedParams.contains("user") &&
                        relatedParams.contains("forum") && relatedParams.contains("thread")) {
                    query = String.format("SELECT * FROM forum f INNER JOIN ( thread t INNER JOIN " +
                            "(post p INNER JOIN user u ON p.user=u.email) ON p.thread=t.id) ON p.forum=f.short_name WHERE p.id=%d", postId);
                    PostAdvanced<Forum<String>, ThreadFull, UserFull> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                        if (resultSet.next()) {
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
                            return data;
                        } else {
                            return null;
                        }
                    });
                    if (postAdv != null) {
                        UserDAOImpl.addAdvancedLists(connection, postAdv.getUser());
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                    } else {
                        Common.addNotFound(response);
                    }
                    return;
                }
                Common.addNotValid(response);
            } else {
                query = String.format("SELECT * FROM post WHERE id=%d", postId);
                PostFull post = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                    new PostFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getInt(5), resultSet.getInt(6), resultSet.getInt(7),
                            resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                            resultSet.getBoolean(12), resultSet.getBoolean(13), resultSet.getBoolean(14), resultSet.getString(15))
                    : null);
                if (post != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, post));
                } else {
                    Common.addNotFound(response);
                }
            }
        } catch (WrongDataException e) {
            LOG.error("Can't get list of posts by user!", e);
            Common.addNotCorrect(response);
        } finally {
            connectionPool.returnConnection(connection);
        }
    }

    @Override
    public void list(HttpServletRequest request, HttpServletResponse response) {
        String forumShortName = Common.escapeInjections(request.getParameter("forum"));
        Integer threadId = request.getParameter("thread") == null ? null :
                Integer.valueOf(request.getParameter("thread"));
        String query;
        if (forumShortName != null) {
            query = String.format("SELECT * FROM post WHERE forum=\"%s\"", forumShortName);
        } else {
            if (threadId != null) {
                query = String.format("SELECT * FROM post WHERE thread=%d", threadId);
            } else {
                Common.addNotValid(response);
                return;
            }
        }
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
                    data.add(new PostFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getInt(5), resultSet.getInt(6), resultSet.getInt(7),
                            resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                            resultSet.getBoolean(12), resultSet.getBoolean(13), resultSet.getBoolean(14), resultSet.getString(15)));
                }
                return data;
            });
//            if (!postList.isEmpty()) {
                Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
//            } else {
//                Common.addNotFound(response);
//            }
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
                query += String.format(" AND date >= \"%s\"", sinceDate);
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
                    query += " ORDER BY date ASC";
                    break;
                case "desc" :
                    query += " ORDER BY date DESC";
                    break;
                default :
                    Common.addNotValid(response);
                    return null;
            }
        } else {
            query += " ORDER BY date DESC";
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
    public void remove(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("post")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("UPDATE post SET isDeleted=true WHERE id=%d",
                        (Integer) params.get("post"));
                TExecutor.execUpdate(connection, query);
                query = String.format("UPDATE thread t SET t.posts=t.posts-1 WHERE t.id=(SELECT p.thread FROM post p WHERE p.id=%d)",
                        (Integer) params.get("post"));
                TExecutor.execUpdate(connection, query);
                Common.addStringToResponse(response,
                        String.format("{\"code\": 0, \"response\": {\"post\": %d}}", (Integer)params.get("post")));
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
    public void restore(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("post")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("UPDATE post SET isDeleted=false WHERE id=%d",
                        (Integer) params.get("post"));
                TExecutor.execUpdate(connection, query);
                query = String.format("UPDATE thread t SET t.posts=t.posts+1 WHERE t.id=(SELECT p.thread FROM post p WHERE p.id=%d)",
                        (Integer) params.get("post"));
                TExecutor.execUpdate(connection, query);
                Common.addStringToResponse(response,
                        String.format("{\"code\": 0, \"response\": {\"post\": %d}}", (Integer)params.get("post")));
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
    public void update(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("post") && params.containsKey("message")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("UPDATE post SET message=\"%s\" WHERE id=%d",
                        Common.escapeInjections((String)params.get("message")),
                        (Integer)params.get("post"));
                TExecutor.execUpdate(connection, query);
                query = String.format("SELECT * FROM post WHERE id=%d",
                        (Integer) params.get("post"));
                PostFull post = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new PostFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                resultSet.getString(4), resultSet.getInt(5), resultSet.getInt(6), resultSet.getInt(7),
                                resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                                resultSet.getBoolean(12), resultSet.getBoolean(13), resultSet.getBoolean(14), resultSet.getString(15))
                        : null);
                if (post != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, post));
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
    public void vote(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("post") && params.containsKey("vote")) {
            Connection connection = connectionPool.getConnection();
            try {
                Integer vote = (Integer)params.get("vote");
                String query;
                if (vote == 1) {
                    query = String.format("UPDATE post SET likes=likes + 1, points=points + 1 WHERE id=%d",
                            (Integer)params.get("post"));
                } else {
                    if (vote == -1) {
                        query = String.format("UPDATE post SET dislikes=dislikes + 1, points=points - 1 WHERE id=%d",
                                (Integer)params.get("post"));
                    } else {
                        Common.addNotCorrect(response);
                        return;
                    }
                }
                TExecutor.execUpdate(connection, query);
                query = String.format("SELECT * FROM post WHERE id=%d", (Integer) params.get("post"));
                PostFull post = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new PostFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                resultSet.getString(4), resultSet.getInt(5), resultSet.getInt(6), resultSet.getInt(7),
                                resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                                resultSet.getBoolean(12), resultSet.getBoolean(13), resultSet.getBoolean(14), resultSet.getString(15))
                        : null);
                if (post != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, post));
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
