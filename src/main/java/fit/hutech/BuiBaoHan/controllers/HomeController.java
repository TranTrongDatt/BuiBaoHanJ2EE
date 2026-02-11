package fit.hutech.BuiBaoHan.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import fit.hutech.BuiBaoHan.services.BookService;
import fit.hutech.BuiBaoHan.services.FieldService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class HomeController {

    private final FieldService fieldService;
    private final BookService bookService;

    @GetMapping
    public String home(Model model) {
        // Danh mục (lĩnh vực) active từ DB
        model.addAttribute("fields", fieldService.getActiveFields());
        // Sách nổi bật từ DB (tối đa 10 cuốn cho Swiper)
        model.addAttribute("featuredBooks", bookService.getFeaturedBooks(10));
        return "home/index";
    }
}
