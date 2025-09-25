package br.com.gpro.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.gpro.infra.ConnectionFactory;
import br.com.gpro.model.Project;
import br.com.gpro.model.ProjectStatus;

public class ProjectDaoSqlite implements ProjectDao {

    @Override
    public Long insert(Project p) {
        String sql = """
            INSERT INTO projects (name, description, start_date, due_date, status, manager_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getStartDate().toString()); // ISO yyyy-MM-dd
            ps.setString(4, p.getDueDate() != null ? p.getDueDate().toString() : null);
            ps.setString(5, p.getStatus().name());
            ps.setLong(6, p.getManagerId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir projeto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Project> findById(Long id) {
        String sql = "SELECT * FROM projects WHERE project_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar projeto: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Project> listAll() {
        String sql = "SELECT * FROM projects ORDER BY start_date DESC, name";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Project> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar projetos: " + e.getMessage(), e);
        }
    }

    private Project map(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getLong("project_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        String sd = rs.getString("start_date");
        String dd = rs.getString("due_date");
        p.setStartDate(sd != null ? LocalDate.parse(sd) : null);
        p.setDueDate(dd != null && !dd.isBlank() ? LocalDate.parse(dd) : null);
        p.setStatus(ProjectStatus.valueOf(rs.getString("status")));
        p.setManagerId(rs.getLong("manager_id"));
        return p;
    }
    @Override
    public void addTeam(Long projectId, Long teamId) {
        String sql = "INSERT OR IGNORE INTO project_teams (project_id, team_id) VALUES (?, ?)";
        try (Connection c = ConnectionFactory.get();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao alocar equipe no projeto: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeTeam(Long projectId, Long teamId) {
        String sql = "DELETE FROM project_teams WHERE project_id = ? AND team_id = ?";
        try (Connection c = ConnectionFactory.get();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao desalocar equipe do projeto: " + e.getMessage(), e);
        }
    }
    @Override
    public void update(Project p) {
        String sql = """
        UPDATE projects
        SET name=?, description=?, start_date=?, due_date=?, status=?, manager_id=?
        WHERE project_id=?
        """;
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getStartDate().toString());
            ps.setString(4, p.getDueDate() == null ? null : p.getDueDate().toString());
            ps.setString(5, p.getStatus().name());
            ps.setLong(6, p.getManagerId());
            ps.setLong(7, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar projeto: " + e.getMessage(), e); }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM projects WHERE project_id = ?";
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate(); // FK ON DELETE CASCADE cuida de tasks/project_teams
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir projeto: " + e.getMessage(), e); }
    }

    @Override
    public Project buscarPorId(long projectId) {
        String sql = """
            SELECT project_id, name, description, start_date, due_date, status, manager_id
            FROM projects
            WHERE project_id = ?
        """;
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:gestao_projetos.db");
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Project p = new Project();
                    p.setId(rs.getLong("project_id"));
                    p.setName(rs.getString("name"));
                    p.setDescription(rs.getString("description"));
                    var sd = rs.getString("start_date");
                    p.setStartDate(sd == null ? null : java.time.LocalDate.parse(sd));
                    var dd = rs.getString("due_date");
                    p.setDueDate(dd == null ? null : java.time.LocalDate.parse(dd));
                    p.setStatus(br.com.gpro.model.ProjectStatus.valueOf(rs.getString("status")));
                    p.setManagerId(rs.getLong("manager_id"));
                    return p;
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar projeto por id", e);
        }
    }

    @Override
    public void atualizarStatus(long projectId, br.com.gpro.model.ProjectStatus status) {
        String sql = "UPDATE projects SET status = ? WHERE project_id = ?";
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:gestao_projetos.db");
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name()); // grava PLANEJADO/ANDAMENTO/CONCLUIDO/CANCELADO
            ps.setLong(2, projectId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar status do projeto", e);
        }
    }
    
}
