package fit.hutech.BuiBaoHan.controllers.admin;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
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

import fit.hutech.BuiBaoHan.entities.Author;
import fit.hutech.BuiBaoHan.services.AuthorService;
import fit.hutech.BuiBaoHan.services.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Author Management Controller
 * Quản lý CRUD tác giả
 */
@Controller
@RequestMapping("/admin/authors")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthorController {

    private final AuthorService authorService;
    private final FileStorageService fileStorageService;

    /**
     * Danh sách tất cả tác giả
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String listAuthors(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String nationality) {
        
        Page<Author> authors;
        
        if (search != null && !search.isBlank()) {
            authors = authorService.searchAuthors(search.trim(), pageable);
        } else {
            authors = authorService.getAllAuthors(pageable);
        }
        
        // Filter by nationality if provided
        if (nationality != null && !nationality.isBlank()) {
            List<Author> filtered = authors.getContent().stream()
                    .filter(a -> nationality.equalsIgnoreCase(a.getNationality()))
                    .toList();
            authors = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }
        
        model.addAttribute("authors", authors);
        model.addAttribute("nationalities", authorService.getAllNationalities());
        model.addAttribute("search", search);
        model.addAttribute("nationality", nationality);
        model.addAttribute("currentPage", "authors");
        model.addAttribute("pageTitle", "Quản lý Tác giả");
        
        return "admin/authors/list";
    }

    /**
     * Form thêm tác giả mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("author", new Author());
        model.addAttribute("nationalities", authorService.getAllNationalities());
        model.addAttribute("currentPage", "authors");
        model.addAttribute("pageTitle", "Thêm Tác giả");
        return "admin/authors/form";
    }

    /**
     * Form chỉnh sửa tác giả
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return authorService.getAuthorById(id)
                .map(author -> {
                    model.addAttribute("author", author);
                    model.addAttribute("nationalities", authorService.getAllNationalities());
                    model.addAttribute("currentPage", "authors");
                    model.addAttribute("pageTitle", "Sửa Tác giả");
                    return "admin/authors/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy tác giả");
                    return "redirect:/admin/authors";
                });
    }

    /**
     * Lưu tác giả (thêm mới hoặc cập nhật)
     */
    @PostMapping("/save")
    public String saveAuthor(
            @Valid @ModelAttribute("author") Author author,
            BindingResult result,
            @RequestParam(required = false) MultipartFile avatarFile,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("nationalities", authorService.getAllNationalities());
            model.addAttribute("currentPage", "authors");
            model.addAttribute("pageTitle", author.getId() == null ? "Thêm Tác giả" : "Sửa Tác giả");
            return "admin/authors/form";
        }
        
        try {
            // Handle avatar upload
            if (avatarFile != null && !avatarFile.isEmpty()) {
                String avatarPath = fileStorageService.storeImage(avatarFile, "avatars");
                author.setAvatar(avatarPath);
            } else if (author.getId() != null) {
                // Keep existing avatar if no new file uploaded
                authorService.getAuthorById(author.getId())
                        .ifPresent(existing -> {
                            if (author.getAvatar() == null || author.getAvatar().isBlank()) {
                                author.setAvatar(existing.getAvatar());
                            }
                        });
            }
            
            // Set default isActive if null
            if (author.getIsActive() == null) {
                author.setIsActive(true);
            }
            
            // Save using repository via service's internal save
            saveAuthorEntity(author);
            
            String message = author.getId() == null ? "Thêm tác giả thành công" : "Cập nhật tác giả thành công";
            redirectAttributes.addFlashAttribute("success", message);
            
            return "redirect:/admin/authors";
            
        } catch (IOException e) {
            log.error("Error uploading avatar: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi tải ảnh: " + e.getMessage());
            return "redirect:/admin/authors";
        } catch (IllegalArgumentException e) {
            log.error("Error saving author: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/authors";
        }
    }

    /**
     * Helper method để lưu Author entity trực tiếp
     */
    private Author saveAuthorEntity(Author author) {
        // Sử dụng AuthorService để lưu
        if (author.getId() == null) {
            // Create new
            var dto = fit.hutech.BuiBaoHan.dto.AuthorDto.builder()
                    .name(author.getName())
                    .birthDate(author.getBirthDate())
                    .deathDate(author.getDeathDate())
                    .biography(author.getBiography())
                    .avatar(author.getAvatar())
                    .nationality(author.getNationality())
                    .website(author.getWebsite())
                    .isActive(author.getIsActive())
                    .build();
            return authorService.createAuthor(dto);
        } else {
            // Update existing
            var dto = fit.hutech.BuiBaoHan.dto.AuthorDto.builder()
                    .id(author.getId())
                    .name(author.getName())
                    .birthDate(author.getBirthDate())
                    .deathDate(author.getDeathDate())
                    .biography(author.getBiography())
                    .avatar(author.getAvatar())
                    .nationality(author.getNationality())
                    .website(author.getWebsite())
                    .isActive(author.getIsActive())
                    .build();
            return authorService.updateAuthor(author.getId(), dto);
        }
    }

    /**
     * Xóa tác giả
     */
    @PostMapping("/delete/{id}")
    public String deleteAuthor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            authorService.deleteAuthor(id);
            redirectAttributes.addFlashAttribute("success", "Xóa tác giả thành công");
        } catch (IllegalStateException e) {
            log.error("Cannot delete author: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting author: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Không thể xóa tác giả");
        }
        return "redirect:/admin/authors";
    }

    /**
     * Xem chi tiết tác giả
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String viewAuthor(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return authorService.getAuthorWithBooks(id)
                .map(author -> {
                    model.addAttribute("author", author);
                    model.addAttribute("bookCount", authorService.countBooks(id));
                    model.addAttribute("currentPage", "authors");
                    model.addAttribute("pageTitle", "Chi tiết Tác giả");
                    return "admin/authors/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy tác giả");
                    return "redirect:/admin/authors";
                });
    }

    /**
     * Toggle trạng thái active của tác giả
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            authorService.getAuthorById(id).ifPresent(author -> {
                author.setIsActive(!author.getIsActive());
                var dto = fit.hutech.BuiBaoHan.dto.AuthorDto.builder()
                        .id(author.getId())
                        .name(author.getName())
                        .birthDate(author.getBirthDate())
                        .deathDate(author.getDeathDate())
                        .biography(author.getBiography())
                        .avatar(author.getAvatar())
                        .nationality(author.getNationality())
                        .website(author.getWebsite())
                        .isActive(author.getIsActive())
                        .build();
                authorService.updateAuthor(id, dto);
            });
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công");
        } catch (Exception e) {
            log.error("Error toggling status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Không thể cập nhật trạng thái");
        }
        return "redirect:/admin/authors";
    }
}
