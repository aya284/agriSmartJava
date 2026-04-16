package entities;

import java.time.LocalDateTime;

public class SuiviTache {
    private int idSuivi;
    private int taskId;
    private LocalDateTime date;
    private String rendement;
    private String problemes;
    private String solution;
    private String statut;
    private String taskTitre;

    // Empty constructor
    public SuiviTache() {
    }

    // Constructor without id (without statut)
    public SuiviTache(int taskId, LocalDateTime date, String rendement, String problemes, String solution) {
        this.taskId = taskId;
        this.date = date;
        this.rendement = rendement;
        this.problemes = problemes;
        this.solution = solution;
    }

    // Constructor without id (with statut)
    public SuiviTache(int taskId, LocalDateTime date, String rendement, String problemes, String solution, String statut) {
        this.taskId = taskId;
        this.date = date;
        this.rendement = rendement;
        this.problemes = problemes;
        this.solution = solution;
        this.statut = statut;
    }

    // Constructor with id
    public SuiviTache(int idSuivi, int taskId, LocalDateTime date, String rendement, String problemes, String solution, String statut) {
        this.idSuivi = idSuivi;
        this.taskId = taskId;
        this.date = date;
        this.rendement = rendement;
        this.problemes = problemes;
        this.solution = solution;
        this.statut = statut;
    }

    // Constructor with all fields
    public SuiviTache(int idSuivi, int taskId, LocalDateTime date, String rendement, String problemes, String solution, String statut, String taskTitre) {
        this.idSuivi = idSuivi;
        this.taskId = taskId;
        this.date = date;
        this.rendement = rendement;
        this.problemes = problemes;
        this.solution = solution;
        this.statut = statut;
        this.taskTitre = taskTitre;
    }

    // Getters and Setters
    public int getIdSuivi() {
        return idSuivi;
    }

    public void setIdSuivi(int idSuivi) {
        this.idSuivi = idSuivi;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getRendement() {
        return rendement;
    }

    public void setRendement(String rendement) {
        this.rendement = rendement;
    }

    public String getProblemes() {
        return problemes;
    }

    public void setProblemes(String problemes) {
        this.problemes = problemes;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
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

    @Override
    public String toString() {
        return "SuiviTache{" +
                "idSuivi=" + idSuivi +
                ", taskId=" + taskId +
                ", date=" + date +
                ", rendement='" + rendement + '\'' +
                ", problemes='" + problemes + '\'' +
                ", solution='" + solution + '\'' +
                ", statut='" + statut + '\'' +
                ", taskTitre='" + taskTitre + '\'' +
                '}';
    }
}


