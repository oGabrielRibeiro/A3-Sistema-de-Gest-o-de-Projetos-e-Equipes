package br.com.gpro.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import br.com.gpro.dao.ProjectDao;
import br.com.gpro.dao.TaskDao;
import br.com.gpro.dao.UserDao;
import br.com.gpro.infra.ConnectionFactory;
import br.com.gpro.model.Priority;
import br.com.gpro.model.Task;
import br.com.gpro.model.TaskStatus;

public class TaskController {
    private final TaskDao taskDao;
    private final ProjectDao projectDao;
    private final UserDao userDao;

    public TaskController(TaskDao taskDao, ProjectDao projectDao, UserDao userDao) {
        this.taskDao = taskDao;
        this.projectDao = projectDao;
        this.userDao = userDao;
    }

    public Long criarTarefa(Long projectId, String title, String desc, Long assigneeId,
                            Priority prio, Double estimateH, LocalDate dueDate) {
        projectDao.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Projeto inexistente"));

        if (title == null || title.isBlank()) throw new IllegalArgumentException("Título é obrigatório");

        if (assigneeId != null) {
            // valida existencia do usuário
            userDao.findById(assigneeId).orElseThrow(() -> new IllegalArgumentException("Responsável inexistente"));
            // valida se o usuário participa do projeto via view project_members_v
            if (!isUserInProject(projectId, assigneeId)) {
                throw new IllegalArgumentException("Responsável não está alocado ao projeto (via equipe)");
            }
        }
        if (dueDate != null && dueDate.isBefore(LocalDate.now().minusYears(50))) {
            throw new IllegalArgumentException("Data de vencimento inválida");
        }

        Task t = new Task();
        t.setProjectId(projectId);
        t.setTitle(title.trim());
        t.setDescription(desc);
        t.setAssigneeId(assigneeId);
        t.setPriority(prio == null ? Priority.MEDIA : prio);
        t.setEstimateH(estimateH);
        t.setDueDate(dueDate);
        return taskDao.insert(t);
    }

    public List<Task> listarPorProjeto(Long projectId) {
        projectDao.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Projeto inexistente"));
        return taskDao.listByProject(projectId);
    }

    public void moverStatus(Long taskId, TaskStatus novoStatus, Double addSpentHours) {
        if (novoStatus == TaskStatus.CONCLUIDO && (addSpentHours == null || addSpentHours < 0))
            throw new IllegalArgumentException("Para concluir, informe horas >= 0");
        taskDao.updateStatus(taskId, novoStatus, addSpentHours == null ? 0.0 : addSpentHours);
    }

    public void reatribuir(Long taskId, Long assigneeId) {
        // valida existencia e participação
        var t = taskDao.findById(taskId).orElseThrow(() -> new IllegalArgumentException("Tarefa inexistente"));
        userDao.findById(assigneeId).orElseThrow(() -> new IllegalArgumentException("Usuário inexistente"));
        if (!isUserInProject(t.getProjectId(), assigneeId))
            throw new IllegalArgumentException("Usuário não está alocado ao projeto (via equipe)");
        taskDao.reassign(taskId, assigneeId);
    }

    private boolean isUserInProject(Long projectId, Long userId) {
        String sql = "SELECT 1 FROM project_members_v WHERE project_id = ? AND user_id = ? LIMIT 1";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao validar membro do projeto: " + e.getMessage(), e);
        }
    }
    public void atualizarTarefaBasica(Long id, String title, String desc, Priority prio, Double estimateH, LocalDate due) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Título é obrigatório");
        taskDao.updateBasics(id, title.trim(), desc, prio == null ? Priority.MEDIA : prio, estimateH, due);
    }

    public void excluirTarefa(Long id) {
        taskDao.delete(id);
    }

    public void moverStatus(Long taskId, TaskStatus novo, double horasASomar) {
        // ... soma horas se aplicável
        taskDao.atualizarStatus(taskId, novo);

    }
}
