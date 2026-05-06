package services;

import entities.TaskAssignment;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TaskAssignmentService {
    private final Connection conn = MyConnection.getInstance().getConn();

    public void add(TaskAssignment assignment) throws SQLException {
        String sql = "INSERT INTO task_assignment (task_id, worker_id, date_assignment, statut) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, assignment.getTaskId());
            stmt.setInt(2, assignment.getWorkerId());
            stmt.setTimestamp(3, Timestamp.valueOf(assignment.getDateAssignment()));
            stmt.setString(4, assignment.getStatut());
            stmt.executeUpdate();
        }
    }

    public void update(TaskAssignment assignment) throws SQLException {
        String sql = "UPDATE task_assignment SET task_id = ?, worker_id = ?, date_assignment = ?, statut = ? WHERE id_assignment = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, assignment.getTaskId());
            stmt.setInt(2, assignment.getWorkerId());
            stmt.setTimestamp(3, Timestamp.valueOf(assignment.getDateAssignment()));
            stmt.setString(4, assignment.getStatut());
            stmt.setInt(5, assignment.getIdAssignment());
            stmt.executeUpdate();
        }
    }

    public void delete(int idAssignment) throws SQLException {
        String sql = "DELETE FROM task_assignment WHERE id_assignment = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAssignment);
            stmt.executeUpdate();
        }
    }

    public List<TaskAssignment> getAll() throws SQLException {
        List<TaskAssignment> assignments = new ArrayList<>();
        String sql = "SELECT ta.id_assignment, ta.task_id, ta.worker_id, ta.date_assignment, ta.statut, t.titre, CONCAT(u.first_name, ' ', u.last_name) as worker_name " +
                     "FROM task_assignment ta " +
                     "JOIN task t ON ta.task_id = t.id_task " +
                     "JOIN users u ON ta.worker_id = u.id";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                TaskAssignment assignment = new TaskAssignment(
                        rs.getInt("id_assignment"),
                        rs.getInt("task_id"),
                        rs.getInt("worker_id"),
                        rs.getTimestamp("date_assignment").toLocalDateTime(),
                        rs.getString("statut"),
                        rs.getString("titre"),
                        rs.getString("worker_name")
                );
                assignments.add(assignment);
            }
        }
        return assignments;
    }

    public List<TaskAssignment> getByWorker(int workerId) throws SQLException {
        List<TaskAssignment> assignments = new ArrayList<>();
        String sql = "SELECT ta.id_assignment, ta.task_id, ta.worker_id, ta.date_assignment, ta.statut, t.titre, CONCAT(u.first_name, ' ', u.last_name) as worker_name " +
                     "FROM task_assignment ta " +
                     "JOIN task t ON ta.task_id = t.id_task " +
                     "JOIN users u ON ta.worker_id = u.id " +
                     "WHERE ta.worker_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TaskAssignment assignment = new TaskAssignment(
                            rs.getInt("id_assignment"),
                            rs.getInt("task_id"),
                            rs.getInt("worker_id"),
                            rs.getTimestamp("date_assignment").toLocalDateTime(),
                            rs.getString("statut"),
                            rs.getString("titre"),
                            rs.getString("worker_name")
                    );
                    assignments.add(assignment);
                }
            }
        }
        return assignments;
    }

    public void validateTask(int taskId) throws SQLException {
        String sql = "UPDATE task SET statut = 'termine' WHERE id_task = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.executeUpdate();
        }
    }

    public void refuseTask(int taskId) throws SQLException {
        String sql = "UPDATE task SET statut = 'en_cours' WHERE id_task = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.executeUpdate();
        }
    }
}





