package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private final String PATH = "jdbc:mysql://localhost:3306/agrismart";
    private final String user = "root";
    private final String pwd = "";  
    private Connection conn;
    private static MyConnection instance;

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
