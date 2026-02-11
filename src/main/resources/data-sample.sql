-- ============================================
-- MiniVerse Sample Data - Dữ liệu mẫu cho AI Chatbot
-- Bao gồm: 4 Lĩnh vực, 7 Danh mục, 6 Tác giả, 12 Sách
-- Chạy script sau khi ứng dụng đã khởi động lần đầu
-- ============================================

-- Đảm bảo không trùng dữ liệu khi chạy lại
SET @now = NOW();

-- ============================================
-- 1. THÊM TÁC GIẢ (AUTHORS)
-- ============================================
INSERT INTO author (name, biography, nationality, is_active, created_at, updated_at)
SELECT 'Bộ Giáo dục và Đào tạo', 'Bộ Giáo dục và Đào tạo Việt Nam - đơn vị biên soạn sách giáo khoa chính thức', 'Việt Nam', TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM author WHERE name = 'Bộ Giáo dục và Đào tạo');

INSERT INTO author (name, biography, nationality, is_active, created_at, updated_at)
SELECT 'Fujiko F. Fujio', 'Fujimoto Hiroshi - họa sĩ manga Nhật Bản nổi tiếng, cha đẻ của Doraemon', 'Nhật Bản', TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM author WHERE name = 'Fujiko F. Fujio');

INSERT INTO author (name, biography, nationality, is_active, created_at, updated_at)
SELECT 'Nguyễn Văn Hiệp', 'Giáo sư, chuyên gia về An toàn thông tin tại Việt Nam', 'Việt Nam', TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM author WHERE name = 'Nguyễn Văn Hiệp');

INSERT INTO author (name, biography, nationality, is_active, created_at, updated_at)
SELECT 'Phạm Hữu Khang', 'Tác giả chuyên viết sách về lập trình và công nghệ thông tin', 'Việt Nam', TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM author WHERE name = 'Phạm Hữu Khang');

INSERT INTO author (name, biography, nationality, is_active, created_at, updated_at)
SELECT 'Nguyễn Dzoãn Cẩm Vân', 'Nghệ nhân ẩm thực, tác giả nổi tiếng về sách nấu ăn Việt Nam', 'Việt Nam', TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM author WHERE name = 'Nguyễn Dzoãn Cẩm Vân');

INSERT INTO author (name, biography, nationality, is_active, created_at, updated_at)
SELECT 'Nguyễn Nhật Ánh', 'Nhà văn nổi tiếng Việt Nam, tác giả nhiều tác phẩm văn học cho thiếu nhi và thanh niên', 'Việt Nam', TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM author WHERE name = 'Nguyễn Nhật Ánh');

-- ============================================
-- 2. THÊM LĨNH VỰC (FIELDS) - 4 lĩnh vực
-- ============================================
INSERT INTO field (name, slug, description, display_order, is_active, created_at, updated_at)
SELECT 'Sách Giáo Khoa', 'sach-giao-khoa', 'Sách giáo khoa các cấp từ tiểu học đến đại học, phục vụ học tập chính quy', 1, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM field WHERE slug = 'sach-giao-khoa');

INSERT INTO field (name, slug, description, display_order, is_active, created_at, updated_at)
SELECT 'Tiểu Thuyết', 'tieu-thuyet', 'Tiểu thuyết văn học trong nước và quốc tế, nhiều thể loại phong phú', 2, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM field WHERE slug = 'tieu-thuyet');

INSERT INTO field (name, slug, description, display_order, is_active, created_at, updated_at)
SELECT 'Truyện Tranh', 'truyen-tranh', 'Truyện tranh manga, comic trong nước và quốc tế dành cho mọi lứa tuổi', 3, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM field WHERE slug = 'truyen-tranh');

INSERT INTO field (name, slug, description, display_order, is_active, created_at, updated_at)
SELECT 'Sách Dạy Nấu Ăn', 'sach-day-nau-an', 'Sách hướng dẫn nấu ăn, công thức món ăn Việt Nam và quốc tế', 4, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM field WHERE slug = 'sach-day-nau-an');

-- ============================================
-- 3. THÊM DANH MỤC (CATEGORIES) - 7 danh mục
-- ============================================

-- Lấy ID của lĩnh vực
SET @field_sgk = (SELECT id FROM field WHERE slug = 'sach-giao-khoa' LIMIT 1);
SET @field_tt = (SELECT id FROM field WHERE slug = 'truyen-tranh' LIMIT 1);
SET @field_tth = (SELECT id FROM field WHERE slug = 'tieu-thuyet' LIMIT 1);
SET @field_na = (SELECT id FROM field WHERE slug = 'sach-day-nau-an' LIMIT 1);

-- Cấp 1
INSERT INTO category (name, slug, description, field_id, display_order, is_active, created_at, updated_at)
SELECT 'Cấp 1', 'cap-1', 'Sách giáo khoa dành cho học sinh tiểu học từ lớp 1 đến lớp 5', @field_sgk, 1, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM category WHERE slug = 'cap-1');

-- Cấp 2
INSERT INTO category (name, slug, description, field_id, display_order, is_active, created_at, updated_at)
SELECT 'Cấp 2', 'cap-2', 'Sách giáo khoa dành cho học sinh trung học cơ sở từ lớp 6 đến lớp 9', @field_sgk, 2, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM category WHERE slug = 'cap-2');

-- Cấp 3
INSERT INTO category (name, slug, description, field_id, display_order, is_active, created_at, updated_at)
SELECT 'Cấp 3', 'cap-3', 'Sách giáo khoa dành cho học sinh trung học phổ thông từ lớp 10 đến lớp 12', @field_sgk, 3, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM category WHERE slug = 'cap-3');

-- IT
INSERT INTO category (name, slug, description, field_id, display_order, is_active, created_at, updated_at)
SELECT 'IT - Công Nghệ Thông Tin', 'it-cong-nghe-thong-tin', 'Sách về lập trình, an toàn thông tin, phát triển phần mềm và công nghệ', @field_sgk, 4, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM category WHERE slug = 'it-cong-nghe-thong-tin');

-- Truyện tranh có hình
INSERT INTO category (name, slug, description, field_id, display_order, is_active, created_at, updated_at)
SELECT 'Truyện Tranh Có Hình', 'truyen-tranh-co-hinh', 'Truyện tranh màu, manga, comic có nhiều hình minh họa sinh động', @field_tt, 1, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM category WHERE slug = 'truyen-tranh-co-hinh');

-- Văn học Việt Nam
INSERT INTO category (name, slug, description, field_id, display_order, is_active, created_at, updated_at)
SELECT 'Văn Học Việt Nam', 'van-hoc-viet-nam', 'Tiểu thuyết, truyện ngắn của các tác giả Việt Nam', @field_tth, 1, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM category WHERE slug = 'van-hoc-viet-nam');

-- Ẩm thực Việt Nam
INSERT INTO category (name, slug, description, field_id, display_order, is_active, created_at, updated_at)
SELECT 'Ẩm Thực Việt Nam', 'am-thuc-viet-nam', 'Công thức nấu ăn các món ăn truyền thống Việt Nam', @field_na, 1, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM category WHERE slug = 'am-thuc-viet-nam');

-- ============================================
-- 4. THÊM SÁCH (BOOKS) - 12 sản phẩm
-- ============================================

-- Lấy ID các danh mục và tác giả
SET @cat_cap1 = (SELECT id FROM category WHERE slug = 'cap-1' LIMIT 1);
SET @cat_cap2 = (SELECT id FROM category WHERE slug = 'cap-2' LIMIT 1);
SET @cat_cap3 = (SELECT id FROM category WHERE slug = 'cap-3' LIMIT 1);
SET @cat_it = (SELECT id FROM category WHERE slug = 'it-cong-nghe-thong-tin' LIMIT 1);
SET @cat_tt = (SELECT id FROM category WHERE slug = 'truyen-tranh-co-hinh' LIMIT 1);
SET @cat_vhvn = (SELECT id FROM category WHERE slug = 'van-hoc-viet-nam' LIMIT 1);
SET @cat_atvn = (SELECT id FROM category WHERE slug = 'am-thuc-viet-nam' LIMIT 1);

SET @author_bgd = (SELECT id FROM author WHERE name = 'Bộ Giáo dục và Đào tạo' LIMIT 1);
SET @author_fujiko = (SELECT id FROM author WHERE name = 'Fujiko F. Fujio' LIMIT 1);
SET @author_hiep = (SELECT id FROM author WHERE name = 'Nguyễn Văn Hiệp' LIMIT 1);
SET @author_khang = (SELECT id FROM author WHERE name = 'Phạm Hữu Khang' LIMIT 1);
SET @author_camvan = (SELECT id FROM author WHERE name = 'Nguyễn Dzoãn Cẩm Vân' LIMIT 1);
SET @author_nna = (SELECT id FROM author WHERE name = 'Nguyễn Nhật Ánh' LIMIT 1);

-- =============================================
-- XÓA CỘT 'author' DƯ THỪA (đã có author_id FK)
-- =============================================
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book' AND COLUMN_NAME = 'author');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE book DROP COLUMN author', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1. Sách Tiếng Việt lớp 1 Tập 1 (Cấp 1)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Tiếng Việt Lớp 1 - Tập 1', 'tieng-viet-lop-1-tap-1', 'ISBN-TV1-001', 
'Sách giáo khoa Tiếng Việt lớp 1 tập 1 theo chương trình giáo dục phổ thông mới. Giúp học sinh làm quen với chữ cái, tập đọc và viết những âm, vần cơ bản.',
35000, 40000, 120, 100, 80, 20, @cat_cap1, @author_bgd, 'Tiếng Việt', 'AVAILABLE', 150, 50, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'tieng-viet-lop-1-tap-1');

-- 2. Sách Tiếng Việt lớp 1 Tập 2 (Cấp 1)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Tiếng Việt Lớp 1 - Tập 2', 'tieng-viet-lop-1-tap-2', 'ISBN-TV1-002', 
'Sách giáo khoa Tiếng Việt lớp 1 tập 2. Tiếp nối tập 1, học sinh sẽ học các vần phức tạp hơn và bắt đầu đọc các bài văn ngắn.',
35000, 40000, 128, 95, 75, 18, @cat_cap1, @author_bgd, 'Tiếng Việt', 'AVAILABLE', 140, 48, FALSE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'tieng-viet-lop-1-tap-2');

-- 3. Sách Toán Số Học (Cấp 2)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Toán Số Học Lớp 6', 'toan-so-hoc-lop-6', 'ISBN-TSH6-001', 
'Sách giáo khoa Toán số học lớp 6 bao gồm các kiến thức về số nguyên, phân số, số thập phân và các phép tính cơ bản. Phù hợp cho học sinh THCS.',
45000, 55000, 180, 80, 60, 15, @cat_cap2, @author_bgd, 'Tiếng Việt', 'AVAILABLE', 200, 80, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'toan-so-hoc-lop-6');

-- 4. Sách Toán Hình Học (Cấp 2)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Toán Hình Học Lớp 7', 'toan-hinh-hoc-lop-7', 'ISBN-THH7-001', 
'Sách giáo khoa Toán hình học lớp 7 với các kiến thức về tam giác, tứ giác, đường tròn và các định lý hình học cơ bản.',
48000, 58000, 160, 70, 55, 12, @cat_cap2, @author_bgd, 'Tiếng Việt', 'AVAILABLE', 180, 65, FALSE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'toan-hinh-hoc-lop-7');

-- 5. Sách Toán Lớp 10 (Cấp 3)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Toán Lớp 10 - Đại Số và Giải Tích', 'toan-lop-10-dai-so-giai-tich', 'ISBN-T10-001', 
'Sách giáo khoa Toán lớp 10 phần Đại số và Giải tích. Bao gồm hàm số, phương trình, bất phương trình và các bài toán ứng dụng.',
55000, 65000, 220, 90, 70, 18, @cat_cap3, @author_bgd, 'Tiếng Việt', 'AVAILABLE', 280, 95, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'toan-lop-10-dai-so-giai-tich');

-- 6. Sách Lập Trình Web (IT)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Lập Trình Web Toàn Tập', 'lap-trinh-web-toan-tap', 'ISBN-LTW-001', 
'Sách hướng dẫn lập trình web từ cơ bản đến nâng cao. Bao gồm HTML, CSS, JavaScript, React, Node.js và các công nghệ web hiện đại. Phù hợp cho sinh viên CNTT và người mới bắt đầu.',
185000, 220000, 450, 50, 40, 10, @cat_it, @author_khang, 'Tiếng Việt', 'AVAILABLE', 500, 120, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'lap-trinh-web-toan-tap');

-- 7. Sách An Toàn Thông Tin Nâng Cao (IT)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'An Toàn Thông Tin Nâng Cao', 'an-toan-thong-tin-nang-cao', 'ISBN-ATTT-001', 
'Sách chuyên sâu về An toàn thông tin, bao gồm mã hóa, bảo mật mạng, phát hiện xâm nhập, kỹ thuật hack ethical và phòng chống tấn công mạng. Dành cho sinh viên và chuyên gia CNTT.',
250000, 300000, 520, 40, 30, 8, @cat_it, @author_hiep, 'Tiếng Việt', 'AVAILABLE', 350, 85, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'an-toan-thong-tin-nang-cao');

-- 8. Sách Java cơ bản (IT)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Java Từ Cơ Bản Đến Nâng Cao', 'java-tu-co-ban-den-nang-cao', 'ISBN-JAVA-001', 
'Sách học lập trình Java từ những khái niệm cơ bản đến OOP, Collections, Multithreading và Spring Framework. Có nhiều bài tập thực hành và project mẫu.',
195000, 230000, 480, 45, 35, 10, @cat_it, @author_khang, 'Tiếng Việt', 'AVAILABLE', 380, 110, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'java-tu-co-ban-den-nang-cao');

-- 9. Truyện Doraemon Tập 1 (Truyện tranh có hình)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Doraemon - Tập 1: Chú Mèo Máy Đến Từ Tương Lai', 'doraemon-tap-1', 'ISBN-DRM-001', 
'Tập đầu tiên của series Doraemon huyền thoại. Kể về Doraemon - chú mèo máy từ thế kỷ 22 được cử về quá khứ để giúp đỡ Nobita. Truyện tranh màu sinh động, phù hợp mọi lứa tuổi.',
25000, 30000, 180, 200, 150, 40, @cat_tt, @author_fujiko, 'Tiếng Việt', 'AVAILABLE', 1000, 500, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'doraemon-tap-1');

-- 10. Truyện Doraemon Tập 2 (Truyện tranh có hình)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Doraemon - Tập 2: Những Bảo Bối Thần Kỳ', 'doraemon-tap-2', 'ISBN-DRM-002', 
'Tiếp nối series Doraemon với những câu chuyện hấp dẫn về các bảo bối thần kỳ từ túi không gian bốn chiều của Doraemon.',
25000, 30000, 180, 180, 140, 35, @cat_tt, @author_fujiko, 'Tiếng Việt', 'AVAILABLE', 850, 420, FALSE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'doraemon-tap-2');

-- 11. Mắt Biếc (Văn học Việt Nam)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT 'Mắt Biếc', 'mat-biec', 'ISBN-MB-001', 
'Tiểu thuyết lãng mạn của nhà văn Nguyễn Nhật Ánh. Câu chuyện tình yêu trong sáng của Ngạn dành cho Hà Lan - cô gái có đôi mắt biếc. Tác phẩm đã được chuyển thể thành phim điện ảnh nổi tiếng.',
95000, 115000, 300, 120, 95, 25, @cat_vhvn, @author_nna, 'Tiếng Việt', 'AVAILABLE', 2000, 800, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = 'mat-biec');

-- 12. 100 Món Ăn Việt Nam (Ẩm thực)
INSERT INTO book (title, slug, isbn, description, price, original_price, page_count, total_quantity, stock_quantity, library_stock, category_id, author_id, language, status, view_count, sold_count, is_featured, created_at, updated_at)
SELECT '100 Món Ăn Việt Nam Truyền Thống', '100-mon-an-viet-nam-truyen-thong', 'ISBN-NAV-001', 
'Tổng hợp 100 công thức nấu các món ăn truyền thống Việt Nam từ Bắc vào Nam. Hướng dẫn chi tiết từng bước với hình ảnh minh họa sống động. Phù hợp cho người mới học nấu ăn.',
145000, 175000, 280, 60, 48, 12, @cat_atvn, @author_camvan, 'Tiếng Việt', 'AVAILABLE', 450, 180, TRUE, @now, @now
FROM dual WHERE NOT EXISTS (SELECT 1 FROM book WHERE slug = '100-mon-an-viet-nam-truyen-thong');

-- ============================================
-- HOÀN THÀNH THÊM DỮ LIỆU MẪU
-- ============================================
SELECT 'Đã thêm dữ liệu mẫu thành công!' AS message;
SELECT CONCAT('- Tổng số Author: ', COUNT(*)) AS info FROM author;
SELECT CONCAT('- Tổng số Field: ', COUNT(*)) AS info FROM field;
SELECT CONCAT('- Tổng số Category: ', COUNT(*)) AS info FROM category;
SELECT CONCAT('- Tổng số Book: ', COUNT(*)) AS info FROM book;
