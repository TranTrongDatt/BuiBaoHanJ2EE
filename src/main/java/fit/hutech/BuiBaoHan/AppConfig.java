package fit.hutech.BuiBaoHan;

import fit.hutech.BuiBaoHan.entities.Book;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public List<Book> getBooks() {
        var books = new ArrayList<Book>();
        books.add(new Book(1L, "Lập trình Web Spring Framework", "Hana Nguyễn", 29.99, "Công nghệ thông tin"));
        books.add(new Book(2L, "Lập trình ứng dụng Java", "Huy Cường", 45.63, "Công nghệ thông tin"));
        books.add(new Book(3L, "Lập trình Web Spring Boot", "Xuân Nhân", 12.0, "Công nghệ thông tin"));
        books.add(new Book(4L, "Lập trình Web Spring MVC", "Ánh Nguyễn", 0.12, "Công nghệ thông tin"));
        return books;
    }
}