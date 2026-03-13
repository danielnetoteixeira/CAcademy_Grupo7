package cncs.academy.ess.service;

import org.apache.commons.dbcp2.BasicDataSource;

public class DatabaseSetupService {

    // Garante que todas as tabelas existem, criando-as se necessário
    public static void createAllTables(BasicDataSource ds) {
        String dropTodos = "DROP TABLE IF EXISTS todos CASCADE;";
        String dropLists = "DROP TABLE IF EXISTS lists CASCADE;";
        String dropUsers = "DROP TABLE IF EXISTS users CASCADE;";

        String usersTable = "CREATE TABLE users (" +
                "id SERIAL PRIMARY KEY," +
                "username VARCHAR(255) UNIQUE NOT NULL," +
                "password VARCHAR(255) NOT NULL,"+
                "salt VARCHAR(255) NOT NULL);";
        String listsTable = "CREATE TABLE lists (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL," +
                "owner_id INTEGER NOT NULL," +
                "FOREIGN KEY (owner_id) REFERENCES users(id));";
        String todosTable = "CREATE TABLE todos (" +
                "id SERIAL PRIMARY KEY," +
                "description VARCHAR(255) NOT NULL," +
                "completed BOOLEAN NOT NULL DEFAULT FALSE," +
                "list_id INTEGER NOT NULL," +
                "FOREIGN KEY (list_id) REFERENCES lists(id));";

        String[] queries = {dropTodos, dropLists, dropUsers, usersTable, listsTable, todosTable};
        for (String query : queries) {
            try (java.sql.Connection conn = ds.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute(query);
            } catch (Exception e) {
                // Ignorar se já existir
            }
        }
    }

    // Preenche a base de dados com dados de teste
    public static void fillDummyData(
            cncs.academy.ess.service.TodoUserService userService,
            cncs.academy.ess.service.TodoListsService toDoListService,
            cncs.academy.ess.service.TodoService todoService) throws Exception {
        userService.addUser("user1", "password1");
        userService.addUser("user2", "password2");
        userService.addUser("user3", "password3");
        toDoListService.createTodoListItem("Shopping list", 1);
        toDoListService.createTodoListItem("Other", 1);
        todoService.createTodoItem("Bread", 1);
        todoService.createTodoItem("Milk", 1);
        todoService.createTodoItem("Eggs", 1);
        todoService.createTodoItem("Cheese", 1);
        todoService.createTodoItem("Butter", 1);
    }
}
