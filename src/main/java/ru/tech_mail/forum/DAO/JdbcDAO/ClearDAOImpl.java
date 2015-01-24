package ru.tech_mail.forum.DAO.JdbcDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tech_mail.forum.DAO.ClearDAO;
import ru.tech_mail.forum.exceptions.DuplicateDataException;
import ru.tech_mail.forum.exceptions.WrongDataException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;

public class ClearDAOImpl implements ClearDAO {
    private static final Logger LOG = LoggerFactory.getLogger(ClearDAOImpl.class);
    private final ConnectionPool connectionPool;

    public ClearDAOImpl(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        Connection connection = connectionPool.getConnection();
        try {
            String query = "TRUNCATE TABLE %s";
            TExecutor.execUpdate(connection, String.format(query, "forum"));
            TExecutor.execUpdate(connection, String.format(query, "friendship"));
            TExecutor.execUpdate(connection, String.format(query, "post"));
            TExecutor.execUpdate(connection, String.format(query, "subscribe"));
            TExecutor.execUpdate(connection, String.format(query, "thread"));
            TExecutor.execUpdate(connection, String.format(query, "user"));
        } catch (WrongDataException e) {
            LOG.error("Can't clear tables!");
            Common.addError(response);
            return;
        } catch (DuplicateDataException e) {
            Common.addError(response);
            return;
        } finally {
            connectionPool.returnConnection(connection);
        }
        Common.addOK(response);
    }
}
