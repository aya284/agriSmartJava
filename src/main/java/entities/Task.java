package entities;

import java.time.LocalDateTime;

public class Task {
    private int idTask;
    private String titre;
    private String description;
    private String resume;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String priorite;
    private String statut;
    private String type;
    private String localisation;
    private Integer parcelleId;
    private Integer cultureId;
    private Integer createdBy;

    public Task() {
    }

    public Task(String titre, String description, String resume, LocalDateTime dateDebut, LocalDateTime dateFin,
                String priorite, String statut, String type, String localisation,
                Integer parcelleId, Integer cultureId, Integer createdBy) {
        this.titre = titre;
        this.description = description;
        this.resume = resume;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.priorite = priorite;
        this.statut = statut;
        this.type = type;
        this.localisation = localisation;
        this.parcelleId = parcelleId;
        this.cultureId = cultureId;
        this.createdBy = createdBy;
    }

    public Task(int idTask, String titre, String description, String resume, LocalDateTime dateDebut, LocalDateTime dateFin,
                String priorite, String statut, String type, String localisation,
                Integer parcelleId, Integer cultureId, Integer createdBy) {
        this(titre, description, resume, dateDebut, dateFin, priorite, statut, type, localisation, parcelleId, cultureId, createdBy);
        this.idTask = idTask;
    }

    public int getIdTask() {
        return idTask;
    }

    public void setIdTask(int idTask) {
        this.idTask = idTask;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public Integer getParcelleId() {
        return parcelleId;
    }

    public void setParcelleId(Integer parcelleId) {
        this.parcelleId = parcelleId;
    }

    public Integer getCultureId() {
        return cultureId;
    }

    public void setCultureId(Integer cultureId) {
        this.cultureId = cultureId;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "Task{" +
                "idTask=" + idTask +
                ", titre='" + titre + '\'' +
                ", statut='" + statut + '\'' +
                ", priorite='" + priorite + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
