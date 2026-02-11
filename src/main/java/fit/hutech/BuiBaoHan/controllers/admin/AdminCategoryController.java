package fit.hutech.BuiBaoHan.controllers.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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

import fit.hutech.BuiBaoHan.entities.Category;
import fit.hutech.BuiBaoHan.services.CategoryService;
import fit.hutech.BuiBaoHan.services.FieldService;
import fit.hutech.BuiBaoHan.services.FileStorageService;
import fit.hutech.BuiBaoHan.services.SlugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Category Management Controller
 * Quản lý CRUD danh mục (thể loại) sách
 */
@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final FieldService fieldService;
    private final FileStorageService fileStorageService;
    private final SlugService slugService;

    /**
     * Danh sách tất cả danh mục
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String listCategories(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long fieldId) {
        
        List<Category> allCategories = categoryService.getAllCategories();
        
        // Filter by search
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            allCategories = allCategories.stream()
                    .filter(c -> c.getName().toLowerCase().contains(searchLower))
                    .toList();
        }
        
        // Filter by field
        if (fieldId != null) {
            allCategories = allCategories.stream()
                    .filter(c -> c.getField() != null && c.getField().getId().equals(fieldId))
                    .toList();
        }
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allCategories.size());
        List<Category> pageContent = start < allCategories.size() 
                ? allCategories.subList(start, end) 
                : List.of();
        Page<Category> categories = new PageImpl<>(pageContent, pageable, allCategories.size());
        
        model.addAttribute("categories", categories);
        model.addAttribute("fields", fieldService.getAllFields());
        model.addAttribute("search", search);
        model.addAttribute("fieldId", fieldId);
        model.addAttribute("currentPage", "categories");
        model.addAttribute("pageTitle", "Quản lý Danh mục");
        
        return "admin/categories/list";
    }

    /**
     * Form thêm danh mục mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("fields", fieldService.getAllFields());
        model.addAttribute("currentPage", "categories");
        model.addAttribute("pageTitle", "Thêm Danh mục");
        return "admin/categories/form";
    }

    /**
     * Form chỉnh sửa danh mục
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return categoryService.getCategoryById(id)
                .map(category -> {
                    model.addAttribute("category", category);
                    model.addAttribute("fields", fieldService.getAllFields());
                    model.addAttribute("currentPage", "categories");
                    model.addAttribute("pageTitle", "Sửa Danh mục");
                    return "admin/categories/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục");
                    return "redirect:/admin/categories";
                });
    }

    /**
     * Lưu danh mục (thêm mới hoặc cập nhật)
     */
    @PostMapping("/save")
    public String saveCategory(
            @Valid @ModelAttribute("category") Category category,
            BindingResult result,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) Long fieldId,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("fields", fieldService.getAllFields());
            model.addAttribute("currentPage", "categories");
            model.addAttribute("pageTitle", category.getId() == null ? "Thêm Danh mục" : "Sửa Danh mục");
            return "admin/categories/form";
        }
        
        try {
            // Generate slug
            if (category.getSlug() == null || category.getSlug().isEmpty()) {
                category.setSlug(slugService.toSlug(category.getName()));
            }
            
            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                String imagePath = fileStorageService.storeImage(imageFile, "categories");
                category.setImage(imagePath);
            }
            
            // Set field relationship
            if (fieldId != null) {
                fieldService.getFieldById(fieldId).ifPresent(category::setField);
            }
            
            // Save category
            categoryService.addCategory(category);
            
            redirectAttributes.addFlashAttribute("success", 
                    category.getId() == null ? "Thêm danh mục thành công" : "Cập nhật danh mục thành công");
            
            return "redirect:/admin/categories";
            
        } catch (java.io.IOException | RuntimeException e) {
            log.error("Error saving category: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    /**
     * Xóa danh mục
     */
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategoryById(id);
            redirectAttributes.addFlashAttribute("success", "Xóa danh mục thành công");
        } catch (Exception e) {
            log.error("Error deleting category: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Không thể xóa danh mục vì đang được sử dụng");
        }
        return "redirect:/admin/categories";
    }

    /**
     * Xem chi tiết danh mục
     */
    @GetMapping("/{id}")
    public String viewCategory(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return categoryService.getCategoryByIdWithDetails(id)
                .map(category -> {
                    model.addAttribute("category", category);
                    model.addAttribute("currentPage", "categories");
                    model.addAttribute("pageTitle", "Chi tiết Danh mục");
                    return "admin/categories/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục");
                    return "redirect:/admin/categories";
                });
    }
}
