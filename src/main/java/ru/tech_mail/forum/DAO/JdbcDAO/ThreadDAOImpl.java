package ru.tech_mail.forum.DAO.JdbcDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tech_mail.forum.DAO.ThreadDAO;
import ru.tech_mail.forum.exceptions.DuplicateDataException;
import ru.tech_mail.forum.exceptions.WrongDataException;
import ru.tech_mail.forum.responses.*;
import ru.tech_mail.forum.responses.Thread;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ThreadDAOImpl implements ThreadDAO {
    private static final Logger LOG = LoggerFactory.getLogger(ClearDAOImpl.class);
    private final ConnectionPool connectionPool;

    public ThreadDAOImpl(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public void close(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("thread")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("UPDATE Thread SET isClosed=true WHERE id=%d",
                        (Integer) params.get("thread"));
                TExecutor.execUpdate(connection, query);
                Common.addStringToResponse(response,
                        String.format("{\"code\": 0, \"response\": {\"thread\": %d}}", (Integer) params.get("thread")));
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
    public void create(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("forum") && params.containsKey("title") && params.containsKey("isClosed") &&
                params.containsKey("user") && params.containsKey("date") &&params.containsKey("message") &&
                params.containsKey("slug")){
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("INSERT INTO Thread(forum, title, isClosed, user, date, message, slug, isDeleted) " +
                                    "VALUES(\"%s\", \"%s\", %b, \"%s\", \"%s\", \"%s\", \"%s\", %b)",
                        Common.escapeInjections((String)params.get("forum")),
                        Common.escapeInjections((String)params.get("title")),
                        params.get("isClosed"),
                        Common.escapeInjections((String)params.get("user")),
                        Common.escapeInjections((String)params.get("date")),
                        Common.escapeInjections((String)params.get("message")),
                        Common.escapeInjections((String)params.get("slug")),
                        params.containsKey("isDeleted") ? (Boolean)params.get("isDeleted") : false);
                Integer threadId;
                if ((threadId = TExecutor.execUpdateGetId(connection, query)) != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0,
                            new Thread<>(threadId, (String) params.get("title"), (String) params.get("slug"),
                                    (String) params.get("forum"), (String) params.get("user"),
                                    (Boolean) params.get("isClosed"),
                                    params.containsKey("isDeleted") ? (Boolean)params.get("isDeleted") : false,
                                    (String)params.get("date"), (String) params.get("message"))));

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

    public static ThreadFull getThreadDetails(Connection connection, int id) throws WrongDataException {
        return TExecutor.execQuery(connection,
                String.format("SELECT * FROM Thread t WHERE t.id=%d", id),
                (resultSet) -> resultSet.next() ? new ThreadFull<>(resultSet, resultSet.getString("t.forum"), resultSet.getString("t.user")) : null);
    }

    @Override
    public void details(HttpServletRequest request, HttpServletResponse response) {
        String threadId = Common.escapeInjections(request.getParameter("thread"));
        if (threadId == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        String query = String.format("SELECT * FROM Thread t WHERE t.id=%d", Integer.valueOf(threadId));
        String[] optionalParams = request.getParameterValues("related");
        try {
            if (optionalParams != null && optionalParams.length > 0) {
                if (optionalParams.length == 1) {
                     if (optionalParams[0].equals("user")) {
                         ThreadFull<String, UserFull> threadAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                             if (resultSet.next()) {
                                 return new ThreadFull<>(resultSet, resultSet.getString("t.forum"), new UserFull(resultSet.getString("t.user")));
                             } else {
                                 return null;
                             }
                         });
                         if (threadAdv != null) {
                             threadAdv.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull) threadAdv.getUser()).getEmail()));
                             Common.addToResponse(response, new BaseResponse<>((byte) 0, threadAdv));
                         } else {
                             Common.addNotFound(response);
                         }
                     } else {
                         if (optionalParams[0].equals("forum")) {
                             ThreadFull<Forum, String> threadAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                                 if (resultSet.next()) {
                                     return new ThreadFull<>(resultSet, new Forum<>(resultSet.getString("t.forum")), resultSet.getString("t.user"));
                                 } else {
                                     return null;
                                 }
                             });
                             if (threadAdv != null) {
                                 threadAdv.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum) threadAdv.getForum()).getShort_name()));
                                 Common.addToResponse(response, new BaseResponse<>((byte) 0, threadAdv));
                             } else {
                                 Common.addNotFound(response);
                             }
                         } else {
                             Common.addNotCorrect(response);
                         }
                     }
                } else {
                    if (optionalParams.length == 2 &&
                            ((optionalParams[0].equals("user") && optionalParams[1].equals("forum")) ||
                                    (optionalParams[1].equals("user") && optionalParams[0].equals("forum")))) {
                        ThreadFull<Forum, UserFull> threadAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                return new ThreadFull<>(resultSet, new Forum(resultSet.getString("t.forum")),
                                        new UserFull(resultSet.getString("t.user")));
                            } else {
                                return null;
                            }
                        });
                        if (threadAdv != null) {
                            threadAdv.setUser(UserDAOImpl.getUserDetails(connection, ((UserFull) threadAdv.getUser()).getEmail()));
                            threadAdv.setForum(ForumDAOImpl.getForumDetails(connection, ((Forum) threadAdv.getForum()).getShort_name()));
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, threadAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    Common.addNotCorrect(response);
                }
            } else {
                ThreadFull thread = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new ThreadFull<>(resultSet, resultSet.getString("t.forum"), resultSet.getString("t.user")) : null);
                if (thread != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, thread));
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
        String userEmail = Common.escapeInjections(request.getParameter("user"));
        String forumShortName = Common.escapeInjections(request.getParameter("forum"));
        String query;
        if (userEmail != null) {
            query = String.format("SELECT * FROM Thread t WHERE t.user = \"%s\"", userEmail);
        } else {
            if (forumShortName != null) {
                query = String.format("SELECT * FROM Thread t WHERE t.forum = \"%s\"", forumShortName);
            } else {
                Common.addNotValid(response);
                return;
            }
        }
        if ((query = addOptionalParams(request, response, "t.date", query)) == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        try {
            List<ThreadFull> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                List<ThreadFull> data = new ArrayList<>();
                while (resultSet.next()) {
                    data.add(new ThreadFull<>(resultSet, resultSet.getString("t.forum"), resultSet.getString("t.user")));
                }
                return data;
            });
            Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
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
                case "asc" :
                    query += " ORDER BY " + dateField + " ASC";
                    break;
                case "desc" :
                    query += " ORDER BY " + dateField + " DESC";
                    break;
                default :
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
    public void listPosts(HttpServletRequest request, HttpServletResponse response) {
        Integer threadId = request.getParameter("thread") == null ? null :
                Integer.valueOf(request.getParameter("thread"));
        String query;
        if (threadId != null) {
            query = String.format("SELECT * FROM Post p WHERE p.thread = %d", threadId);
        } else {
            Common.addNotValid(response);
            return;
        }
        if ((query = addOptionalParams(request, response, "p.date", query)) == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        try {
            List<PostFull> postList = TExecutor.execQuery(connection, query, (resultSet) -> {
                List<PostFull> data = new ArrayList<>();
                while (resultSet.next()) {
                    data.add(new PostFull<>(resultSet,
                            resultSet.getString("p.forum"), resultSet.getString("p.user"), resultSet.getInt("p.thread")));
                }
                return data;
            });
            Common.addToResponse(response, new BaseResponse<>((byte) 0, postList));
        } catch (WrongDataException e) {
            LOG.error("Can't get list of posts by thread!", e);
            Common.addNotCorrect(response);
        } finally {
            connectionPool.returnConnection(connection);
        }
    }

    @Override
    public void open(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("thread")){
            Connection connection = connectionPool.getConnection();
            try {
                TExecutor.execUpdate(connection,
                        String.format("UPDATE Thread SET isClosed=false WHERE id=%d", (Integer)params.get("thread")));

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
            Common.addStringToResponse(response,
                    String.format("{\"code\": 0, \"response\": {\"thread\": %d}}", (Integer)params.get("thread")));
        } else {
            Common.addNotCorrect(response);
        }
    }

    @Override
    public void remove(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("thread")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("UPDATE Thread SET isDeleted=true WHERE id=%d",
                        (Integer) params.get("thread"));
                TExecutor.execUpdate(connection, query);
                query = String.format("UPDATE Post SET isDeleted=true WHERE thread=%d",
                        (Integer) params.get("thread"));
                TExecutor.execUpdate(connection, query);
                Common.addStringToResponse(response,
                        String.format("{\"code\": 0, \"response\": {\"thread\": %d}}", (Integer) params.get("thread")));
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
        if (params.containsKey("thread")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("UPDATE Thread SET isDeleted=false WHERE id=%d",
                        (Integer) params.get("thread"));
                TExecutor.execUpdate(connection, query);
                query = String.format("UPDATE Post SET isDeleted=false WHERE thread=%d",
                        (Integer) params.get("thread"));
                TExecutor.execUpdate(connection, query);
                Common.addStringToResponse(response,
                        String.format("{\"code\": 0, \"response\": {\"thread\": %d}}", (Integer) params.get("thread")));
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
    public void subscribe(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("thread") && params.containsKey("user")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("INSERT INTO Subscribe(user, thread) VALUES(\"%s\", %d)",
                        Common.escapeInjections((String)params.get("user")),
                        (Integer)params.get("thread"));
                TExecutor.execUpdate(connection, query);
                Common.addStringToResponse(response,
                        String.format("{\"code\": 0, \"response\": {\"thread\": %d, \"user\":\"%s\"}}",
                                (Integer) params.get("thread"), params.get("user")));
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
    public void unsubscribe(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> params = Common.readRequest(request);
        if (params == null || params.isEmpty()) {
            Common.addNotValid(response);
            return;
        }
        if (params.containsKey("thread") && params.containsKey("user")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("DELETE FROM Subscribe WHERE user=\"%s\" AND thread=%d",
                        Common.escapeInjections((String)params.get("user")),
                        (Integer)params.get("thread"));
                TExecutor.execUpdate(connection, query);
                Common.addStringToResponse(response,
                        String.format("{\"code\": 0, \"response\": {\"thread\": %d, \"user\":\"%s\"}}",
                                (Integer) params.get("thread"), params.get("user")));
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
        if (params.containsKey("thread") && params.containsKey("message") && params.containsKey("slug")) {
            Connection connection = connectionPool.getConnection();
            try {
                String query = String.format("UPDATE Thread SET message=\"%s\", slug=\"%s\" WHERE id=%d",
                        Common.escapeInjections((String)params.get("message")),
                        Common.escapeInjections((String)params.get("slug")),
                        (Integer)params.get("thread"));
                TExecutor.execUpdate(connection, query);
                query = String.format("SELECT * FROM Thread t WHERE t.id=%d",
                        (Integer) params.get("thread"));
                ThreadFull thread = TExecutor.execQuery(connection, query,
                        (resultSet) -> resultSet.next() ?
                                new ThreadFull<>(resultSet, resultSet.getString("t.forum"), resultSet.getString("t.user")) : null);
                if (thread != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, thread));
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
        if (params.containsKey("thread") && params.containsKey("vote")) {
            Connection connection = connectionPool.getConnection();
            try {
                Integer vote = (Integer)params.get("vote");
                String query;
                if (vote == 1) {
                    query = String.format("UPDATE Thread SET likes=likes + 1, points=points + 1 WHERE id=%d",
                            (Integer)params.get("thread"));
                } else {
                    if (vote == -1) {
                        query = String.format("UPDATE Thread SET dislikes=dislikes + 1, points=points - 1 WHERE id=%d",
                                (Integer)params.get("thread"));
                    } else {
                        Common.addNotCorrect(response);
                        return;
                    }
                }
                TExecutor.execUpdate(connection, query);
                query = String.format("SELECT * FROM Thread t WHERE t.id=%d",
                        (Integer) params.get("thread"));
                ThreadFull thread = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new ThreadFull<>(resultSet, resultSet.getString("t.forum"), resultSet.getString("t.user")) : null);
                if (thread != null) {
                    Common.addToResponse(response, new BaseResponse<>((byte) 0, thread));
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
