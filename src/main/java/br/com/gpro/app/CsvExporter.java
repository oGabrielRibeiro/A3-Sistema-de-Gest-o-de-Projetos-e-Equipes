package br.com.gpro.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class CsvExporter {
    public static Path write(String fileName, List<String[]> rows) {
        try {
            Path p = Path.of(fileName);
            try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                for (String[] r : rows) {
                    w.write(escape(r)); w.newLine();
                }
            }
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar CSV: " + e.getMessage(), e);
        }
    }

    private static String escape(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<cols.length;i++) {
            if (i>0) sb.append(',');
            String v = cols[i] == null ? "" : cols[i];
            boolean need = v.contains(",") || v.contains("\"") || v.contains("\n");
            if (need) {
                sb.append('"').append(v.replace("\"","\"\"")).append('"');
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }
    // no ReportService:
    public List<String[]> tarefasAtrasadasCsv() {
        String sql = """
            SELECT p.name AS project, COALESCE(u.full_name,'(sem responsável)') AS assignee, t.title, t.due_date
            FROM tasks t
            JOIN projects p ON p.project_id = t.project_id
            LEFT JOIN users u ON u.user_id = t.assignee_id
            WHERE t.status <> 'DONE' AND t.due_date IS NOT NULL AND DATE(t.due_date) < DATE('now')
            ORDER BY p.name, t.due_date, assignee
        """;
        var rows = new java.util.ArrayList<String[]>();
        rows.add(new String[]{"Projeto","Responsável","Tarefa","Vencimento"});
        try (var c = br.com.gpro.infra.ConnectionFactory.get(); var st = c.createStatement(); var rs = st.executeQuery(sql)) {
            while (rs.next()) rows.add(new String[]{
                rs.getString("project"), rs.getString("assignee"), rs.getString("title"), rs.getString("due_date")
            });
        } catch (Exception e) { throw new RuntimeException("Erro no relatório CSV: " + e.getMessage(), e); }
        return rows;
    }

}
