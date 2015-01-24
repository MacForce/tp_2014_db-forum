//package ru.tech_mail.forum.DAO.SpringDAO;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.DataAccessException;
//import org.springframework.jdbc.core.JdbcTemplate;
//import ru.tech_mail.forum.exceptions.JDBCException;
//import ru.tech_mail.forum.responses.User;
//
//import javax.annotation.Resource;
//import java.sql.ResultSet;
//import java.util.List;
//import java.util.Map;
//
//public class ForumDAO {
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//    @Resource(name = "forumQueries")
//    private Map<String, String> queries;
//
//    private static final Logger LOG = LoggerFactory.getLogger(ForumDAO.class);
//
//    public boolean create(Map<String, Object> forum) throws JDBCException {
//        try {
//            return jdbcTemplate.update(queries.get("create"),
//                    (String) forum.get("name"),
//                    (String) forum.get("short_name"),
//                    (String) forum.get("user"),
//                    (String) forum.get("email")) > 0 ? true : false;
//        } catch (DataAccessException e) {
//            LOG.error("DataAccessException at create user!", e);
//            throw new JDBCException();
//        }
//    }
//
//    public boolean createAnonym(Map<String, Object> user) throws JDBCException {
//        try {
//            return jdbcTemplate.update(queries.get("createAnonym"),
//                    true,
//                    (String) user.get("email")) > 0 ? true : false;
//        } catch (DataAccessException e) {
//            LOG.error("DataAccessException at create anonym user!", e);
//            throw new JDBCException();
//        }
//    }
//
//    public User details(String email) throws JDBCException {
//        List<User> users;
//        try {
//            users = jdbcTemplate.query(queries.get("details"), new Object[]{email},
//                    (ResultSet rs, int rowNum) -> new User(rs.getInt(1),
//                            rs.getString(2),
//                            rs.getString(3),
//                            rs.getString(4),
//                            rs.getBoolean(5),
//                            rs.getString(6))
//            );
//        } catch (DataAccessException e) {
//            LOG.error("DataAccessException at getUsers!", e);
//            throw new JDBCException();
//        }
//        if (users == null || users.isEmpty()) {
//            return null;
//        }
//        return users.get(0);
//    }
//
//    public boolean update(Map<String, Object> user) throws JDBCException {
//        try {
//            return jdbcTemplate.update(queries.get("updateProfile"),
//                    (String) user.get("about"),
//                    (String) user.get("name"),
//                    (String) user.get("email")) > 0 ? true : false;
//        } catch (DataAccessException e) {
//            LOG.error("DataAccessException at create user!", e);
//            throw new JDBCException();
//        }
//    }
//}
