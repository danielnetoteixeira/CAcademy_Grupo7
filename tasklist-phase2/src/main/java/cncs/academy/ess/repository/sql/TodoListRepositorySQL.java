package cncs.academy.ess.repository.sql;

import cncs.academy.ess.model.TodoList;
import cncs.academy.ess.repository.TodoListsRepository;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL implementation of TodoListsRepository using PostgreSQL.
 * Handles all database operations for TodoList entities.
 */
public class TodoListRepositorySQL implements TodoListsRepository {
    private final BasicDataSource dataSource;

    /**
     * Constructor for TodoListRepositorySQL with database connection pool.
     *
     * @param dataSource The connection pool for database access
     */
    public TodoListRepositorySQL(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Find a todo list by its unique ID.
     *
     * @param listId The ID of the list to find
     * @return The TodoList object if found, null otherwise
     */
    @Override
    public TodoList findById(int listId) {
        String sql = "SELECT id, name, owner_id FROM lists WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToTodoList(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find list", e);
        }
        return null;
    }

    /**
     * Retrieve all todo lists from the database.
     *
     * @return A list of all TodoList objects
     */
    @Override
    public List<TodoList> findAll() {
        List<TodoList> lists = new ArrayList<>();
        String sql = "SELECT id, name, owner_id FROM lists";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lists.add(mapResultSetToTodoList(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list of all TodoList", e);
        }
        return lists;
    }

    /**
     * Find all todo lists owned by a specific user.
     *
     * @param userId The ID of the user
     * @return A list of TodoList objects owned by the user
     */
    @Override
    public List<TodoList> findAllByUserId(int userId) {
        List<TodoList> lists = new ArrayList<>();
        String sql = "SELECT id, name, owner_id FROM lists WHERE owner_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lists.add(mapResultSetToTodoList(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list of TodoList objects owned by the user", e);
        }
        return lists;
    }

    /**
     * Save a new todo list to the database.
     * Returns the ID of the saved list.
     *
     * @param todoList The TodoList object to save
     * @return The ID of the saved list
     */
    @Override
    public int save(TodoList todoList) {
        String sql = "INSERT INTO lists (name, owner_id) VALUES (?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, todoList.getName());
            stmt.setInt(2, todoList.getOwnerId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save list", e);
        }
        return -1;
    }

    /**
     * Update an existing todo list in the database.
     *
     * @param todoList The TodoList object with updated values
     */
    @Override
    public void update(TodoList todoList) {
        String sql = "UPDATE lists SET name = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, todoList.getName());
            stmt.setInt(2, todoList.getListId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update list", e);
        }
    }

    /**
     * Delete a todo list by its ID.
     *
     * @param listId The ID of the list to delete
     * @return true if the delete was successful, false otherwise
     */
    @Override
    public boolean deleteById(int listId) {
        String sql = "DELETE FROM lists WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Helper method to map ResultSet rows to TodoList objects.
     *
     * @param rs The ResultSet row to map
     * @return A TodoList object with data from the ResultSet
     * @throws SQLException If database access fails
     */
    private TodoList mapResultSetToTodoList(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        int ownerId = rs.getInt("owner_id");
        return new TodoList(id, name, ownerId);
    }
}
