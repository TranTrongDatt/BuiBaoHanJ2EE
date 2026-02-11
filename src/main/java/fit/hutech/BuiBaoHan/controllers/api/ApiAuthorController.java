package fit.hutech.BuiBaoHan.controllers.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.AuthorDto;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.entities.Author;
import fit.hutech.BuiBaoHan.services.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Author management
 */
@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class ApiAuthorController {

    private final AuthorService authorService;

    // ==================== Public Endpoints ====================

    /**
     * Get all authors (paginated)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllAuthors(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Author> authorsPage = authorService.getAllAuthors(pageable);
        List<AuthorDto> authorDtos = authorsPage.getContent().stream()
                .map(AuthorDto::from)
                .toList();
        
        PageResponse<AuthorDto> pageResponse = PageResponse.from(authorsPage, authorDtos);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Get all active authors (no pagination)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllActiveAuthors() {
        List<AuthorDto> authors = authorService.getActiveAuthors().stream()
                .map(AuthorDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(authors));
    }

    /**
     * Get author by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getAuthorById(@PathVariable Long id) {
        var optAuthor = authorService.getAuthorById(id);
        if (optAuthor.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(AuthorDto.from(optAuthor.get())));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Tác giả"));
    }

    /**
     * Get author with books
     */
    @GetMapping("/{id}/with-books")
    public ResponseEntity<ApiResponse<?>> getAuthorWithBooks(@PathVariable Long id) {
        var optAuthor = authorService.getAuthorWithBooks(id);
        if (optAuthor.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(AuthorDto.from(optAuthor.get())));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Tác giả"));
    }

    /**
     * Search authors by keyword
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchAuthors(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Author> authorsPage = authorService.searchAuthors(keyword, pageable);
        List<AuthorDto> authors = authorsPage.getContent().stream()
                .map(AuthorDto::from)
                .toList();
        PageResponse<AuthorDto> pageResponse = PageResponse.from(authorsPage, authors);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Get authors by nationality
     */
    @GetMapping("/nationality/{nationality}")
    public ResponseEntity<ApiResponse<?>> getAuthorsByNationality(@PathVariable String nationality) {
        List<AuthorDto> authors = authorService.getAuthorsByNationality(nationality).stream()
                .map(AuthorDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(authors));
    }

    /**
     * Get all nationalities
     */
    @GetMapping("/nationalities")
    public ResponseEntity<ApiResponse<?>> getAllNationalities() {
        List<String> nationalities = authorService.getAllNationalities();
        return ResponseEntity.ok(ApiResponse.success(nationalities));
    }

    // ==================== Admin Endpoints ====================

    /**
     * Create new author (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> createAuthor(@Valid @RequestBody AuthorDto authorDto) {
        try {
            Author created = authorService.createAuthor(authorDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(AuthorDto.from(created)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update author (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateAuthor(
            @PathVariable Long id,
            @Valid @RequestBody AuthorDto authorDto) {
        try {
            Author updated = authorService.updateAuthor(id, authorDto);
            return ResponseEntity.ok(ApiResponse.updated(AuthorDto.from(updated)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete author (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteAuthor(@PathVariable Long id) {
        try {
            authorService.deleteAuthor(id);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Count books by author
     */
    @GetMapping("/{id}/books/count")
    public ResponseEntity<ApiResponse<?>> countBooks(@PathVariable Long id) {
        if (!authorService.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Tác giả"));
        }
        long count = authorService.countBooks(id);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
