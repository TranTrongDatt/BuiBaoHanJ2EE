package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Field;

/**
 * Repository cho Field entity
 */
@Repository
public interface IFieldRepository extends JpaRepository<Field, Long> {

    Optional<Field> findByName(String name);

    Optional<Field> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    List<Field> findByIsActiveTrueOrderByDisplayOrderAsc();

    Page<Field> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT f FROM Field f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Field> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT f FROM Field f LEFT JOIN FETCH f.categories WHERE f.id = :id")
    Optional<Field> findByIdWithCategories(@Param("id") Long id);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.field.id = :fieldId")
    long countCategoriesByFieldId(@Param("fieldId") Long fieldId);
    
    /**
     * Find top 5 fields ordered by display order
     */
    List<Field> findTop5ByOrderByDisplayOrderAsc();
    
    /**
     * Find top 5 fields with book counts (optimized for dashboard chart)
     * Returns Object[] where [0] = field name, [1] = total book count across all categories
     */
    @Query("SELECT f.name, COUNT(b) FROM Field f LEFT JOIN f.categories c LEFT JOIN c.books b GROUP BY f.id, f.name ORDER BY f.displayOrder ASC LIMIT 5")
    List<Object[]> findTop5FieldsWithBookCount();
}