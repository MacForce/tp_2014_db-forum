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
import java.io.Reader;
import java.util.Properties;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
//    private static final Properties properties = loadProperties();

//    private static Properties loadProperties() {
//        Properties properties = new Properties();
//        try (Reader reader = new FileReader("resources/config.properties")) {
//            properties.load(reader);
//        } catch (IOException e) {
//            LOG.error("Can't read properties!", e);
//            return null;
//        }
//        return properties;
//    }

    public static void main(String[] args) throws Exception {
        ConnectionPool connPool = new ConnectionPool();
        if (!connPool.testDBConnect()) {
            LOG.error("Stopping server, because database connection isn't established!!!");
            return;
        }
//        Common.updateIdsCounts(connPool.getConnection());

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(10);
        threadPool.setMaxThreads(1000);
        Server server = new Server(threadPool);

        ServerConnector connector=new ServerConnector(server);
//        int port = Integer.valueOf(properties.getProperty("port"));
        connector.setPort(8080);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new Servlet(server, new ClearDAOImpl(connPool),
                new ForumDAOImpl(connPool), new PostDAOImpl(connPool), new ThreadDAOImpl(connPool),
                new UserDAOImpl(connPool))), "/db/api/*");
        server.setHandler(context);

        LOG.info("Starting server at port: " + 8080);
        server.start();
        server.join();

        connPool.close();
    }
}
