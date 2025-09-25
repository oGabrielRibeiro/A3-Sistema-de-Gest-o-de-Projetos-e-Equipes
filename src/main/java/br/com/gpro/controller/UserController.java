package br.com.gpro.controller;

import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import br.com.gpro.dao.UserDao;
import br.com.gpro.model.Role;
import br.com.gpro.model.User;
import br.com.gpro.util.CpfUtils;


public class UserController {
    private final UserDao userDao;
    public UserController(UserDao userDao) { this.userDao = userDao; }

    public Long criarAdminSeNaoExiste() {
        return userDao.findByUsername("admin")
            .map(User::getId)
            .orElseGet(() -> {
                User u = new User();
                u.setFullName("Administrador");
                u.setCpf("00000000000");
                u.setEmail("admin@sistema.com");
                u.setJobTitle("Administrador");
                u.setUsername("admin");
                String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
                u.setPassword(hash); // salva o HASH
                u.setRole(Role.ADM);
                return userDao.insert(u);
            });
    }

    public List<User> listarUsuarios() {
        return userDao.listAll();
    }
    public Long criarUsuario(String nome, String cpf, String email, String cargo,
                            String username, String senhaPlano, Role role) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome obrigatório");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username obrigatório");
        if (!emailValido(email)) throw new IllegalArgumentException("E-mail inválido");
        if (!CpfUtils.isValid(cpf)) throw new IllegalArgumentException("CPF inválido");

        User u = new User();
        u.setFullName(nome.trim());
        u.setCpf(CpfUtils.onlyDigits(cpf));
        u.setEmail(email.trim().toLowerCase());
        u.setJobTitle(cargo);
        u.setUsername(username.trim().toLowerCase());
        u.setPassword(BCrypt.hashpw(senhaPlano, BCrypt.gensalt()));
        u.setRole(role == null ? Role.OPER : role);
        return userDao.insert(u);
    }

    public void atualizarBasico(Long id, String nome, String email, String cargo) {
        if (id == null) throw new IllegalArgumentException("Id obrigatório");
        if (!emailValido(email)) throw new IllegalArgumentException("E-mail inválido");
        User u = userDao.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        u.setFullName(nome);
        u.setEmail(email.trim().toLowerCase());
        u.setJobTitle(cargo);
        userDao.updateBasics(u);
    }

    public void alterarSenha(Long id, String senhaAtual, String novaSenha, boolean adminForcando) {
        var u = userDao.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (!adminForcando) {
            if (!org.mindrot.jbcrypt.BCrypt.checkpw(senhaAtual, u.getPassword()))
                throw new IllegalArgumentException("Senha atual incorreta");
        }
        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(novaSenha, org.mindrot.jbcrypt.BCrypt.gensalt());
        userDao.changePassword(id, hash);
    }

    public void alterarPerfil(Long id, Role role) { userDao.changeRole(id, role); }
    public void setAtivo(Long id, boolean ativo) { userDao.setActive(id, ativo); }

    /* --- helpers simples --- */
    private static boolean emailValido(String e) {
        return e != null && e.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    private static String soDigitos(String s) { return s == null ? "" : s.replaceAll("\\D",""); }
    private static boolean cpfValido(String cpf) {
        String d = soDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count()==1) return false;
        return dv(d, 9) == Character.getNumericValue(d.charAt(9))
            && dv(d,10) == Character.getNumericValue(d.charAt(10));
    }
    private static int dv(String d, int len) {
        int soma=0, peso=len+1;
        for(int i=0;i<len;i++) soma += (d.charAt(i)-'0') * (peso--);
        int r = 11 - (soma % 11);
        return (r>9)?0:r;
    }
}
