package br.com.gpro.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.gpro.infra.ConnectionFactory;
import br.com.gpro.model.Role;
import br.com.gpro.model.User;

public class UserDaoSqlite implements UserDao {

    @Override
    public Long insert(User u) {
        String sql = """
            INSERT INTO users (full_name, cpf, email, job_title, username, password, role, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, 1)
        """;
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getCpf());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getJobTitle());
            ps.setString(5, u.getUsername());
            ps.setString(6, u.getPassword());
            ps.setString(7, u.getRole().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<User> listAll() {
        String sql = "SELECT * FROM users ORDER BY full_name";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setCpf(rs.getString("cpf"));
        u.setEmail(rs.getString("email"));
        u.setJobTitle(rs.getString("job_title"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getInt("is_active") == 1);
        return u;
    }
    @Override
    public void updateBasics(User u) {
        String sql = "UPDATE users SET full_name=?, email=?, job_title=? WHERE user_id=?";
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getJobTitle());
            ps.setLong(4, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao atualizar usuário: " + e.getMessage(), e); }
    }

    @Override
    public void changePassword(Long userId, String passwordHash) {
        String sql = "UPDATE users SET password=? WHERE user_id=?";
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao alterar senha: " + e.getMessage(), e); }
    }

    @Override
    public void changeRole(Long userId, Role role) {
        String sql = "UPDATE users SET role=? WHERE user_id=?";
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao alterar perfil: " + e.getMessage(), e); }
    }

    @Override
    public void setActive(Long userId, boolean active) {
        String sql = "UPDATE users SET is_active=? WHERE user_id=?";
        try (var c = ConnectionFactory.get(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Erro ao ativar/desativar usuário: " + e.getMessage(), e); }
    }

}
