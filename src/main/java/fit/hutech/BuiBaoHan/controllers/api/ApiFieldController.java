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
import fit.hutech.BuiBaoHan.dto.FieldDto;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.entities.Field;
import fit.hutech.BuiBaoHan.services.FieldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Field management
 */
@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
public class ApiFieldController {

    private final FieldService fieldService;

    // ==================== Public Endpoints ====================

    /**
     * Get all fields (paginated)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FieldDto>>> getAllFields(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Field> fieldsPage = fieldService.findAll(pageable);
        List<FieldDto> fieldDtos = fieldsPage.getContent().stream()
                .map(FieldDto::from)
                .toList();
        
        PageResponse<FieldDto> pageResponse = PageResponse.from(fieldsPage, fieldDtos);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Get all fields (no pagination)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FieldDto>>> getAllFieldsNoPaging() {
        List<FieldDto> fields = fieldService.findAll().stream()
                .map(FieldDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(fields));
    }

    /**
     * Get field by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FieldDto>> getFieldById(@PathVariable Long id) {
        return fieldService.findById(id)
                .map(field -> ResponseEntity.ok(ApiResponse.success(FieldDto.from(field))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Field")));
    }

    /**
     * Get field by slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<FieldDto>> getFieldBySlug(@PathVariable String slug) {
        return fieldService.findBySlug(slug)
                .map(field -> ResponseEntity.ok(ApiResponse.success(FieldDto.from(field))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Field")));
    }

    /**
     * Search fields by name
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FieldDto>>> searchFields(@RequestParam String keyword) {
        List<FieldDto> fields = fieldService.search(keyword).stream()
                .map(FieldDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(fields));
    }

    /**
     * Get fields with categories
     */
    @GetMapping("/with-categories")
    public ResponseEntity<ApiResponse<List<FieldDto>>> getFieldsWithCategories() {
        List<FieldDto> fields = fieldService.findAll().stream()
                .map(FieldDto::withCategories)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(fields));
    }

    // ==================== Admin Endpoints ====================

    /**
     * Create new field (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FieldDto>> createField(@Valid @RequestBody FieldDto fieldDto) {
        try {
            Field created = fieldService.create(fieldDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(FieldDto.from(created)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update field (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FieldDto>> updateField(
            @PathVariable Long id,
            @Valid @RequestBody FieldDto fieldDto) {
        try {
            Field updated = fieldService.update(id, fieldDto);
            return ResponseEntity.ok(ApiResponse.updated(FieldDto.from(updated)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete field (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteField(@PathVariable Long id) {
        try {
            fieldService.delete(id);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
