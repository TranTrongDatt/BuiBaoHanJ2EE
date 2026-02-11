package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Author;

/**
 * Repository cho Author entity
 */
@Repository
public interface IAuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByName(String name);

    boolean existsByName(String name);

    List<Author> findByIsActiveTrueOrderByNameAsc();

    Page<Author> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT a FROM Author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.nationality) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Author> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT a FROM Author a WHERE a.nationality = :nationality AND a.isActive = true")
    List<Author> findByNationality(@Param("nationality") String nationality);

    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id")
    Optional<Author> findByIdWithBooks(@Param("id") Long id);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.author.id = :authorId")
    long countBooksByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT DISTINCT a.nationality FROM Author a WHERE a.nationality IS NOT NULL ORDER BY a.nationality")
    List<String> findAllNationalities();
}
