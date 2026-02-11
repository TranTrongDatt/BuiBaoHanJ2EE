package fit.hutech.BuiBaoHan.dto;

import java.util.Collections;
import java.util.List;

import fit.hutech.BuiBaoHan.entities.Field;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record FieldDto(
        Long id,
        
        @NotBlank(message = "Tên lĩnh vực không được để trống")
        @Size(max = 100, message = "Tên lĩnh vực không quá 100 ký tự")
        String name,
        
        String image,
        
        @Size(max = 500, message = "Mô tả không quá 500 ký tự")
        String description,
        
        String slug,
        
        Integer displayOrder,
        
        Boolean isActive,
        
        List<String> categoryNames
) {
    // Constructor without categoryNames for backwards compatibility
    public FieldDto(Long id, String name, String image, String description, 
                    String slug, Integer displayOrder, Boolean isActive) {
        this(id, name, image, description, slug, displayOrder, isActive, Collections.emptyList());
    }
    
    public static FieldDto from(Field field) {
        return FieldDto.builder()
                .id(field.getId())
                .name(field.getName())
                .image(field.getImage())
                .description(field.getDescription())
                .slug(field.getSlug())
                .displayOrder(field.getDisplayOrder())
                .isActive(field.getIsActive())
                .categoryNames(Collections.emptyList())
                .build();
    }
    
    public static FieldDto withCategories(Field field) {
        List<String> catNames = field.getCategories() != null 
                ? field.getCategories().stream().map(c -> c.getName()).toList()
                : Collections.emptyList();
        
        return FieldDto.builder()
                .id(field.getId())
                .name(field.getName())
                .image(field.getImage())
                .description(field.getDescription())
                .slug(field.getSlug())
                .displayOrder(field.getDisplayOrder())
                .isActive(field.getIsActive())
                .categoryNames(catNames)
                .build();
    }
    
    public Field toEntity() {
        return Field.builder()
                .id(id)
                .name(name)
                .image(image)
                .description(description)
                .slug(slug)
                .displayOrder(java.util.Objects.requireNonNullElse(displayOrder, 0))
                .isActive(!Boolean.FALSE.equals(isActive))
                .build();
    }
}
