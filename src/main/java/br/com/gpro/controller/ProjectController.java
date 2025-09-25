package br.com.gpro.controller;

import java.time.LocalDate;
import java.util.List;

import br.com.gpro.dao.ProjectDao;
import br.com.gpro.dao.TaskDao;
import br.com.gpro.dao.UserDao;
import br.com.gpro.model.Project;
import br.com.gpro.model.ProjectStatus;
import br.com.gpro.model.Role;

public class ProjectController {
    private final ProjectDao projectDao;
    private final UserDao userDao;
    private final TaskDao taskDao;

    public ProjectController(ProjectDao projectDao, UserDao userDao, TaskDao taskDao) {
        this.projectDao = projectDao;
        this.userDao = userDao;
        this.taskDao = taskDao;
    }

    public Long criarProjeto(String name, String desc, LocalDate start, LocalDate due, ProjectStatus status, Long managerId) {
        // Valida gerente
        var managerOpt = userDao.findById(managerId);
        if (managerOpt.isEmpty()) throw new IllegalArgumentException("Gerente inexistente (id=" + managerId + ")");
        var manager = managerOpt.get();
        if (!(manager.getRole() == Role.GER || manager.getRole() == Role.ADM)) {
            throw new IllegalArgumentException("Usuário não possui perfil de gerente/admin");
        }
        // Valida datas
        if (start == null) throw new IllegalArgumentException("Data de início obrigatória");
        if (due != null && due.isBefore(start)) throw new IllegalArgumentException("Data de término não pode ser anterior ao início");

        Project p = new Project();
        p.setName(name);
        p.setDescription(desc);
        p.setStartDate(start);
        p.setDueDate(due);
        p.setStatus(status != null ? status : ProjectStatus.PLANEJADO);
        p.setManagerId(managerId);

        return projectDao.insert(p);
    }

    public List<Project> listarProjetos() {
        return projectDao.listAll();
    }
    public void alocarEquipeEmProjeto(Long projectId, Long teamId) {
    // valida se existem
    projectDao.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Projeto inexistente"));
    // apenas checa a existência da equipe via TeamDao indiretamente no menu (ou injete TeamDao aqui para validar também)
    projectDao.addTeam(projectId, teamId);
    }

    public void desalocarEquipeDoProjeto(Long projectId, Long teamId) {
        projectDao.removeTeam(projectId, teamId);
    }
    // dentro de ProjectController (adicione este método)
    public String progressoProjeto(Long projectId) {
        var p = projectDao.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Projeto inexistente"));
        String sql = """
        SELECT 
            COUNT(*) AS total,
            SUM(CASE WHEN status='DONE' THEN 1 ELSE 0 END) AS done,
            SUM(CASE WHEN status!='DONE' AND due_date IS NOT NULL AND due_date < DATE('now') THEN 1 ELSE 0 END) AS overdue
        FROM tasks WHERE project_id = ?
        """;
        try (var c = br.com.gpro.infra.ConnectionFactory.get();
            var ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int done = rs.getInt("done");
                    int overdue = rs.getInt("overdue");
                    double pct = total == 0 ? 0.0 : (100.0 * done / total);
                    return String.format("Projeto [%d] %s → %.1f%% concluído | %d/%d done | %d atrasadas",
                            p.getId(), p.getName(), pct, done, total, overdue);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro no relatório de progresso: " + e.getMessage(), e);
        }
        return "Sem dados.";
    }
    public void atualizarProjeto(Project p) {
        // validar campos essenciais
        if (p.getId() == null) throw new IllegalArgumentException("Id obrigatório");
        if (p.getStartDate() == null) throw new IllegalArgumentException("Data de início obrigatória");
        if (p.getDueDate() != null && p.getDueDate().isBefore(p.getStartDate()))
            throw new IllegalArgumentException("Data de término não pode ser anterior ao início");
        projectDao.update(p);
    }

    public void excluirProjeto(Long id) {
        projectDao.delete(id); // cascata remove tasks e vínculos
    }

    public void concluirProjeto(long projectId) {
        var p = projectDao.buscarPorId(projectId);
        if (p == null) throw new IllegalArgumentException("Projeto inexistente: " + projectId);

        int abertas = taskDao.countAbertasNoProjeto(projectId);
        if (abertas > 0) {
            // Detalhes úteis para entender o que sobrou
            var pend = taskDao.listarAbertasNoProjeto(projectId);
            String linhas = pend.stream().limit(5)
                    .map(t -> "- #" + t.getId() + " " + t.getTitle() + " [" + t.getStatus().pt() + "]")
                    .reduce("", (a,b) -> a + (a.isEmpty() ? "" : "\n") + b);

            // Status brutos no BD (para achar whitespace/variações)
            var dist = taskDao.distinctStatusPorProjeto(projectId);
            String distTxt = dist.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce("", (a,b) -> a + (a.isEmpty() ? "" : ", ") + b);

            throw new IllegalStateException(
                "Projeto não pode ser concluído: há " + abertas + " tarefa(s) pendente(s)."
            + (linhas.isBlank() ? "" : "\n" + linhas)
            + (dist.isEmpty() ? "" : "\n\nStatus no BD: " + distTxt)
            );
        }

        projectDao.atualizarStatus(projectId, br.com.gpro.model.ProjectStatus.CONCLUIDO);

        // (opcional) revalidar e retornar/mostrar novo status
        var p2 = projectDao.buscarPorId(projectId);
        if (p2 != null && p2.getStatus() != br.com.gpro.model.ProjectStatus.CONCLUIDO) {
            throw new IllegalStateException("Falha ao atualizar status do projeto no BD.");
        }
    }
}
