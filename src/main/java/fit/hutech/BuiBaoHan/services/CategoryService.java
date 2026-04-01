package fit.hutech.BuiBaoHan.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import fit.hutech.BuiBaoHan.entities.Category;
import fit.hutech.BuiBaoHan.repositories.ICategoryRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Validated
@Transactional(isolation = Isolation.SERIALIZABLE,
        rollbackFor = {Exception.class, Throwable.class})
public class CategoryService {

    private final ICategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
    
    /**
     * Get category by ID with field and books eagerly loaded (for detail view)
     */
    public Optional<Category> getCategoryByIdWithDetails(Long id) {
        return categoryRepository.findByIdWithDetails(id);
    }

    public void addCategory(Category category) {
        categoryRepository.save(category);
    }

    public void updateCategory(Long id, @NotNull String name) {
        categoryRepository.findById(id).ifPresent(existingCategory -> {
            existingCategory.setName(name);
            categoryRepository.save(existingCategory);
        });
    }

    public void deleteCategoryById(Long id) {
        categoryRepository.deleteById(id);
    }
}
