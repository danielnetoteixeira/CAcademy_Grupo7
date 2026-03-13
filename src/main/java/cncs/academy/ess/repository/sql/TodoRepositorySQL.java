package cncs.academy.ess.repository.sql;

import cncs.academy.ess.model.Todo;
import cncs.academy.ess.repository.TodoRepository;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL implementation of TodoRepository using PostgreSQL.
 * Handles all database operations for Todo entities.
 */
public class TodoRepositorySQL implements TodoRepository {
    private final BasicDataSource dataSource;

    /**
     * Constructor for TodoRepositorySQL with database connection pool.
     *
     * @param dataSource The connection pool for database access
     */
    public TodoRepositorySQL(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Find a todo by its unique ID.
     *
     * @param todoId The ID of the todo to find
     * @return The Todo object if found, null otherwise
     */
    @Override
    public Todo findById(int todoId) {
        String sql = "SELECT id, description, completed, list_id FROM todos WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, todoId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToTodo(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find Todo", e);
        }
        return null;
    }

    /**
     * Retrieve all todos from the database.
     *
     * @return A list of all Todo objects
     */
    @Override
    public List<Todo> findAll() {
        List<Todo> todos = new ArrayList<>();
        String sql = "SELECT id, description, completed, list_id FROM todos";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                todos.add(mapResultSetToTodo(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed all Todos", e);
        }
        return todos;
    }

    /**
     * Find all todos for a specific list.
     *
     * @param listId The ID of the list
     * @return A list of Todo objects belonging to the list
     */
    @Override
    public List<Todo> findAllByListId(int listId) {
        List<Todo> todos = new ArrayList<>();
        String sql = "SELECT id, description, completed, list_id FROM todos WHERE list_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                todos.add(mapResultSetToTodo(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed all Todos by list ID", e);
        }
        return todos;
    }

    /**
     * Save a new todo to the database.
     * Returns the ID of the saved todo.
     *
     * @param todo The Todo object to save
     * @return The ID of the saved todo
     */
    @Override
    public int save(Todo todo) {
        String sql = "INSERT INTO todos (description, completed, list_id) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, todo.getDescription());
            stmt.setBoolean(2, todo.isCompleted());
            stmt.setInt(3, todo.getListId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save Todo", e);
        }
        return -1;
    }

    /**
     * Update an existing todo in the database.
     *
     * @param todo The Todo object with updated values
     */
    @Override
    public void update(Todo todo) {
        String sql = "UPDATE todos SET description = ?, completed = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, todo.getDescription());
            stmt.setBoolean(2, todo.isCompleted());
            stmt.setInt(3, todo.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update Todo", e);
        }
    }

    /**
     * Delete a todo by its ID.
     *
     * @param todoId The ID of the todo to delete
     * @return true if the delete was successful, false otherwise
     */
    @Override
    public boolean deleteById(int todoId) {
        String sql = "DELETE FROM todos WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, todoId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Helper method to map ResultSet rows to Todo objects.
     *
     * @param rs The ResultSet row to map
     * @return A Todo object with data from the ResultSet
     * @throws SQLException If database access fails
     */
    private Todo mapResultSetToTodo(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String description = rs.getString("description");
        boolean completed = rs.getBoolean("completed");
        int listId = rs.getInt("list_id");
        return new Todo(id, description, completed, listId);
    }
}
