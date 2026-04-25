package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    public String PATH = ConfigLoader.get("DB_URL", "jdbc:mysql://localhost:3306/agrismart");
    public String user = ConfigLoader.get("DB_USER", "root");
    public String pwd = ConfigLoader.get("DB_PASS", "");
    public Connection conn;
    public static MyConnection instance;

    private MyConnection() {
        try {
            conn = DriverManager.getConnection(PATH, user, pwd);
            System.out.println("cnx etabli !!!!!!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }
}
