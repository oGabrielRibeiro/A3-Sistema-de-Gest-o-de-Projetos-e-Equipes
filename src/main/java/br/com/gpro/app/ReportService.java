package br.com.gpro.app;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import br.com.gpro.infra.ConnectionFactory;

public class ReportService {

    public List<String> tarefasAtrasadas() {
        String sql = """
            SELECT p.project_id, p.name AS project, u.full_name AS assignee, t.title, t.due_date
            FROM tasks t
            JOIN projects p ON p.project_id = t.project_id
            LEFT JOIN users u ON u.user_id = t.assignee_id
            WHERE t.status <> 'DONE'
              AND t.due_date IS NOT NULL
              AND DATE(t.due_date) < DATE('now')
            ORDER BY p.name, t.due_date, u.full_name
        """;
        List<String> out = new ArrayList<>();
        try (Connection c = ConnectionFactory.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(String.format(
                    "[P%03d] %s | %s | %s | due=%s",
                    rs.getInt("project_id"),
                    rs.getString("project"),
                    rs.getString("assignee") == null ? "(sem responsável)" : rs.getString("assignee"),
                    rs.getString("title"),
                    rs.getString("due_date")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro no relatório de tarefas atrasadas: " + e.getMessage(), e);
        }
        return out;
    }

    public List<String> cargaPorUsuario() {
        String sql = """
            SELECT u.user_id, u.full_name,
                   COALESCE(SUM(t.estimate_h),0) AS est_total,
                   COALESCE(SUM(t.spent_h),0)    AS spent_total
            FROM users u
            JOIN tasks t ON t.assignee_id = u.user_id
            JOIN projects p ON p.project_id = t.project_id
            WHERE p.status IN ('PLANNED','IN_PROGRESS')
            GROUP BY u.user_id, u.full_name
            ORDER BY u.full_name
        """;
        List<String> out = new ArrayList<>();
        try (Connection c = ConnectionFactory.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                double est = rs.getDouble("est_total");
                double spent = rs.getDouble("spent_total");
                double pct = est == 0 ? 0 : (spent / est) * 100.0;
                out.add(String.format(
                    "[U%03d] %s | est=%.1fh | spent=%.1fh | perf=%.1f%%",
                    rs.getInt("user_id"),
                    rs.getString("full_name"),
                    est, spent, pct));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro no relatório de carga por usuário: " + e.getMessage(), e);
        }
        return out;
    }
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
