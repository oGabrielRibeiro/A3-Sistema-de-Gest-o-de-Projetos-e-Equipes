package br.com.gpro.dao;

import java.util.List;
import java.util.Optional;

import br.com.gpro.model.Project;
import br.com.gpro.model.ProjectStatus;

public interface ProjectDao {
    Long insert(Project p);
    Optional<Project> findById(Long id);
    List<Project> listAll();
    Project buscarPorId(long projectId);

    void addTeam(Long projectId, Long teamId);
    void removeTeam(Long projectId, Long teamId);
    void atualizarStatus(long projectId, ProjectStatus status);

    void update(Project p);     // NEW
    void delete(Long id);       // NEW
}

