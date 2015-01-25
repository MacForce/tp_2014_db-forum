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
                String query = String.format("INSERT INTO Post(date, thread, message, user, forum, " +
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
//                    if (!params.containsKey("isDeleted")) {
//                        TExecutor.execUpdate(connection, String.format("UPDATE Thread SET posts=posts+1 WHERE id=%d", (Integer) params.get("thread")));
//                    } else {
//                        if (!(Boolean)params.get("isDeleted")) {
//                            TExecutor.execUpdate(connection, String.format("UPDATE Thread SET posts=posts+1 WHERE id=%d", (Integer) params.get("thread")));
//                        }
//                    }
                    Common.addToResponse(response, new BaseResponse<>((byte) 0,
                            new Post<>(postId, (String)params.get("message"), (String)params.get("forum"), (String)params.get("user"),
                                    params.containsKey("parent") ? (Integer)params.get("parent") : null,
                                    (Integer)params.get("thread"),
                                    params.containsKey("isApproved") ? (Boolean)params.get("isApproved") : false,
                                    params.containsKey("isHighlighted") ? (Boolean)params.get("isHighlighted") : false,
                                    params.containsKey("isEdited") ? (Boolean)params.get("isEdited") : false,
                                    params.containsKey("isSpam") ? (Boolean)params.get("isSpam") : false,
                                    params.containsKey("isDeleted") ? (Boolean)params.get("isDeleted") : false,
                                    (String)params.get("date"))));
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
        String query = String.format("SELECT * FROM Post p WHERE p.id=%d", postId);
        String[] optionalParams = request.getParameterValues("related");
        try {
            if (optionalParams != null) {
                if (optionalParams.length == 1) {
                    if (optionalParams[0].equals("user")) {
                        PostFull<String, Integer, UserFull> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                return new PostFull<>(resultSet, resultSet.getString("p.forum"),
                                        new UserFull(resultSet.getString("p.user")), resultSet.getInt("p.thread"));
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            postAdv.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull) postAdv.getUser()).getEmail()));
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (optionalParams[0].equals("forum")) {
                        PostFull<Forum<String>, Integer, String> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                return new PostFull<>(resultSet, new Forum<>(resultSet.getString("p.forum")),
                                        resultSet.getString("p.user"), resultSet.getInt("p.thread"));
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            postAdv.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum) postAdv.getForum()).getShort_name()));
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (optionalParams[0].equals("thread")) {
                        PostFull<String, ThreadFull<String, String>, String> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                return new PostFull<>(resultSet, resultSet.getString("p.forum"),
                                        resultSet.getString("p.user"), new ThreadFull<>(resultSet.getInt("p.thread")));
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            postAdv.setThread(ThreadDAOImpl.getThreadDetails(connection, ((ThreadFull) postAdv.getThread()).getId()));
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
                        PostFull<Forum<String>, Integer, UserFull> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                return new PostFull<>(resultSet, new Forum<>(resultSet.getString("p.forum")),
                                        new UserFull(resultSet.getString("p.user")), resultSet.getInt("p.thread"));
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            postAdv.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum) postAdv.getForum()).getShort_name()));
                            postAdv.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull) postAdv.getUser()).getEmail()));
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (relatedParams.contains("user") && relatedParams.contains("thread")) {
                        PostFull<String, ThreadFull, UserFull> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                return new PostFull<>(resultSet, resultSet.getString("p.forum"),
                                        new UserFull(resultSet.getString("p.user")), new ThreadFull<>(resultSet.getInt("p.thread")));
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            postAdv.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull) postAdv.getUser()).getEmail()));
                            postAdv.setThread(ThreadDAOImpl.getThreadDetails(connection, ((ThreadFull) postAdv.getThread()).getId()));
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    if (relatedParams.contains("forum") && relatedParams.contains("thread")) {
                        PostFull<Forum<String>, ThreadFull, String> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                return new PostFull<>(resultSet, new Forum<>(resultSet.getString("p.forum")),
                                        resultSet.getString("p.user"), new ThreadFull<>(resultSet.getInt("p.thread")));
                            } else {
                                return null;
                            }
                        });
                        if (postAdv != null) {
                            postAdv.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum) postAdv.getForum()).getShort_name()));
                            postAdv.setThread(ThreadDAOImpl.getThreadDetails(connection, ((ThreadFull) postAdv.getThread()).getId()));
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
                    PostFull<Forum<String>, ThreadFull, UserFull> postAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                        if (resultSet.next()) {
                            return new PostFull<>(resultSet, new Forum<>(resultSet.getString("p.forum")),
                                    new UserFull(resultSet.getString("p.user")), new ThreadFull<>(resultSet.getInt("p.thread")));
                        } else {
                            return null;
                        }
                    });
                    if (postAdv != null) {
                        postAdv.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum) postAdv.getForum()).getShort_name()));
                        postAdv.setThread(ThreadDAOImpl.getThreadDetails(connection, ((ThreadFull) postAdv.getThread()).getId()));
                        postAdv.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull) postAdv.getUser()).getEmail()));
                        Common.addToResponse(response, new BaseResponse<>((byte) 0, postAdv));
                    } else {
                        Common.addNotFound(response);
                    }
                    return;
                }
                Common.addNotValid(response);
            } else {
                PostFull<String, Integer, String> post = TExecutor.execQuery(connection, query,
                        (resultSet) -> resultSet.next() ? new PostFull<>(resultSet, resultSet.getString("p.forum"),
                                resultSet.getString("p.user"), resultSet.getInt("p.thread")) : null);
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
            query = String.format("SELECT * FROM Post p WHERE p.forum=\"%s\"", forumShortName);
        } else {
            if (threadId != null) {
                query = String.format("SELECT * FROM Post p WHERE p.thread=%d", threadId);
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
                    data.add(new PostFull<>(resultSet, resultSet.getString("p.forum"), resultSet.getString("p.user"), resultSet.getInt("p.thread")));
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
                String query = String.format("UPDATE Post SET isDeleted=true WHERE id=%d",
                        (Integer) params.get("post"));
                TExecutor.execUpdate(connection, query);
                query = String.format("UPDATE Thread t SET t.posts=t.posts-1 WHERE t.id=(SELECT p.thread FROM Post p WHERE p.id=%d)",
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
                String query = String.format("UPDATE Post SET isDeleted=false WHERE id=%d",
                        (Integer) params.get("post"));
                TExecutor.execUpdate(connection, query);
                query = String.format("UPDATE Thread t SET t.posts=t.posts+1 WHERE t.id=(SELECT p.thread FROM Post p WHERE p.id=%d)",
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
                String query = String.format("UPDATE Post SET message=\"%s\" WHERE id=%d",
                        Common.escapeInjections((String)params.get("message")),
                        (Integer)params.get("post"));
                TExecutor.execUpdate(connection, query);
                query = String.format("SELECT * FROM Post p WHERE id=%d",
                        (Integer) params.get("post"));
                PostFull post = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new PostFull<>(resultSet, resultSet.getString("p.forum"), resultSet.getString("p.user"), resultSet.getInt("p.thread")) : null);
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
                    query = String.format("UPDATE Post SET likes=likes + 1, points=points + 1 WHERE id=%d",
                            (Integer)params.get("post"));
                } else {
                    if (vote == -1) {
                        query = String.format("UPDATE Post SET dislikes=dislikes + 1, points=points - 1 WHERE id=%d",
                                (Integer)params.get("post"));
                    } else {
                        Common.addNotCorrect(response);
                        return;
                    }
                }
                TExecutor.execUpdate(connection, query);
                query = String.format("SELECT * FROM Post p WHERE p.id=%d", (Integer) params.get("post"));
                PostFull post = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new PostFull<>(resultSet, resultSet.getString("p.forum"), resultSet.getString("p.user"), resultSet.getInt("p.thread")) : null);
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
