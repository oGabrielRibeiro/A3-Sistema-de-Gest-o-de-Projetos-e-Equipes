package br.com.gpro.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.LinkedHashMap;

import br.com.gpro.infra.ConnectionFactory;
import br.com.gpro.model.Priority;
import br.com.gpro.model.Task;
import br.com.gpro.model.TaskStatus;

public class TaskDaoSqlite implements TaskDao {

    @Override
    public Long insert(Task t) {
        String sql = """
          INSERT INTO tasks (project_id, title, description, assignee_id, priority, status, estimate_h, spent_h, due_date)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, t.getProjectId());
            ps.setString(2, t.getTitle());
            ps.setString(3, t.getDescription());
            if (t.getAssigneeId() == null) ps.setNull(4, Types.INTEGER); else ps.setLong(4, t.getAssigneeId());
            ps.setString(5, t.getPriority().name());
            ps.setString(6, t.getStatus().name());
            if (t.getEstimateH() == null) ps.setNull(7, Types.REAL); else ps.setDouble(7, t.getEstimateH());
            ps.setDouble(8, t.getSpentH() == null ? 0.0 : t.getSpentH());
            ps.setString(9, t.getDueDate() != null ? t.getDueDate().toString() : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir tarefa: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Task> findById(Long id) {
        String sql = "SELECT * FROM tasks WHERE task_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar tarefa: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Task> listByProject(Long projectId) {
        String sql = "SELECT * FROM tasks WHERE project_id = ? ORDER BY (due_date IS NULL), due_date, priority DESC, task_id DESC";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar tarefas: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(Long taskId, TaskStatus status, Double addSpentHours) {
        String sql = "UPDATE tasks SET status = ?, spent_h = COALESCE(spent_h,0) + COALESCE(?,0) WHERE task_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (addSpentHours == null) ps.setNull(2, Types.REAL); else ps.setDouble(2, addSpentHours);
            ps.setLong(3, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status: " + e.getMessage(), e);
        }
    }

    @Override
    public void reassign(Long taskId, Long assigneeId) {
        String sql = "UPDATE tasks SET assignee_id = ? WHERE task_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (assigneeId == null) ps.setNull(1, Types.INTEGER); else ps.setLong(1, assigneeId);
            ps.setLong(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao reatribuir tarefa: " + e.getMessage(), e);
        }
    }

    private Task map(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getLong("task_id"));
        t.setProjectId(rs.getLong("project_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        long aid = rs.getLong("assignee_id");
        t.setAssigneeId(rs.wasNull() ? null : aid);
        t.setPriority(Priority.valueOf(rs.getString("priority")));
        t.setStatus(TaskStatus.valueOf(rs.getString("status")));
        double est = rs.getDouble("estimate_h"); if (rs.wasNull()) t.setEstimateH(null); else t.setEstimateH(est);
        t.setSpentH(rs.getDouble("spent_h"));
        String dd = rs.getString("due_date");
        t.setDueDate(dd == null ? null : LocalDate.parse(dd));
        String ca = rs.getString("created_at");
        t.setCreatedAt(ca == null ? null : LocalDate.parse(ca));
        return t;
    }

    @Override
    public void updateBasics(Long taskId, String title, String desc, Priority prio, Double estimateH, LocalDate dueDate) {
        String sql = """
        UPDATE tasks
        SET title=?, description=?, priority=?, estimate_h=?, due_date=?
        WHERE task_id=?
        """;
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, desc);
            ps.setString(3, prio.name());
            if (estimateH == null) ps.setNull(4, Types.REAL); else ps.setDouble(4, estimateH);
            ps.setString(5, dueDate == null ? null : dueDate.toString());
            ps.setLong(6, taskId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar tarefa: " + e.getMessage(), e); }
    }

    @Override
    public void delete(Long taskId) {
        String sql = "DELETE FROM tasks WHERE task_id = ?";
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir tarefa: " + e.getMessage(), e); }
    }

    @Override
    public java.util.List<br.com.gpro.model.Task> listarAbertasNoProjeto(long projectId) {
        String sql = """
            SELECT task_id, project_id, title, description, assignee_id, priority, status,
                estimate_h, spent_h, due_date, created_at
            FROM tasks
            WHERE project_id = ?
            AND UPPER(TRIM(status)) IN ('FAZER','ANDAMENTO','BLOQUEADO','REVISANDO')
            ORDER BY due_date IS NULL, due_date, task_id
        """;
        var out = new java.util.ArrayList<br.com.gpro.model.Task>();
        try (var con = java.sql.DriverManager.getConnection("jdbc:sqlite:gestao_projetos.db");
            var ps  = con.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    var t = new br.com.gpro.model.Task();
                    t.setId(rs.getLong("task_id"));
                    t.setProjectId(rs.getLong("project_id"));
                    t.setTitle(rs.getString("title"));
                    t.setDescription(rs.getString("description"));
                    var asg = rs.getObject("assignee_id");
                    t.setAssigneeId(asg == null ? null : rs.getLong("assignee_id"));
                    t.setPriority(br.com.gpro.model.Priority.valueOf(rs.getString("priority")));
                    // lê como veio; se quiser, pode normalizar aqui também:
                    t.setStatus(br.com.gpro.model.TaskStatus.valueOf(rs.getString("status").trim().toUpperCase()));
                    var due = rs.getString("due_date");
                    t.setDueDate(due == null ? null : java.time.LocalDate.parse(due));
                    t.setEstimateH((Double) rs.getObject("estimate_h"));
                    t.setSpentH(rs.getDouble("spent_h"));
                    out.add(t);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar tarefas abertas do projeto", e);
        }
        return out;
    }

    @Override
    public void atualizarStatus(Long taskId, TaskStatus status) {
        String sql = "UPDATE tasks SET status = ? WHERE task_id = ?";
        try (var con = java.sql.DriverManager.getConnection("jdbc:sqlite:gestao_projetos.db");
            var ps  = con.prepareStatement(sql)) {
            ps.setString(1, status.name()); // grava 'CONCLUIDO'
            ps.setLong(2, taskId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar status da tarefa", e);
        }
    }

    @Override
    public int countAbertasNoProjeto(long projectId) {
        String sql = """
            SELECT COUNT(*)
            FROM tasks
            WHERE project_id = ?
            AND UPPER(TRIM(status)) IN ('FAZER','ANDAMENTO','BLOQUEADO','REVISANDO')
        """;
        try (var con = java.sql.DriverManager.getConnection("jdbc:sqlite:gestao_projetos.db");
            var ps  = con.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar tarefas abertas do projeto", e);
        }
    }

    @Override
    public Map<String, Integer> distinctStatusPorProjeto(long projectId) {
        String sql = """
            SELECT TRIM(status) AS s, COUNT(*) AS c
            FROM tasks
            WHERE project_id = ?
            GROUP BY TRIM(status)
            ORDER BY c DESC
        """;
        Map<String, Integer> map = new LinkedHashMap<>();
        try (var con = java.sql.DriverManager.getConnection("jdbc:sqlite:gestao_projetos.db");
            var ps  = con.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("s"), rs.getInt("c"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar status distintos do projeto", e);
        }
        return map;
    }

}
