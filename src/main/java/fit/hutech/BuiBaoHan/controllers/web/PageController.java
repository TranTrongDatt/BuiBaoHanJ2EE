package fit.hutech.BuiBaoHan.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller cho các trang tĩnh: Service, About Us
 */
@Controller
public class PageController {

    /**
     * Trang dịch vụ mượn/trả sách
     */
    @GetMapping("/service")
    public String service(Model model) {
        model.addAttribute("pageTitle", "Dịch vụ mượn sách");
        return "service/index";
    }

    /**
     * Trang giới thiệu shop
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "Giới thiệu");
        return "about/index";
    }
}
