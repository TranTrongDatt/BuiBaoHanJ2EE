package fit.hutech.BuiBaoHan.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service gửi email.
 * 
 * Email templates:
 * - Password reset
 * - Welcome email
 * - Order confirmation
 * - Overdue reminder
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@miniverse.vn}")
    private String fromEmail;

    @Value("${app.name:MiniVerse}")
    private String appName;

    @Value("${app.url:http://localhost:9090}")
    private String appUrl;

    /**
     * Gửi email reset password
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("resetToken", resetToken);
            context.setVariable("resetLink", appUrl + "/reset-password?token=" + resetToken);
            context.setVariable("appName", appName);
            context.setVariable("expiryMinutes", 15);

            String htmlContent = templateEngine.process("email/password-reset", context);

            sendHtmlEmail(toEmail, appName + " - Đặt lại mật khẩu", htmlContent);
            
            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Gửi email chào mừng
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);

            String htmlContent = templateEngine.process("email/welcome", context);

            sendHtmlEmail(toEmail, "Chào mừng bạn đến với " + appName + "!", htmlContent);
            
            log.info("Welcome email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Gửi email thông báo bảo mật (đăng nhập mới, đổi mật khẩu)
     */
    @Async
    public void sendSecurityAlertEmail(String toEmail, String userName, String alertType, String details) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("alertType", alertType);
            context.setVariable("details", details);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/security-alert", context);

            sendHtmlEmail(toEmail, appName + " - Thông báo bảo mật", htmlContent);
            
            log.info("Security alert email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send security alert email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Helper method gửi HTML email
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }

    /**
     * Gửi email đơn giản (text only)
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            
            mailSender.send(message);
            log.info("Simple email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage());
        }
    }
}
