package fit.hutech.BuiBaoHan.controllers;

import java.util.Optional;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

@Controller
public class ExceptionController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(@NotNull HttpServletRequest request) {
        return Optional
                .ofNullable(request.getAttribute(
                        RequestDispatcher.ERROR_STATUS_CODE))
                .map(status -> status instanceof Integer i ? i : Integer.valueOf(String.valueOf(status)))
                .filter(status -> status == 400
                || status == 401
                || status == 403
                || status == 404
                || status == 500)
                .map(status -> "errors/" + status)
                .orElse("errors/500");
    }

    /**
     * Access denied page (403)
     */
    @GetMapping("/403")
    public String accessDenied() {
        return "errors/403";
    }
}
