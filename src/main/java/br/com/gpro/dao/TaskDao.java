package br.com.gpro.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.gpro.model.Priority;     
import br.com.gpro.model.Task;
import br.com.gpro.model.TaskStatus;

public interface TaskDao {
    Long insert(Task t);
    Optional<Task> findById(Long id);
    Map<String, Integer> distinctStatusPorProjeto(long projectId);
    List<Task> listByProject(Long projectId);
    List<br.com.gpro.model.Task> listarAbertasNoProjeto(long projectId);
    void updateStatus(Long taskId, TaskStatus status, Double addSpentHours);
    void reassign(Long taskId, Long assigneeId);
    void atualizarStatus(Long taskId, TaskStatus status);
    int countAbertasNoProjeto(long projectId);
    void updateBasics(Long taskId, String title, String desc,
                      Priority prio, Double estimateH, LocalDate dueDate); // <— ASSINATURA EXATA
    void delete(Long taskId);
}
