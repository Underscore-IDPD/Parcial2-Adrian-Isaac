package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

public class Cockroach {

    public static void registrarLogin(String username) {

        String url = System.getenv("JDBC_DATABASE_URL");

        try (Connection conn = DriverManager.getConnection(url)) {

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO auditoria_login (username, fecha) VALUES (?, ?)"
            );

            ps.setString(1, username);
            ps.setObject(2, LocalDateTime.now());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}