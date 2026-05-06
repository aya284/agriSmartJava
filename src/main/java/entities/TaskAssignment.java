package entities;

import java.time.LocalDateTime;

public class TaskAssignment {
    private int idAssignment;
    private int taskId;
    private int workerId;
    private LocalDateTime dateAssignment;
    private String statut;
    private String taskTitre;
    private String workerName;

    // Empty constructor
    public TaskAssignment() {
    }

    // Constructor without id
    public TaskAssignment(int taskId, int workerId, LocalDateTime dateAssignment, String statut) {
        this.taskId = taskId;
        this.workerId = workerId;
        this.dateAssignment = dateAssignment;
        this.statut = statut != null ? statut : "assignee";
    }

    // Constructor with id
    public TaskAssignment(int idAssignment, int taskId, int workerId, LocalDateTime dateAssignment, String statut) {
        this.idAssignment = idAssignment;
        this.taskId = taskId;
        this.workerId = workerId;
        this.dateAssignment = dateAssignment;
        this.statut = statut != null ? statut : "assignee";
    }

    // Constructor with all fields
    public TaskAssignment(int idAssignment, int taskId, int workerId, LocalDateTime dateAssignment, String statut, String taskTitre, String workerName) {
        this.idAssignment = idAssignment;
        this.taskId = taskId;
        this.workerId = workerId;
        this.dateAssignment = dateAssignment;
        this.statut = statut;
        this.taskTitre = taskTitre;
        this.workerName = workerName;
    }

    // Getters and Setters
    public int getIdAssignment() {
        return idAssignment;
    }

    public void setIdAssignment(int idAssignment) {
        this.idAssignment = idAssignment;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getWorkerId() {
        return workerId;
    }

    public void setWorkerId(int workerId) {
        this.workerId = workerId;
    }

    public LocalDateTime getDateAssignment() {
        return dateAssignment;
    }

    public void setDateAssignment(LocalDateTime dateAssignment) {
        this.dateAssignment = dateAssignment;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getTaskTitre() {
        return taskTitre;
    }

    public void setTaskTitre(String taskTitre) {
        this.taskTitre = taskTitre;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    @Override
    public String toString() {
        return "TaskAssignment{" +
                "idAssignment=" + idAssignment +
                ", taskId=" + taskId +
                ", workerId=" + workerId +
                ", dateAssignment=" + dateAssignment +
                ", statut='" + statut + '\'' +
                ", taskTitre='" + taskTitre + '\'' +
                ", workerName='" + workerName + '\'' +
                '}';
    }
}

