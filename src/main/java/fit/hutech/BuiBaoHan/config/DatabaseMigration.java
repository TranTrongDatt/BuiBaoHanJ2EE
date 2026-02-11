package fit.hutech.BuiBaoHan.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Database migration: xóa các cột dư thừa trong bảng book
 * Chạy 1 lần khi khởi động app, tự kiểm tra trước khi DROP
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DatabaseMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        dropRedundantAuthorColumn();
    }

    /**
     * Xóa cột 'author' (VARCHAR) dư thừa trong bảng book.
     * Bảng book đã có author_id (FK → bảng author), cột 'author' text là dư thừa
     * và gây lỗi "Field 'author' doesn't have a default value" khi INSERT.
     */
    private void dropRedundantAuthorColumn() {
        try {
            String dbName = jdbcTemplate.queryForObject(
                "SELECT DATABASE()", String.class);
            
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'book' AND COLUMN_NAME = 'author'",
                Integer.class, dbName);

            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE book DROP COLUMN author");
                log.info("Migration: Đã xóa cột 'author' dư thừa khỏi bảng book");
            } else {
                log.debug("Migration: Cột 'author' không tồn tại trong bảng book, bỏ qua");
            }
        } catch (Exception e) {
            log.warn("Migration: Không thể xóa cột 'author': {}", e.getMessage());
        }
    }
}
