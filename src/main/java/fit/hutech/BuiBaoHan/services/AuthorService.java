package fit.hutech.BuiBaoHan.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.dto.AuthorDto;
import fit.hutech.BuiBaoHan.entities.Author;
import fit.hutech.BuiBaoHan.repositories.IAuthorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Tác giả
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthorService {

    private final IAuthorRepository authorRepository;

    /**
     * Lấy tất cả tác giả (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<Author> getAllAuthors(Pageable pageable) {
        return authorRepository.findAll(pageable);
    }

    /**
     * Lấy tất cả tác giả active
     */
    @Transactional(readOnly = true)
    public List<Author> getActiveAuthors() {
        return authorRepository.findByIsActiveTrueOrderByNameAsc();
    }

    /**
     * Lấy tất cả tác giả (không phân trang)
     */
    @Transactional(readOnly = true)
    public List<Author> findAll() {
        return authorRepository.findAll();
    }

    /**
     * Tìm tác giả theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Author> getAuthorById(Long id) {
        return authorRepository.findById(id);
    }

    /**
     * Tìm tác giả theo tên
     */
    @Transactional(readOnly = true)
    public Optional<Author> getAuthorByName(String name) {
        return authorRepository.findByName(name);
    }

    /**
     * Tìm tác giả theo ID kèm sách
     */
    @Transactional(readOnly = true)
    public Optional<Author> getAuthorWithBooks(Long id) {
        return authorRepository.findByIdWithBooks(id);
    }

    /**
     * Tìm kiếm tác giả
     */
    @Transactional(readOnly = true)
    public Page<Author> searchAuthors(String keyword, Pageable pageable) {
        return authorRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Tìm tác giả theo quốc tịch
     */
    @Transactional(readOnly = true)
    public List<Author> getAuthorsByNationality(String nationality) {
        return authorRepository.findByNationality(nationality);
    }

    /**
     * Lấy danh sách tất cả quốc tịch
     */
    @Transactional(readOnly = true)
    public List<String> getAllNationalities() {
        return authorRepository.findAllNationalities();
    }

    /**
     * Tạo tác giả mới
     */
    public Author createAuthor(AuthorDto dto) {
        // Check duplicate name
        if (authorRepository.existsByName(dto.name())) {
            throw new IllegalArgumentException("Tác giả với tên '" + dto.name() + "' đã tồn tại");
        }

        Author author = dto.toEntity();
        Author saved = authorRepository.save(author);
        log.info("Created author: {}", saved.getName());
        return saved;
    }

    /**
     * Cập nhật tác giả
     */
    public Author updateAuthor(Long id, AuthorDto dto) {
        Author existing = authorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tác giả với ID: " + id));

        // Check duplicate name (exclude current)
        if (!existing.getName().equals(dto.name()) && authorRepository.existsByName(dto.name())) {
            throw new IllegalArgumentException("Tác giả với tên '" + dto.name() + "' đã tồn tại");
        }

        existing.setName(dto.name());
        existing.setBirthDate(dto.birthDate());
        existing.setDeathDate(dto.deathDate());
        existing.setBiography(dto.biography());
        existing.setAvatar(dto.avatar());
        existing.setNationality(dto.nationality());
        existing.setWebsite(dto.website());
        existing.setIsActive(dto.isActive() != null ? dto.isActive() : existing.getIsActive());

        Author updated = authorRepository.save(existing);
        log.info("Updated author: {}", updated.getId());
        return updated;
    }

    /**
     * Xóa tác giả
     */
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tác giả với ID: " + id));

        // Check if author has books
        long bookCount = authorRepository.countBooksByAuthorId(id);
        if (bookCount > 0) {
            throw new IllegalStateException("Không thể xóa tác giả đang có " + bookCount + " sách");
        }

        authorRepository.delete(author);
        log.info("Deleted author: {}", id);
    }

    /**
     * Đếm số sách của tác giả
     */
    @Transactional(readOnly = true)
    public long countBooks(Long authorId) {
        return authorRepository.countBooksByAuthorId(authorId);
    }

    /**
     * Kiểm tra tác giả tồn tại
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return authorRepository.existsById(id);
    }
}
