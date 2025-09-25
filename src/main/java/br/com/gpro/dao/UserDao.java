package br.com.gpro.dao;

import java.util.List;
import java.util.Optional;

import br.com.gpro.model.Role;
import br.com.gpro.model.User;

public interface UserDao {
    Long insert(User u);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> listAll();

    void updateBasics(User u);                 // nome, email, cargo
    void changePassword(Long userId, String passwordHash);
    void changeRole(Long userId, Role role);
    void setActive(Long userId, boolean active);
}
