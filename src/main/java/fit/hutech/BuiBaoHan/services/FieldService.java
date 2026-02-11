package fit.hutech.BuiBaoHan.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.dto.FieldDto;
import fit.hutech.BuiBaoHan.entities.Field;
import fit.hutech.BuiBaoHan.repositories.IFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Lĩnh vực sách
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FieldService {

    private final IFieldRepository fieldRepository;
    private final SlugService slugService;

    /**
     * Lấy tất cả fields
     */
    @Transactional(readOnly = true)
    public List<Field> getAllFields() {
        return fieldRepository.findAll();
    }

    /**
     * Lấy tất cả fields (alias for getAllFields)
     */
    @Transactional(readOnly = true)
    public List<Field> findAll() {
        return getAllFields();
    }

    /**
     * Lấy fields đang active, sắp xếp theo displayOrder
     */
    @Transactional(readOnly = true)
    public List<Field> getActiveFields() {
        return fieldRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Lấy fields phân trang
     */
    @Transactional(readOnly = true)
    public Page<Field> getFields(Pageable pageable) {
        return fieldRepository.findAll(pageable);
    }

    /**
     * Tìm field theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Field> getFieldById(Long id) {
        return fieldRepository.findById(id);
    }

    /**
     * Tìm field theo slug
     */
    @Transactional(readOnly = true)
    public Optional<Field> getFieldBySlug(String slug) {
        return fieldRepository.findBySlug(slug);
    }

    /**
     * Tìm field theo ID kèm categories
     */
    @Transactional(readOnly = true)
    public Optional<Field> getFieldWithCategories(Long id) {
        return fieldRepository.findByIdWithCategories(id);
    }

    /**
     * Tìm kiếm fields theo tên
     */
    @Transactional(readOnly = true)
    public Page<Field> searchByName(String keyword, Pageable pageable) {
        return fieldRepository.searchByName(keyword, pageable);
    }

    /**
     * Tạo field mới
     */
    public Field createField(FieldDto dto) {
        // Check duplicate name
        if (fieldRepository.existsByName(dto.name())) {
            throw new IllegalArgumentException("Lĩnh vực với tên '" + dto.name() + "' đã tồn tại");
        }

        Field field = dto.toEntity();
        
        // Generate unique slug
        String slug = slugService.toUniqueSlug(dto.name(), fieldRepository::existsBySlug);
        field.setSlug(slug);

        Field saved = fieldRepository.save(field);
        log.info("Created field: {} with slug: {}", saved.getName(), saved.getSlug());
        return saved;
    }

    /**
     * Cập nhật field
     */
    public Field updateField(Long id, FieldDto dto) {
        Field existing = fieldRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lĩnh vực với ID: " + id));

        // Check duplicate name (exclude current)
        if (!existing.getName().equals(dto.name()) && fieldRepository.existsByName(dto.name())) {
            throw new IllegalArgumentException("Lĩnh vực với tên '" + dto.name() + "' đã tồn tại");
        }

        existing.setName(dto.name());
        existing.setImage(dto.image());
        existing.setDescription(dto.description());
        existing.setDisplayOrder(dto.displayOrder() != null ? dto.displayOrder() : existing.getDisplayOrder());
        existing.setIsActive(dto.isActive() != null ? dto.isActive() : existing.getIsActive());

        // Regenerate slug if name changed
        if (!existing.getSlug().equals(slugService.toSlug(dto.name()))) {
            String newSlug = slugService.toUniqueSlug(dto.name(), 
                    slug -> !slug.equals(existing.getSlug()) && fieldRepository.existsBySlug(slug));
            existing.setSlug(newSlug);
        }

        Field updated = fieldRepository.save(existing);
        log.info("Updated field: {}", updated.getId());
        return updated;
    }

    /**
     * Xóa field
     */
    public void deleteField(Long id) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lĩnh vực với ID: " + id));

        // Check if field has categories
        long categoryCount = fieldRepository.countCategoriesByFieldId(id);
        if (categoryCount > 0) {
            throw new IllegalStateException("Không thể xóa lĩnh vực đang có " + categoryCount + " danh mục");
        }

        fieldRepository.delete(field);
        log.info("Deleted field: {}", id);
    }

    /**
     * Đếm số categories của field
     */
    @Transactional(readOnly = true)
    public long countCategories(Long fieldId) {
        return fieldRepository.countCategoriesByFieldId(fieldId);
    }

    /**
     * Kiểm tra field tồn tại
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return fieldRepository.existsById(id);
    }

    // ==================== ApiFieldController Support Methods ====================

    /**
     * Find all fields with pagination (overload)
     */
    @Transactional(readOnly = true)
    public Page<Field> findAll(Pageable pageable) {
        return fieldRepository.findAll(pageable);
    }

    /**
     * Find field by ID (alias)
     */
    @Transactional(readOnly = true)
    public Optional<Field> findById(Long id) {
        return getFieldById(id);
    }

    /**
     * Find field by slug (alias)
     */
    @Transactional(readOnly = true)
    public Optional<Field> findBySlug(String slug) {
        return getFieldBySlug(slug);
    }

    /**
     * Search fields by keyword
     */
    @Transactional(readOnly = true)
    public List<Field> search(String keyword) {
        return fieldRepository.searchByName(keyword, Pageable.unpaged()).getContent();
    }

    /**
     * Create field from DTO (alias)
     */
    public Field create(FieldDto dto) {
        return createField(dto);
    }

    /**
     * Update field from DTO (alias)
     */
    public Field update(Long id, FieldDto dto) {
        return updateField(id, dto);
    }

    /**
     * Delete field by ID (alias)
     */
    public void delete(Long id) {
        deleteField(id);
    }
}
