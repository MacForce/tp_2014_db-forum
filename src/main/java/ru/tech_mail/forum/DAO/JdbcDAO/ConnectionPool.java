package ru.tech_mail.forum.DAO.JdbcDAO;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionPool {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionPool.class);
    private BasicDataSource connectionPool;

    public ConnectionPool() {
        connectionPool = new BasicDataSource();
        connectionPool.setDriverClassName("com.mysql.jdbc.Driver");
        connectionPool.setUrl("jdbc:mysql://127.0.0.1:3306/forum-v2?useUnicode=true&characterEncoding=UTF-8");
        connectionPool.setUsername("root");
        connectionPool.setPassword("");
        connectionPool.setInitialSize(15);
        connectionPool.setDefaultAutoCommit(true);
        connectionPool.setMaxIdle(25);
//        connectionPool.setPoolPreparedStatements(true);
//        connectionPool.setMaxOpenPreparedStatements(10);
        connectionPool.setMaxTotal(50);
        connectionPool.setMaxWaitMillis(10_000);
        connectionPool.setDefaultQueryTimeout(1000);
    }

    public boolean testDBConnect() {
        try {
            Connection connection = connectionPool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            return rs.next();
        } catch (SQLException e) {
            LOG.error("Testing connection failed!", e);
            return false;
        }
    }

    public Connection getConnection() {
        try {
            return connectionPool.getConnection();
        } catch (SQLException e) {
            LOG.error("Can't get connection!", e);
            return null;
        }
    }

    public void close() {
        try {
            connectionPool.close();
        } catch (SQLException e) {
            LOG.error("Can't close connection pool!", e);
        }
    }

    public void returnConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            LOG.error("Can't close connection!", e);
        }
    }
}
