package br.com.gpro.app;

import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import br.com.gpro.dao.UserDao;
import br.com.gpro.model.User;

public class AuthService {
    private final UserDao userDao;
    private User currentUser;

    public AuthService(UserDao userDao) { this.userDao = userDao; }

  public boolean login(String username, String plainPassword) {
    if (username == null || plainPassword == null) return false;
    String ukey = username.trim().toLowerCase();

    Optional<User> opt = userDao.findByUsername(ukey);
    if (opt.isEmpty()) return false;

    User u = opt.get();
    if (!u.isActive()) return false;

    String stored = u.getPassword();
    boolean ok;

    // Caso 1: já está com BCrypt ($2a/$2b/$2y)
    if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))) {
        ok = BCrypt.checkpw(plainPassword, stored);
    } else {
        // Caso 2: senha antiga em texto puro -> compara uma vez e UPGRADE para hash
        ok = stored != null && stored.equals(plainPassword);
        if (ok) {
            String newHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            userDao.changePassword(u.getId(), newHash);
            u.setPassword(newHash);
        }
    }

    if (ok) { currentUser = u; }
    return ok;
}
    public void logout() { currentUser = null; }
    public boolean isLogged() { return currentUser != null; }
    public User getCurrentUser() { return currentUser; }

}
