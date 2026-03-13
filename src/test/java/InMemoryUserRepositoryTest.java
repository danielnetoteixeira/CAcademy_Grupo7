import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.memory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepositoryTest {
    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
    }

    @Test
    void saveAndFindById_ShouldReturnSavedUser() {
        // Arrange
        User user = new User("jane", "password", "salt");

        // Act
        int id = repository.save(user);
        User savedUser = repository.findById(id);

        // Assert
        assertEquals(user.getUsername(), savedUser.getUsername());
        assertEquals(user.getPassword(), savedUser.getPassword());
    }

    @Test
    void findAll_ShouldReturnList() {
        // Arrange
        User user = new User("jane", "password", "salt");
        repository.save(user);
        // Act
        List<User> users = repository.findAll();

        // Assert
        assertEquals(1, users.size());
    }

    @Test
    void deleteByIdRemovesUser() {
        int id = repository.save(new User("jane", "gina", "eva"));
        assertNotNull(repository.findById(id));

        repository.deleteById(id);
        assertNull(repository.findById(id));

        // No exception for missing ID
        assertDoesNotThrow(() -> repository.deleteById(12345));
    }

    @Test
    void saveAndFindByUsername_ShouldReturnSavedUser() {
        // Arrange
        User user = new User("jane", "password", "salt");

        // Act
        repository.save(user);
        User savedUser = repository.findByUsername("jane");

        // Assert
        assertEquals(user.getUsername(), savedUser.getUsername());
        assertEquals(user.getPassword(), savedUser.getPassword());
    }
}