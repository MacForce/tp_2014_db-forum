package ru.tech_mail.forum.DAO.JdbcDAO;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class ConnectionPool {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionPool.class);
    private final Properties properties;
    private BasicDataSource connectionPool;

    public ConnectionPool(Properties properties) {
        this.properties = properties;
        connectionPool = new BasicDataSource();
        connectionPool.setDriverClassName("com.mysql.jdbc.Driver");
//        connectionPool.setUrl("jdbc:mysql://127.0.0.1:3306/forum_db?useUnicode=true&characterEncoding=UTF-8");
        connectionPool.setUrl(properties.getProperty("databaseUrl"));
        connectionPool.setUsername(properties.getProperty("dbLogin"));
        connectionPool.setPassword(properties.getProperty("dbPass"));
        connectionPool.setInitialSize(Integer.valueOf(properties.getProperty("initDBPoolSize")));
        connectionPool.setDefaultAutoCommit(true);
        connectionPool.setMaxIdle(Integer.valueOf(properties.getProperty("maxIdleDBPool")));
//        connectionPool.setPoolPreparedStatements(true);
//        connectionPool.setMaxOpenPreparedStatements(10);
        connectionPool.setMaxTotal(Integer.valueOf(properties.getProperty("maxTotalDBPool")));
        connectionPool.setMaxWaitMillis(Integer.valueOf(properties.getProperty("maxWaitMillisDBPool")));
        connectionPool.setDefaultQueryTimeout(Integer.valueOf(properties.getProperty("defaultQueryDBPool")));
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
