package br.com.gpro.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DbInit {
    public static void run(String schemaPath, String seedPath) {
        try (Connection c = ConnectionFactory.get()) {
            c.setAutoCommit(false);
            if (schemaPath != null) execSqlFile(c, schemaPath);
            if (seedPath != null && Files.exists(Path.of(seedPath))) execSqlFile(c, seedPath);
            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao inicializar BD: " + e.getMessage(), e);
        }
    }

    private static void execSqlFile(Connection c, String path) throws IOException, SQLException {
        String content = Files.readString(Path.of(path), java.nio.charset.StandardCharsets.UTF_8);
        // normaliza EOL e remove BOM
        content = content.replace("\r\n", "\n").replace("\r", "\n");
        if (content.startsWith("\uFEFF")) content = content.substring(1);

        StringBuilder current = new StringBuilder();
        int count = 0;
        try (Statement st = c.createStatement()) {
            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) continue; // ignora comentários/linhas vazias
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) { // terminou um comando
                    String sql = current.toString().trim();
                    // remove ; final
                    if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();
                    if (!sql.isEmpty()) {
                        try {
                            st.execute(sql); // funciona para DDL/DML
                            count++;
                        } catch (SQLException ex) {
                            throw new SQLException("Erro ao executar SQL: " + sql + " -> " + ex.getMessage(), ex);
                        }
                    }
                    current.setLength(0);
                }
            }
            // pega comando final sem ';' (se houver)
            String leftover = current.toString().trim();
            if (!leftover.isEmpty()) {
                st.execute(leftover);
                count++;
            }
        }
        System.out.println("DbInit: " + count + " comandos executados de " + path);
    }
}
