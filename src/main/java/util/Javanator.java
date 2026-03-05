package util;

import org.h2.tools.Server;
import jakarta.persistence.*;
import java.sql.*;

public class Javanator {
    private static Server h2Server;
    private static EntityManagerFactory emf;

    static {
        try {
            h2Server = Server.createTcpServer(
                    "-tcpPort", "9092",
                    "-tcpAllowOthers",
                    "-ifNotExists"
            ).start();

            System.out.println("H2 TCP Server iniciado.");

            emf = Persistence.createEntityManagerFactory("blogPU");

            System.out.println("EMF creado.");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start H2 or EMF", e);
        }
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void stopServer() {
        if (emf != null) emf.close();
        if (h2Server != null) h2Server.stop();
    }
}