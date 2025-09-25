package br.com.gpro.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    // cria o .db na raiz do projeto; ativa foreign_keys via URL
    private static final String URL = "jdbc:sqlite:" 
        + System.getProperty("user.dir") 
        + "/gestao_projetos.db?foreign_keys=on";

    static {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) { throw new RuntimeException(e); }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
