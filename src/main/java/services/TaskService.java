package services;

import entities.Task;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class TaskService {
    private static final String INSERT_SQL =
            "INSERT INTO task (titre, description, resume, date_debut, date_fin, priorite, statut, type, localisation, parcelle_id, culture_id, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE task SET titre = ?, description = ?, resume = ?, date_debut = ?, date_fin = ?, priorite = ?, statut = ?, type = ?, localisation = ?, parcelle_id = ?, culture_id = ?, created_by = ? " +
            "WHERE id_task = ?";
    private static final String DELETE_SQL = "DELETE FROM task WHERE id_task = ?";
    private static final String SELECT_ALL_SQL =
            "SELECT id_task, titre, description, resume, date_debut, date_fin, priorite, statut, type, localisation, parcelle_id, culture_id, created_by FROM task ORDER BY id_task DESC";

    private final Connection connection = MyConnection.getInstance().getConn();

    public void add(Task task) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {
            bindTask(preparedStatement, task, false);
            preparedStatement.executeUpdate();
        }
    }

    public void update(Task task) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL)) {
            bindTask(preparedStatement, task, true);
            preparedStatement.executeUpdate();
        }
    }

    public void delete(int idTask) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL)) {
            preparedStatement.setInt(1, idTask);
            preparedStatement.executeUpdate();
        }
    }

    public List<Task> getAll() throws SQLException {
        List<Task> tasks = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                tasks.add(mapResultSet(resultSet));
            }
        }
        return tasks;
    }

    private void bindTask(PreparedStatement preparedStatement, Task task, boolean includeId) throws SQLException {
        preparedStatement.setString(1, task.getTitre());
        preparedStatement.setString(2, task.getDescription());
        preparedStatement.setString(3, task.getResume());
        preparedStatement.setTimestamp(4, toTimestamp(task.getDateDebut()));
        preparedStatement.setTimestamp(5, toTimestamp(task.getDateFin()));
        preparedStatement.setString(6, task.getPriorite());
        preparedStatement.setString(7, task.getStatut());
        preparedStatement.setString(8, task.getType());
        preparedStatement.setString(9, task.getLocalisation());
        setNullableInt(preparedStatement, 10, task.getParcelleId());
        setNullableInt(preparedStatement, 11, task.getCultureId());
        setNullableInt(preparedStatement, 12, task.getCreatedBy());

        if (includeId) {
            preparedStatement.setInt(13, task.getIdTask());
        }
    }

    private Task mapResultSet(ResultSet resultSet) throws SQLException {
        Timestamp dateDebut = resultSet.getTimestamp("date_debut");
        Timestamp dateFin = resultSet.getTimestamp("date_fin");

        return new Task(
                resultSet.getInt("id_task"),
                resultSet.getString("titre"),
                resultSet.getString("description"),
                resultSet.getString("resume"),
                dateDebut == null ? null : dateDebut.toLocalDateTime(),
                dateFin == null ? null : dateFin.toLocalDateTime(),
                resultSet.getString("priorite"),
                resultSet.getString("statut"),
                resultSet.getString("type"),
                resultSet.getString("localisation"),
                getNullableInt(resultSet, "parcelle_id"),
                getNullableInt(resultSet, "culture_id"),
                getNullableInt(resultSet, "created_by")
        );
    }

    private void setNullableInt(PreparedStatement preparedStatement, int index, Integer value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.INTEGER);
        } else {
            preparedStatement.setInt(index, value);
        }
    }

    private Integer getNullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
