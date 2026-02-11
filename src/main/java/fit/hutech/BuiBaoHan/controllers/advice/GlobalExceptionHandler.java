package fit.hutech.BuiBaoHan.controllers.advice;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import fit.hutech.BuiBaoHan.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for web (Thymeleaf) controllers
 */
@ControllerAdvice(basePackages = {"fit.hutech.BuiBaoHan.controllers.web", "fit.hutech.BuiBaoHan.controllers.admin"})
@Slf4j
public class GlobalExceptionHandler {

    // ==================== HTTP Error Pages ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} - {}", request.getRequestURI(), ex.getMessage());
        
        ModelAndView mav = new ModelAndView("errors/404");
        mav.addObject("message", ex.getMessage());
        mav.addObject("path", request.getRequestURI());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("No resource found: {}", request.getRequestURI());
        
        ModelAndView mav = new ModelAndView("errors/404");
        mav.addObject("message", "The requested page was not found");
        mav.addObject("path", request.getRequestURI());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {} - {}", request.getRequestURI(), ex.getMessage());
        
        ModelAndView mav = new ModelAndView("errors/403");
        mav.addObject("message", "You do not have permission to access this resource");
        mav.addObject("path", request.getRequestURI());
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthenticationException(AuthenticationException ex, RedirectAttributes redirectAttributes) {
        log.warn("Authentication failed: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", "Please log in to continue");
        return "redirect:/login";
    }

    // ==================== Business Exceptions ====================

    @ExceptionHandler(InvalidOperationException.class)
    public String handleInvalidOperation(InvalidOperationException ex, RedirectAttributes redirectAttributes, 
                                         HttpServletRequest request) {
        log.warn("Invalid operation: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public String handleDuplicateResource(DuplicateResourceException ex, RedirectAttributes redirectAttributes,
                                          HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    // ==================== Library Exceptions ====================

    @ExceptionHandler(LibraryCardExpiredException.class)
    public String handleLibraryCardExpired(LibraryCardExpiredException ex, RedirectAttributes redirectAttributes) {
        log.warn("Library card expired: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        redirectAttributes.addFlashAttribute("cardExpired", true);
        return "redirect:/library/my-card";
    }

    @ExceptionHandler(BorrowLimitExceededException.class)
    public String handleBorrowLimitExceeded(BorrowLimitExceededException ex, RedirectAttributes redirectAttributes) {
        log.warn("Borrow limit exceeded: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/library/my-borrows";
    }

    @ExceptionHandler(BookNotAvailableException.class)
    public String handleBookNotAvailable(BookNotAvailableException ex, RedirectAttributes redirectAttributes) {
        log.warn("Book not available: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/library";
    }

    // ==================== Payment Exceptions ====================

    @ExceptionHandler(PaymentFailedException.class)
    public String handlePaymentFailed(PaymentFailedException ex, RedirectAttributes redirectAttributes) {
        log.error("Payment failed: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", "Payment failed: " + ex.getMessage());
        redirectAttributes.addFlashAttribute("paymentError", true);
        return "redirect:/cart/checkout";
    }

    // ==================== File Upload Exceptions ====================

    @ExceptionHandler(FileUploadException.class)
    public String handleFileUpload(FileUploadException ex, RedirectAttributes redirectAttributes,
                                   HttpServletRequest request) {
        log.warn("File upload error: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes,
                                      HttpServletRequest request) {
        log.warn("File too large: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", "File size exceeds maximum limit (10MB)");
        
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    // ==================== Database Exceptions ====================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException ex, 
                                               RedirectAttributes redirectAttributes,
                                               HttpServletRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        
        String message = "An error occurred while saving data";
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            message = "A record with this value already exists";
        }
        
        redirectAttributes.addFlashAttribute("error", message);
        
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    // ==================== Generic Exception Handler ====================

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: ", request.getRequestURI(), ex);
        
        ModelAndView mav = new ModelAndView("errors/500");
        mav.addObject("message", "An unexpected error occurred. Please try again later.");
        mav.addObject("path", request.getRequestURI());
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}
