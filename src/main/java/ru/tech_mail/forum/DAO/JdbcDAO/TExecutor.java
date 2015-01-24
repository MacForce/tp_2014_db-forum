package ru.tech_mail.forum.DAO.JdbcDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tech_mail.forum.exceptions.DuplicateDataException;
import ru.tech_mail.forum.exceptions.WrongDataException;

import java.sql.*;

public class TExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(TExecutor.class);

    public static <T> T execQuery(Connection connection, String query, TResultHandler<T> handler) throws WrongDataException {
        Statement stmt = null;
        ResultSet result = null;
        T value = null;
        try {
            stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            result = stmt.executeQuery(query);
            value = handler.handle(result);
        } catch (SQLDataException | SQLSyntaxErrorException e) {
            LOG.error("Wrong query: " + query, e);
            throw new WrongDataException();
        } catch (SQLException e) {
            LOG.error("Can't execute query: " + query, e);
        } catch (Exception e) {
            LOG.error("Some exception by query: " + query, e);
        } finally {
            try {
                if (result != null) result.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    public static void execUpdate(Connection connection, String update) throws WrongDataException, DuplicateDataException {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(update);
        } catch (SQLDataException | SQLSyntaxErrorException e) {
            LOG.error("Wrong query: " + update, e);
            throw new WrongDataException();
        } catch (SQLException e) {
            LOG.error("Can't execute update: " + update, e);
            throw new DuplicateDataException();
        }  catch (Exception e) {
            LOG.error("Some exception by query: " + update, e);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static Integer execUpdateGetId(Connection connection, String update) throws WrongDataException, DuplicateDataException {
        Statement stmt = null;
        ResultSet result = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(update);
            result = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            if (result.next()) {
                return result.getInt(1);
            } else {
                return null;
            }
        } catch (SQLDataException | SQLSyntaxErrorException e) {
            LOG.error("Wrong query: " + update, e);
            throw new WrongDataException();
        } catch (SQLIntegrityConstraintViolationException e) {
            LOG.error("Can't execute update (duplicate data): " + update, e);
            throw new DuplicateDataException();
        } catch (SQLException e) {
            LOG.error("Can't execute update: " + update, e);
            return null;
        }  catch (Exception e) {
            LOG.error("Some exception by query: " + update, e);
            return null;
        } finally {
            try {
                if (result != null) result.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void execUpdateArray(Connection connection, String[] updates) throws WrongDataException {
        Statement stmt;
        try {
            connection.setAutoCommit(false);
            for(String update : updates) {
                stmt = connection.createStatement();
                stmt.close();
            }
            connection.commit();
        } catch (SQLDataException | SQLSyntaxErrorException e) {
            LOG.error("Wrong query: " + arrayToString(updates), e);
            throw new WrongDataException();
        } catch (SQLException e) {
            LOG.error("Can't execute updates: " + arrayToString(updates), e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }  catch (Exception e) {
            LOG.error("Some exception!", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

    }

    private static String arrayToString(String[] strings) {
        String queries = "";
        for (String update : strings) {
            queries += update + ", ";
        }
        return queries;
    }
}
