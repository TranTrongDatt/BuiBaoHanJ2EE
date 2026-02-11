package fit.hutech.BuiBaoHan.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import fit.hutech.BuiBaoHan.constants.RoleType;
import fit.hutech.BuiBaoHan.entities.Author;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.Category;
import fit.hutech.BuiBaoHan.entities.Field;
import fit.hutech.BuiBaoHan.entities.Publisher;
import fit.hutech.BuiBaoHan.entities.Role;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IAuthorRepository;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.ICategoryRepository;
import fit.hutech.BuiBaoHan.repositories.IFieldRepository;
import fit.hutech.BuiBaoHan.repositories.IPublisherRepository;
import fit.hutech.BuiBaoHan.repositories.IRoleRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Data initializer for development and testing
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final IRoleRepository roleRepository;
    private final IUserRepository userRepository;
    private final ICategoryRepository categoryRepository;
    private final IFieldRepository fieldRepository;
    private final IAuthorRepository authorRepository;
    private final IPublisherRepository publisherRepository;
    private final IBookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init-data:false}")
    private boolean initData;

    @Bean
    @Profile({"dev", "development"})
    public CommandLineRunner initDevData() {
        return args -> {
            if (!initData) {
                log.info("Data initialization disabled");
                return;
            }
            
            log.info("Initializing development data...");
            
            initRoles();
            initUsers();
            initCategories();
            initFields();
            initAuthors();
            initPublishers();
            initBooks();
            
            log.info("Development data initialization completed");
        };
    }

    @Bean
    public CommandLineRunner initRolesAlways() {
        return args -> {
            initRoles();
        };
    }

    private void initRoles() {
        for (RoleType roleType : RoleType.values()) {
            String roleName = roleType.getRoleName();
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                role.setDescription(roleType.getDescription());
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }
    }

    private void initUsers() {
        // Admin user
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@miniverse.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("System Administrator");
            admin.setPhone("0900000000");
            admin.setIsActive(true);
            
            Set<Role> roles = new HashSet<>();
            roleRepository.findByName(RoleType.ADMIN.getRoleName()).ifPresent(roles::add);
            admin.setRoles(roles);
            
            userRepository.save(admin);
            log.info("Created admin user");
        }
        
        // Staff user
        if (userRepository.findByUsername("staff").isEmpty()) {
            User staff = new User();
            staff.setUsername("staff");
            staff.setEmail("staff@miniverse.com");
            staff.setPassword(passwordEncoder.encode("Staff@123"));
            staff.setFullName("Staff Member");
            staff.setPhone("0900000001");
            staff.setIsActive(true);
            
            Set<Role> roles = new HashSet<>();
            roleRepository.findByName(RoleType.STAFF.getRoleName()).ifPresent(roles::add);
            staff.setRoles(roles);
            
            userRepository.save(staff);
            log.info("Created staff user");
        }
        
        // Demo user
        if (userRepository.findByUsername("demo").isEmpty()) {
            User demo = new User();
            demo.setUsername("demo");
            demo.setEmail("demo@miniverse.com");
            demo.setPassword(passwordEncoder.encode("Demo@123"));
            demo.setFullName("Demo User");
            demo.setPhone("0900000002");
            demo.setIsActive(true);
            
            Set<Role> roles = new HashSet<>();
            roleRepository.findByName(RoleType.USER.getRoleName()).ifPresent(roles::add);
            demo.setRoles(roles);
            
            userRepository.save(demo);
            log.info("Created demo user");
        }
    }

    private void initCategories() {
        List<String> categoryNames = List.of(
                "Fiction", "Non-Fiction", "Science Fiction", "Fantasy",
                "Mystery", "Romance", "Horror", "Biography",
                "History", "Self-Help", "Science", "Technology",
                "Art", "Travel", "Cooking", "Children"
        );
        
        for (String name : categoryNames) {
            if (!categoryRepository.existsByName(name)) {
                Category category = new Category();
                category.setName(name);
                category.setDescription(name + " books");
                categoryRepository.save(category);
            }
        }
        log.info("Categories initialized: {} total", categoryRepository.count());
    }

    private void initFields() {
        List<String> fieldNames = List.of(
                "Literature", "Computer Science", "Mathematics", "Physics",
                "Chemistry", "Biology", "Psychology", "Philosophy",
                "Economics", "Business", "Law", "Medicine",
                "Engineering", "Architecture", "Music", "Sports"
        );
        
        for (String name : fieldNames) {
            if (fieldRepository.findByName(name).isEmpty()) {
                Field field = new Field();
                field.setName(name);
                field.setDescription(name + " field");
                fieldRepository.save(field);
            }
        }
        log.info("Fields initialized: {} total", fieldRepository.count());
    }

    private void initAuthors() {
        List<String[]> authors = List.of(
                new String[]{"J.K. Rowling", "British author, best known for Harry Potter series"},
                new String[]{"Stephen King", "American author of horror, supernatural fiction"},
                new String[]{"Agatha Christie", "English writer known for detective novels"},
                new String[]{"Dan Brown", "American author known for thriller novels"},
                new String[]{"Paulo Coelho", "Brazilian author known for The Alchemist"}
        );
        
        for (String[] authorData : authors) {
            if (authorRepository.findByName(authorData[0]).isEmpty()) {
                Author author = new Author();
                author.setName(authorData[0]);
                author.setBiography(authorData[1]);
                authorRepository.save(author);
            }
        }
        log.info("Authors initialized: {} total", authorRepository.count());
    }

    private void initPublishers() {
        List<String[]> publishers = List.of(
                new String[]{"Penguin Random House", "New York, USA", "https://www.penguinrandomhouse.com"},
                new String[]{"HarperCollins", "New York, USA", "https://www.harpercollins.com"},
                new String[]{"Simon & Schuster", "New York, USA", "https://www.simonandschuster.com"},
                new String[]{"Hachette", "Paris, France", "https://www.hachette.com"},
                new String[]{"NXB Kim Đồng", "Hanoi, Vietnam", "https://www.nxbkimdong.com.vn"}
        );
        
        for (String[] pubData : publishers) {
            if (publisherRepository.findByName(pubData[0]).isEmpty()) {
                Publisher publisher = new Publisher();
                publisher.setName(pubData[0]);
                publisher.setAddress(pubData[1]);
                publisher.setWebsite(pubData[2]);
                publisherRepository.save(publisher);
            }
        }
        log.info("Publishers initialized: {} total", publisherRepository.count());
    }

    private void initBooks() {
        if (bookRepository.count() > 0) {
            log.info("Books already initialized: {} total", bookRepository.count());
            return;
        }
        
        // Get references
        Category fiction = categoryRepository.findByName("Fiction").orElse(null);
        Author rowling = authorRepository.findByName("J.K. Rowling").orElse(null);
        Publisher penguin = publisherRepository.findByName("Penguin Random House").orElse(null);
        
        if (fiction != null && rowling != null && penguin != null) {
            Book book = new Book();
            book.setTitle("Sample Book");
            book.setIsbn("978-0-13-468599-1");
            book.setDescription("A sample book for testing");
            book.setPrice(new BigDecimal("199000"));
            book.setStock(100);
            book.setTotalQuantity(10);
            book.setLibraryStock(10);
            book.setPublishDate(LocalDate.of(2024, 1, 1));
            book.setPageCount(350);
            book.setCategory(fiction);
            book.setAuthor(rowling);
            book.setPublisher(penguin);
            bookRepository.save(book);
            
            log.info("Sample book created");
        }
        
        log.info("Books initialized: {} total", bookRepository.count());
    }
}
