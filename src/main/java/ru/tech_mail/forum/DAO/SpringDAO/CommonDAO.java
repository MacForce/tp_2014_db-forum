//package ru.tech_mail.forum.DAO.SpringDAO;
//
//import org.codehaus.jackson.map.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.DataAccessException;
//import org.springframework.jdbc.core.JdbcTemplate;
//import ru.tech_mail.forum.exceptions.JDBCException;
//import ru.tech_mail.forum.responses.BaseResponse;
//
//import javax.annotation.Resource;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.Map;
//
//public class CommonDAO {
//    @Autowired
//    private ForumDAO forumDAO;
//    @Autowired
//    private PostDAO postDAO;
//    @Autowired
//    private ThreadDAO threadDAO;
//    @Autowired
//    private UserDAO userDAO;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//    @Resource(name = "common")
//    private Map<String, String> commonQueries;
//
//    private static final Logger LOG = LoggerFactory.getLogger(CommonDAO.class);
//    private final ObjectMapper JSONMapper = new ObjectMapper();
//
//    public void clear(HttpServletRequest request, HttpServletResponse response) {
//        try {
//            jdbcTemplate.update(commonQueries.get("clear"), "user");
//            jdbcTemplate.update(commonQueries.get("clear"), "post");
//            jdbcTemplate.update(commonQueries.get("clear"), "forum");
//            jdbcTemplate.update(commonQueries.get("clear"), "thread");
//            jdbcTemplate.update(commonQueries.get("clear"), "friendship");
//            jdbcTemplate.update(commonQueries.get("clear"), "user_thread");
//        } catch (DataAccessException e) {
//            e.printStackTrace();
//            LOG.error("Can't execute clear sql!", e);
//            return;
//        }
//        setResponse(response, new BaseResponse((byte) 0, "OK"));
//    }
//
//    public void createUser(HttpServletRequest request, HttpServletResponse response) {
//        Map<String, Object> userData = null;
//        try {
//            userData = JSONMapper.readValue(request.getReader(), Map.class);
//        } catch (IOException e) {
//            LOG.error("Can't parse request: " + request.getRequestURI(), e);
//            setResponse(response, new BaseResponse((byte) 2, "ERROR"));
//            return;
//        }
//        if (userData.containsKey("isAnonymous") && ((Boolean) userData.get("isAnonymous"))) {
//            try {
//                if (!userDAO.createAnonym(userData)) {
//                    setResponse(response, new BaseResponse((byte) 5, "ERROR"));
//                    return;
//                }
//            } catch (JDBCException e) {
//                e.printStackTrace();
//                return;
//            } catch (Exception e) {
//                e.printStackTrace();
//                return;
//            }
//        } else {
//            try {
//                if (!userDAO.create(userData)) {
//                    setResponse(response, new BaseResponse((byte) 5, "ERROR"));
//                    return;
//                }
//            } catch (JDBCException e) {
//                e.printStackTrace();
//                return;
//            } catch (Exception e) {
//                e.printStackTrace();
//                return;
//            }
//        }
//        try {
//            setResponse(response, new BaseResponse((byte) 0, userDAO.details((String) userData.get("email"))));
//        } catch (JDBCException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void updateUser(HttpServletRequest request, HttpServletResponse response) {
//        Map<String, Object> userData = null;
//        try {
//            userData = JSONMapper.readValue(request.getReader(), Map.class);
//        } catch (IOException e) {
//            LOG.error("Can't parse request: " + request.getRequestURI(), e);
//            setResponse(response, new BaseResponse((byte) 2, "ERROR"));
//            return;
//        }
//
//        try {
//            if (!userDAO.update(userData)) {
//                setResponse(response, new BaseResponse((byte) 1, "ERROR"));
//                return;
//            }
//        } catch (JDBCException e) {
//            e.printStackTrace();
//            return;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return;
//        }
//        try {
//            setResponse(response, new BaseResponse((byte) 0, userDAO.details((String) userData.get("email"))));
//        } catch (JDBCException e) {
//            e.printStackTrace();
//        }
//    }
//}
