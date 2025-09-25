package br.com.gpro.controller;

import java.util.List;

import br.com.gpro.dao.TeamDao;
import br.com.gpro.dao.UserDao;
import br.com.gpro.model.Team;

public class TeamController {
    private final TeamDao teamDao;
    private final UserDao userDao;

    public TeamController(TeamDao teamDao, UserDao userDao) {
        this.teamDao = teamDao;
        this.userDao = userDao;
    }

    public Long criarEquipe(String name, String desc) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome da equipe é obrigatório");
        Team t = new Team();
        t.setName(name.trim());
        t.setDescription(desc);
        return teamDao.insert(t);
    }

    public void adicionarMembro(Long teamId, Long userId, String roleInTeam) {
        userDao.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuário inexistente"));
        teamDao.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Equipe inexistente"));
        teamDao.addMember(teamId, userId, roleInTeam);
    }

    public void removerMembro(Long teamId, Long userId) {
        teamDao.removeMember(teamId, userId);
    }

    public List<Team> listarEquipes() {
        return teamDao.listAll();
    }
    public void excluirEquipe(Long id) {
        teamDao.delete(id); // cascata remove team_members e project_teams
    }
    public List<br.com.gpro.model.User> listarMembros(long teamId) {
        return teamDao.listarMembros(teamId);
    }

}
