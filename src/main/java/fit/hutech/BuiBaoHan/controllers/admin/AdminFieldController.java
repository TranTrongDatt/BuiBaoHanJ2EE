package fit.hutech.BuiBaoHan.controllers.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fit.hutech.BuiBaoHan.dto.FieldDto;
import fit.hutech.BuiBaoHan.entities.Field;
import fit.hutech.BuiBaoHan.services.FieldService;
import fit.hutech.BuiBaoHan.services.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Field Management Controller
 * Quản lý CRUD lĩnh vực sách
 */
@Controller
@RequestMapping("/admin/fields")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminFieldController {

    private final FieldService fieldService;
    private final FileStorageService fileStorageService;

    /**
     * Danh sách tất cả lĩnh vực
     */
    @GetMapping
    public String listFields(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search) {
        
        Page<Field> fields;
        
        if (search != null && !search.isEmpty()) {
            fields = fieldService.searchByName(search, pageable);
        } else {
            fields = fieldService.getFields(pageable);
        }
        
        model.addAttribute("fields", fields);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", "fields");
        model.addAttribute("pageTitle", "Quản lý Lĩnh vực");
        
        return "admin/fields/list";
    }

    /**
     * Form thêm lĩnh vực mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("field", new Field());
        model.addAttribute("currentPage", "fields");
        model.addAttribute("pageTitle", "Thêm Lĩnh vực");
        return "admin/fields/form";
    }

    /**
     * Form chỉnh sửa lĩnh vực
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return fieldService.getFieldById(id)
                .map(field -> {
                    model.addAttribute("field", field);
                    model.addAttribute("currentPage", "fields");
                    model.addAttribute("pageTitle", "Sửa Lĩnh vực");
                    return "admin/fields/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy lĩnh vực");
                    return "redirect:/admin/fields";
                });
    }

    /**
     * Lưu lĩnh vực (thêm mới)
     */
    @PostMapping("/save")
    public String saveField(
            @Valid @ModelAttribute("field") Field field,
            BindingResult result,
            @RequestParam(required = false) MultipartFile imageFile,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("currentPage", "fields");
            model.addAttribute("pageTitle", field.getId() == null ? "Thêm Lĩnh vực" : "Sửa Lĩnh vực");
            return "admin/fields/form";
        }
        
        try {
            // Handle image upload
            String imagePath = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                imagePath = fileStorageService.storeImage(imageFile, "fields");
            } else if (field.getId() != null) {
                // Keep existing image for updates
                imagePath = fieldService.getFieldById(field.getId())
                        .map(Field::getImage)
                        .orElse(null);
            }
            
            if (field.getId() == null) {
                // Create new field
                FieldDto dto = new FieldDto(
                        null,
                        field.getName(),
                        imagePath,
                        field.getDescription(),
                        null,
                        field.getDisplayOrder(),
                        field.getIsActive()
                );
                fieldService.createField(dto);
                redirectAttributes.addFlashAttribute("success", "Thêm lĩnh vực thành công");
            } else {
                // Update existing field
                FieldDto dto = new FieldDto(
                        field.getId(),
                        field.getName(),
                        imagePath,
                        field.getDescription(),
                        null,
                        field.getDisplayOrder(),
                        field.getIsActive()
                );
                fieldService.updateField(field.getId(), dto);
                redirectAttributes.addFlashAttribute("success", "Cập nhật lĩnh vực thành công");
            }
            
            return "redirect:/admin/fields";
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fields";
        } catch (java.io.IOException | RuntimeException e) {
            log.error("Error saving field: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/fields";
        }
    }

    /**
     * Xóa lĩnh vực
     */
    @PostMapping("/delete/{id}")
    public String deleteField(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            fieldService.deleteField(id);
            redirectAttributes.addFlashAttribute("success", "Xóa lĩnh vực thành công");
        } catch (IllegalStateException e) {
            log.error("Cannot delete field: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting field: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Không thể xóa lĩnh vực");
        }
        return "redirect:/admin/fields";
    }

    /**
     * Xem chi tiết lĩnh vực
     */
    @GetMapping("/{id}")
    public String viewField(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return fieldService.getFieldWithCategories(id)
                .map(field -> {
                    model.addAttribute("field", field);
                    model.addAttribute("categoryCount", fieldService.countCategories(id));
                    model.addAttribute("currentPage", "fields");
                    model.addAttribute("pageTitle", "Chi tiết Lĩnh vực");
                    return "admin/fields/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy lĩnh vực");
                    return "redirect:/admin/fields";
                });
    }
}
