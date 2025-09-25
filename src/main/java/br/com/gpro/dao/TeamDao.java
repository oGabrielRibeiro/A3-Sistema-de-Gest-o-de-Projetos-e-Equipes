package br.com.gpro.dao;

import java.util.List;
import java.util.Optional;

import br.com.gpro.model.Team;

public interface TeamDao {
    Long insert(Team t);
    Optional<Team> findById(Long id);
    Optional<Team> findByName(String name);
    List<Team> listAll();

    void addMember(Long teamId, Long userId, String roleInTeam);
    void removeMember(Long teamId, Long userId);
    List<Long> listMemberUserIds(Long teamId);
    List<br.com.gpro.model.User> listarMembros(long teamId);

    void delete(Long id);  // NEW
}

