package ru.tech_mail.forum.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tech_mail.forum.DAO.JdbcDAO.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(this.getClass().getClassLoader()
                .getResourceAsStream("config.properties"))) {
            properties.load(reader);
        } catch (IOException e) {
            LOG.error("Can't read properties!", e);
            System.exit(1);
        }
        return properties;
    }


    public static void main(String[] args) throws Exception {
        Properties properties = new Main().loadProperties();
        ConnectionPool connPool = new ConnectionPool(properties);
        if (!connPool.testDBConnect()) {
            LOG.error("Stopping server, because database connection isn't established!!!");
            return;
        }

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(Integer.valueOf(properties.getProperty("minServerThreads")));
        threadPool.setMaxThreads(Integer.valueOf(properties.getProperty("maxServerThreads")));
        Server server = new Server(threadPool);

        ServerConnector connector=new ServerConnector(server);
        int port = Integer.valueOf(properties.getProperty("port"));
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new Servlet(server, new ClearDAOImpl(connPool),
                new ForumDAOImpl(connPool), new PostDAOImpl(connPool), new ThreadDAOImpl(connPool),
                new UserDAOImpl(connPool))), "/db/api/*");
        server.setHandler(context);

        LOG.info("Starting server at port: " + port);
        server.start();
        server.join();

        connPool.close();
    }
}
