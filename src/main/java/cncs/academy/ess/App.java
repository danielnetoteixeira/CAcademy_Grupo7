package cncs.academy.ess;

import cncs.academy.ess.repository.memory.InMemoryUserRepository;
import cncs.academy.ess.service.DatabaseSetupService;
import cncs.academy.ess.controller.AuthorizationMiddleware;
import cncs.academy.ess.controller.TodoController;
import cncs.academy.ess.controller.TodoListController;
import cncs.academy.ess.controller.UserController;
import cncs.academy.ess.repository.TodoListsRepository;
import cncs.academy.ess.repository.TodoRepository;
import cncs.academy.ess.repository.sql.SQLUserRepository;
import cncs.academy.ess.repository.sql.TodoListRepositorySQL;
import cncs.academy.ess.repository.sql.TodoRepositorySQL;
import cncs.academy.ess.service.TodoListsService;
import cncs.academy.ess.service.TodoUserService;
import cncs.academy.ess.service.TodoService;
import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import org.apache.commons.dbcp2.BasicDataSource;
import org.casbin.jcasbin.main.Enforcer;

import java.io.Console;
import java.security.NoSuchAlgorithmException;

public class App {
    public static void main(String[] args) throws Exception {
        String basePath = System.getProperty("user.dir");
        SslPlugin plugin = new SslPlugin(conf -> {
            conf.pemFromPath(basePath+"/cert/cert.pem", basePath+"/cert/key.pem", "12345");
        });

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
            config.registerPlugin(plugin);
        }).start(7100);

        // Inicializar rotas para gestão de utilizadores
        InMemoryUserRepository userInMemoryRepository = new InMemoryUserRepository();
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        String connectURI = String.format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s", "localhost", "5432", "postgres", "postgres", "12345");
        ds.setUrl(connectURI);

        SQLUserRepository userRepository = new SQLUserRepository(ds);
        TodoUserService userService = new TodoUserService(userRepository);
        UserController userController = new UserController(userService);

        TodoListsRepository listsRepository = new TodoListRepositorySQL(ds);
        TodoListsService toDoListService = new TodoListsService(listsRepository);
        TodoListController todoListController = new TodoListController(toDoListService);

        TodoRepository todoRepository = new TodoRepositorySQL(ds);
        TodoService todoService = new TodoService(todoRepository, listsRepository);
        TodoController todoController = new TodoController(todoService, toDoListService);

        Enforcer enforcer = new Enforcer(basePath + "/casbin/model.conf", basePath + "/casbin/policy.csv");

        AuthorizationMiddleware authMiddleware = new AuthorizationMiddleware(userRepository, enforcer);

        // CORS
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "*");
        });
        // Middleware de autorização
        app.before(authMiddleware::handle);

        // Gestão de utilizadores
        app.post("/user", userController::createUser);
        app.get("/user/{userId}", userController::getUser);
        app.delete("/user/{userId}", userController::deleteUser);
        app.post("/login", userController::loginUser);

        // "To do" lists management
        /* POST /todolist
          {
              "listName": "Shopping list"
          }
         */
        app.post("/todolist", todoListController::createTodoList);
        app.get("/todolist", todoListController::getAllTodoLists);
        app.get("/todolist/{listId}", todoListController::getTodoList);
        app.get("/todolist/{listId}/share", todoListController::shareTodoList);


        // "To do" list items management
        /* POST /todo/item
          {
              "description": "Buy milk",
              "listId": 1
          }
         */
        app.post("/todo/item", todoController::createTodoItem);
        /* GET /todo/1/tasks */
        app.get("/todo/{listId}/tasks", todoController::getAllTodoItems);
        /* GET /todo/1/tasks/1 */
        app.get("/todo/{listId}/tasks/{taskId}", todoController::getTodoItem);
        /* DELETE /todo/1/tasks/1 */
        app.delete("/todo/{listId}/tasks/{taskId}", todoController::deleteTodoItem);

        DatabaseSetupService.createAllTables(ds);
        DatabaseSetupService.fillDummyData(userService, toDoListService, todoService);
    }


}
