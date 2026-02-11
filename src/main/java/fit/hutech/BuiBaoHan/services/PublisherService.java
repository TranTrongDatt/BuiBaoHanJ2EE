package fit.hutech.BuiBaoHan.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.dto.PublisherDto;
import fit.hutech.BuiBaoHan.entities.Publisher;
import fit.hutech.BuiBaoHan.repositories.IPublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Nhà xuất bản
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PublisherService {

    private final IPublisherRepository publisherRepository;

    /**
     * Lấy tất cả NXB
     */
    @Transactional(readOnly = true)
    public List<Publisher> getAllPublishers() {
        return publisherRepository.findAllByOrderByNameAsc();
    }

    /**
     * Lấy NXB phân trang
     */
    @Transactional(readOnly = true)
    public Page<Publisher> getPublishers(Pageable pageable) {
        return publisherRepository.findAll(pageable);
    }

    /**
     * Tìm NXB theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Publisher> getPublisherById(Long id) {
        return publisherRepository.findById(id);
    }

    /**
     * Tìm NXB theo tên
     */
    @Transactional(readOnly = true)
    public Optional<Publisher> getPublisherByName(String name) {
        return publisherRepository.findByName(name);
    }

    /**
     * Tìm NXB theo ID kèm sách
     */
    @Transactional(readOnly = true)
    public Optional<Publisher> getPublisherWithBooks(Long id) {
        return publisherRepository.findByIdWithBooks(id);
    }

    /**
     * Tìm kiếm NXB
     */
    @Transactional(readOnly = true)
    public Page<Publisher> searchPublishers(String keyword, Pageable pageable) {
        return publisherRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Tạo NXB mới
     */
    public Publisher createPublisher(PublisherDto dto) {
        // Check duplicate name
        if (publisherRepository.existsByName(dto.name())) {
            throw new IllegalArgumentException("NXB với tên '" + dto.name() + "' đã tồn tại");
        }

        // Check duplicate email
        if (dto.email() != null && publisherRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("NXB với email '" + dto.email() + "' đã tồn tại");
        }

        Publisher publisher = dto.toEntity();
        Publisher saved = publisherRepository.save(publisher);
        log.info("Created publisher: {}", saved.getName());
        return saved;
    }

    /**
     * Cập nhật NXB
     */
    public Publisher updatePublisher(Long id, PublisherDto dto) {
        Publisher existing = publisherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy NXB với ID: " + id));

        // Check duplicate name (exclude current)
        if (!existing.getName().equals(dto.name()) && publisherRepository.existsByName(dto.name())) {
            throw new IllegalArgumentException("NXB với tên '" + dto.name() + "' đã tồn tại");
        }

        // Check duplicate email (exclude current)
        if (dto.email() != null && !dto.email().equals(existing.getEmail()) 
                && publisherRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("NXB với email '" + dto.email() + "' đã tồn tại");
        }

        existing.setName(dto.name());
        existing.setEmail(dto.email());
        existing.setPhone(dto.phone());
        existing.setAddress(dto.address());
        existing.setLogo(dto.logo());
        existing.setWebsite(dto.website());
        existing.setDescription(dto.description());

        Publisher updated = publisherRepository.save(existing);
        log.info("Updated publisher: {}", updated.getId());
        return updated;
    }

    /**
     * Xóa NXB
     */
    public void deletePublisher(Long id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy NXB với ID: " + id));

        // Check if publisher has books
        long bookCount = publisherRepository.countBooksByPublisherId(id);
        if (bookCount > 0) {
            throw new IllegalStateException("Không thể xóa NXB đang có " + bookCount + " sách");
        }

        publisherRepository.delete(publisher);
        log.info("Deleted publisher: {}", id);
    }

    /**
     * Đếm số sách của NXB
     */
    @Transactional(readOnly = true)
    public long countBooks(Long publisherId) {
        return publisherRepository.countBooksByPublisherId(publisherId);
    }

    /**
     * Kiểm tra NXB tồn tại
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return publisherRepository.existsById(id);
    }

    /**
     * Lấy tất cả NXB (alias cho getAllPublishers)
     */
    @Transactional(readOnly = true)
    public List<Publisher> findAll() {
        return getAllPublishers();
    }
}
