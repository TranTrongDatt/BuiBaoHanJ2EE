package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Category;

@Repository
public interface ICategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    /**
     * Find category by ID with field and books eagerly loaded
     */
    @EntityGraph(attributePaths = {"field", "books"})
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdWithDetails(Long id);
    
    boolean existsByName(String name);
    
    /**
     * Find top 5 categories ordered by books count (descending)
     */
    @Query("SELECT c FROM Category c LEFT JOIN c.books b GROUP BY c ORDER BY COUNT(b) DESC LIMIT 5")
    List<Category> findTop5ByOrderByBooksCountDesc();
    
    /**
     * Find top 5 categories with book counts (optimized for dashboard chart)
     * Returns Object[] where [0] = category name, [1] = book count
     */
    @Query("SELECT c.name, COUNT(b) FROM Category c LEFT JOIN c.books b GROUP BY c.id, c.name ORDER BY COUNT(b) DESC LIMIT 5")
    List<Object[]> findTop5CategoriesWithBookCount();
}
