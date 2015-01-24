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
import java.sql.Date;
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
                            new Thread(threadId, (String) params.get("title"), (String) params.get("slug"),
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

    @Override
    public void details(HttpServletRequest request, HttpServletResponse response) {
        String threadId = Common.escapeInjections(request.getParameter("thread"));
        if (threadId == null) {
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
                         query = String.format("SELECT * FROM Thread t INNER JOIN User u ON t.user=u.email WHERE t.id=%d", Integer.valueOf(threadId));
                         ThreadAdvanced<String, UserFull> threadAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                             if (resultSet.next()) {
                                 ThreadAdvanced<String, UserFull> data = new ThreadAdvanced<>(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                         resultSet.getInt(6), resultSet.getInt(7), resultSet.getInt(8), resultSet.getInt(9),
                                         resultSet.getBoolean(10), resultSet.getBoolean(11), resultSet.getString(12), resultSet.getString(13));
                                 data.setForum(resultSet.getString(4));
                                 data.setUser(new UserFull(resultSet.getInt(14), resultSet.getString(15),
                                         resultSet.getString(16), resultSet.getString(17),
                                         resultSet.getString(18), resultSet.getBoolean(19)));
                                 return data;
                             } else {
                                 return null;
                             }
                         });
                         if (threadAdv != null) {
                             UserDAOImpl.addAdvancedLists(connection, threadAdv.getUser());
                             Common.addToResponse(response, new BaseResponse<>((byte) 0, threadAdv));
                         } else {
                             Common.addNotFound(response);
                         }
                     } else {
                         if (optionalParams[0].equals("forum")) {
                             query = String.format("SELECT * FROM Thread t INNER JOIN Forum f ON t.forum=f.short_name WHERE t.id=%d", Integer.valueOf(threadId));
                             ThreadAdvanced<Forum, String> threadAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                                 if (resultSet.next()) {
                                     ThreadAdvanced<Forum, String> data = new ThreadAdvanced<>(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                             resultSet.getInt(6), resultSet.getInt(7), resultSet.getInt(8), resultSet.getInt(9),
                                             resultSet.getBoolean(10), resultSet.getBoolean(11), resultSet.getString(12), resultSet.getString(13));
                                     data.setUser(resultSet.getString(5));
                                     data.setForum(new Forum<>(resultSet.getInt(14), resultSet.getString(15),
                                             resultSet.getString(16), resultSet.getString(17)));
                                     return data;
                                 } else {
                                     return null;
                                 }
                             });
                             if (threadAdv != null) {
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
                        query = String.format("SELECT * FROM Forum f INNER JOIN (User u INNER JOIN Thread t ON t.user=u.email) ON t.forum=f.short_name WHERE t.id=%d", Integer.valueOf(threadId));
                        ThreadAdvanced<Forum, UserFull> threadAdv = TExecutor.execQuery(connection, query, (resultSet) -> {
                            if (resultSet.next()) {
                                ThreadAdvanced<Forum, UserFull> data = new ThreadAdvanced<>(resultSet.getInt(11), resultSet.getString(12), resultSet.getString(13),
                                        resultSet.getInt(16), resultSet.getInt(17), resultSet.getInt(18), resultSet.getInt(19),
                                        resultSet.getBoolean(20), resultSet.getBoolean(21), resultSet.getString(22), resultSet.getString(23));
                                data.setForum(new Forum<>(resultSet.getInt(1), resultSet.getString(2),
                                        resultSet.getString(3), resultSet.getString(4)));
                                data.setUser(new UserFull(resultSet.getInt(5), resultSet.getString(6),
                                        resultSet.getString(7), resultSet.getString(8),
                                        resultSet.getString(9), resultSet.getBoolean(10)));
                                return data;
                            } else {
                                return null;
                            }
                        });
                        if (threadAdv != null) {
                            UserDAOImpl.addAdvancedLists(connection, threadAdv.getUser());
                            Common.addToResponse(response, new BaseResponse<>((byte) 0, threadAdv));
                        } else {
                            Common.addNotFound(response);
                        }
                        return;
                    }
                    Common.addNotCorrect(response);
                }
            } else {
                query = String.format("SELECT * FROM thread WHERE id=%d", Integer.valueOf(threadId));
                ThreadFull thread = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new ThreadFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6), resultSet.getInt(7),
                                resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                                resultSet.getString(12), resultSet.getString(13))
                        : null);
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
            query = String.format("SELECT * FROM Thread WHERE user = \"%s\"", userEmail);
        } else {
            if (forumShortName != null) {
                query = String.format("SELECT * FROM Thread WHERE forum = \"%s\"", forumShortName);
            } else {
                Common.addNotValid(response);
                return;
            }
        }
        query = addOptionalParams(request, response, query);
        if (query == null) {
            Common.addNotValid(response);
            return;
        }
        Connection connection = connectionPool.getConnection();
        try {
            List<ThreadFull> threadList = TExecutor.execQuery(connection, query, (resultSet) -> {
                List<ThreadFull> data = new ArrayList<>();
                while (resultSet.next()) {
                    data.add(new ThreadFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6), resultSet.getInt(7),
                            resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                            resultSet.getString(12), resultSet.getString(13)));
                }
                return data;
            });
//            if (!threadList.isEmpty()) {
                Common.addToResponse(response, new BaseResponse<>((byte) 0, threadList));
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

    private String addOptionalParams(HttpServletRequest request, HttpServletResponse response, String query) {
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
    public void listPosts(HttpServletRequest request, HttpServletResponse response) {
        Integer threadId = request.getParameter("thread") == null ? null :
                Integer.valueOf(request.getParameter("thread"));
        String query;
        if (threadId != null) {
            query = String.format("SELECT * FROM Post WHERE thread = %d", threadId);
        } else {
            Common.addNotValid(response);
            return;
        }
        query = addOptionalParams(request, response, query);
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
                        String.format("UPDATE Thread SET isClosed = false WHERE id = %d ", (Integer)params.get("thread")));

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
                query = String.format("SELECT * FROM Thread WHERE id=%d",
                        (Integer) params.get("thread"));
                ThreadFull thread = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                                new ThreadFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                        resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6), resultSet.getInt(7),
                                        resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                                        resultSet.getString(12), resultSet.getString(13))
                                : null);
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
                query = String.format("SELECT * FROM Thread WHERE id=%d",
                        (Integer) params.get("thread"));
                ThreadFull thread = TExecutor.execQuery(connection, query, (resultSet) -> resultSet.next() ?
                        new ThreadFull(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                                resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6), resultSet.getInt(7),
                                resultSet.getInt(8), resultSet.getInt(9), resultSet.getBoolean(10), resultSet.getBoolean(11),
                                resultSet.getString(12), resultSet.getString(13))
                        : null);
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
