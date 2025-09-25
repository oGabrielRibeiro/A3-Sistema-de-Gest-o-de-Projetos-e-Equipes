package br.com.gpro.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.gpro.infra.ConnectionFactory;
import br.com.gpro.model.Team;

public class TeamDaoSqlite implements TeamDao {
    @Override
    public Long insert(Team t) {
        String sql = "INSERT INTO teams (team_name, description) VALUES (?, ?)";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getDescription());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir equipe: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Team> findById(Long id) {
        String sql = "SELECT * FROM teams WHERE team_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar equipe: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Team> findByName(String name) {
        String sql = "SELECT * FROM teams WHERE team_name = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar equipe por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Team> listAll() {
        String sql = "SELECT * FROM teams ORDER BY team_name";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Team> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar equipes: " + e.getMessage(), e);
        }
    }

    @Override
    public void addMember(Long teamId, Long userId, String roleInTeam) {
        String sql = "INSERT OR IGNORE INTO team_members (team_id, user_id, role_in_team) VALUES (?, ?, ?)";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, teamId);
            ps.setLong(2, userId);
            ps.setString(3, roleInTeam);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar membro à equipe: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeMember(Long teamId, Long userId) {
        String sql = "DELETE FROM team_members WHERE team_id = ? AND user_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, teamId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover membro da equipe: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Long> listMemberUserIds(Long teamId) {
        String sql = "SELECT user_id FROM team_members WHERE team_id = ? ORDER BY user_id";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Long> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getLong(1));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar membros: " + e.getMessage(), e);
        }
    }

    private Team map(ResultSet rs) throws SQLException {
        Team t = new Team();
        t.setId(rs.getLong("team_id"));
        t.setName(rs.getString("team_name"));
        t.setDescription(rs.getString("description"));
        return t;
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM teams WHERE team_id = ?";
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate(); // ON DELETE CASCADE remove vínculos
        } catch (SQLException e) { throw new RuntimeException("Erro ao excluir equipe: " + e.getMessage(), e); }
    }

    @Override
    public List<br.com.gpro.model.User> listarMembros(long teamId) {
        String sql = """
            SELECT u.user_id, u.full_name, u.email, u.username, u.job_title, u.role, u.is_active
            FROM team_members tm
            JOIN users u ON u.user_id = tm.user_id
            WHERE tm.team_id = ?
            ORDER BY u.full_name
        """;
        List<br.com.gpro.model.User> out = new java.util.ArrayList<>();
        try (Connection con = conn();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var u = new br.com.gpro.model.User();
                    u.setId(rs.getLong("user_id"));
                    u.setFullName(rs.getString("full_name"));
                    u.setEmail(rs.getString("email"));
                    u.setUsername(rs.getString("username"));
                    u.setJobTitle(rs.getString("job_title"));
                    u.setRole(br.com.gpro.model.Role.valueOf(rs.getString("role")));
                    u.setActive(rs.getInt("is_active") == 1);
                    out.add(u);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar membros da equipe", e);
        }
        return out;
    }

    private static final String URL = "jdbc:sqlite:gestao_projetos.db";

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(URL);
    }

}
