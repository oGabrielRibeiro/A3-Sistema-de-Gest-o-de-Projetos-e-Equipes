package br.com.gpro.model;

import java.time.LocalDate;

public class Task {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private Long assigneeId; // usuário responsável
    private Priority priority = Priority.MEDIA;
    private TaskStatus status = TaskStatus.FAZER;
    private Double estimateH;
    private Double spentH = 0.0;
    private LocalDate dueDate;
    private LocalDate createdAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Double getEstimateH() { return estimateH; }
    public void setEstimateH(Double estimateH) { this.estimateH = estimateH; }
    public Double getSpentH() { return spentH; }
    public void setSpentH(Double spentH) { this.spentH = spentH; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}
