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
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.dto.PublisherDto;
import fit.hutech.BuiBaoHan.entities.Publisher;
import fit.hutech.BuiBaoHan.services.PublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Publisher management
 */
@RestController
@RequestMapping("/api/publishers")
@RequiredArgsConstructor
public class ApiPublisherController {

    private final PublisherService publisherService;

    // ==================== Public Endpoints ====================

    /**
     * Get all publishers (paginated)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllPublishers(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Publisher> publishersPage = publisherService.getPublishers(pageable);
        List<PublisherDto> publisherDtos = publishersPage.getContent().stream()
                .map(PublisherDto::from)
                .toList();
        
        PageResponse<PublisherDto> pageResponse = PageResponse.from(publishersPage, publisherDtos);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Get all publishers (no pagination)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllPublishersNoPaging() {
        List<PublisherDto> publishers = publisherService.getAllPublishers().stream()
                .map(PublisherDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(publishers));
    }

    /**
     * Get publisher by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getPublisherById(@PathVariable Long id) {
        var optPublisher = publisherService.getPublisherById(id);
        if (optPublisher.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(PublisherDto.from(optPublisher.get())));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Publisher"));
    }

    /**
     * Get publisher with books by ID
     */
    @GetMapping("/{id}/with-books")
    public ResponseEntity<ApiResponse<?>> getPublisherWithBooks(@PathVariable Long id) {
        var optPublisher = publisherService.getPublisherWithBooks(id);
        if (optPublisher.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(PublisherDto.from(optPublisher.get())));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Publisher"));
    }

    /**
     * Search publishers by name
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchPublishers(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Publisher> publishersPage = publisherService.searchPublishers(keyword, pageable);
        List<PublisherDto> publishers = publishersPage.getContent().stream()
                .map(PublisherDto::from)
                .toList();
        PageResponse<PublisherDto> pageResponse = PageResponse.from(publishersPage, publishers);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Get all publishers as simple list
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<?>> getPublishersList() {
        List<PublisherDto> publishers = publisherService.getAllPublishers().stream()
                .map(PublisherDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(publishers));
    }

    // ==================== Admin Endpoints ====================

    /**
     * Create new publisher (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> createPublisher(@Valid @RequestBody PublisherDto publisherDto) {
        try {
            Publisher created = publisherService.createPublisher(publisherDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(PublisherDto.from(created)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update publisher (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updatePublisher(
            @PathVariable Long id,
            @Valid @RequestBody PublisherDto publisherDto) {
        try {
            Publisher updated = publisherService.updatePublisher(id, publisherDto);
            return ResponseEntity.ok(ApiResponse.updated(PublisherDto.from(updated)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete publisher (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deletePublisher(@PathVariable Long id) {
        try {
            publisherService.deletePublisher(id);
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
     * Count books by publisher
     */
    @GetMapping("/{id}/book-count")
    public ResponseEntity<ApiResponse<?>> countBooksByPublisher(@PathVariable Long id) {
        var optPublisher = publisherService.getPublisherById(id);
        if (optPublisher.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Publisher"));
        }
        long count = publisherService.countBooks(id);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
