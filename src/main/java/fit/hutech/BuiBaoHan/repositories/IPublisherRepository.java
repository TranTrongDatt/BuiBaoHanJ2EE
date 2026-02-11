package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Publisher;

/**
 * Repository cho Publisher entity
 */
@Repository
public interface IPublisherRepository extends JpaRepository<Publisher, Long> {

    Optional<Publisher> findByName(String name);

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    List<Publisher> findAllByOrderByNameAsc();

    @Query("SELECT p FROM Publisher p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Publisher> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Publisher p LEFT JOIN FETCH p.books WHERE p.id = :id")
    Optional<Publisher> findByIdWithBooks(@Param("id") Long id);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.publisher.id = :publisherId")
    long countBooksByPublisherId(@Param("publisherId") Long publisherId);
}
