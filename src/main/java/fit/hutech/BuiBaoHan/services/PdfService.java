package fit.hutech.BuiBaoHan.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.pdf.BaseFont;

import fit.hutech.BuiBaoHan.entities.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service tạo PDF từ Thymeleaf template
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private final TemplateEngine templateEngine;

    /**
     * Tạo PDF hóa đơn từ đơn hàng
     */
    public byte[] generateInvoicePdf(Order order) {
        try {
            // Tạo context cho Thymeleaf
            Context context = new Context();
            context.setVariable("order", order);
            context.setVariable("storeInfo", new StoreInfo());

            // Render HTML từ template
            String htmlContent = templateEngine.process("pdf/invoice", context);

            // Chuyển HTML thành PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            
            // Đăng ký font hỗ trợ tiếng Việt
            registerVietnameseFont(renderer);
            
            // Set base URL cho resources (CSS, images)
            String baseUrl = getClass().getClassLoader().getResource("static/").toExternalForm();
            renderer.setDocumentFromString(htmlContent, baseUrl);
            renderer.layout();
            renderer.createPDF(outputStream);

            log.info("Generated PDF invoice for order: {}", order.getOrderCode());
            return outputStream.toByteArray();
        } catch (com.lowagie.text.DocumentException e) {
            log.error("Error generating PDF for order {}: {}", order.getOrderCode(), e.getMessage(), e);
            throw new RuntimeException("Không thể tạo hóa đơn PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đăng ký font hỗ trợ tiếng Việt cho PDF
     */
    private void registerVietnameseFont(ITextRenderer renderer) {
        try {
            ITextFontResolver fontResolver = renderer.getFontResolver();
            
            // Thử tải font từ resources
            try (InputStream fontStream = getClass().getClassLoader()
                    .getResourceAsStream("fonts/DejaVuSans.ttf")) {
                if (fontStream != null) {
                    // Nếu có font trong resources, sử dụng font đó
                    String fontPath = getClass().getClassLoader()
                            .getResource("fonts/DejaVuSans.ttf").toExternalForm();
                    fontResolver.addFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    log.info("Loaded DejaVuSans font from resources");
                    return;
                }
            } catch (IOException | com.lowagie.text.DocumentException e) {
                log.debug("DejaVuSans font not found in resources, trying system fonts");
            }
            
            // Sử dụng font hệ thống Windows - Arial Unicode MS hoặc Segoe UI
            String[] windowsFonts = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/arialuni.ttf",
                "C:/Windows/Fonts/segoeui.ttf",
                "C:/Windows/Fonts/tahoma.ttf"
            };
            
            for (String fontPath : windowsFonts) {
                try {
                    java.io.File fontFile = new java.io.File(fontPath);
                    if (fontFile.exists()) {
                        fontResolver.addFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        log.info("Loaded system font: {}", fontPath);
                        return;
                    }
                } catch (IOException | com.lowagie.text.DocumentException e) {
                    log.debug("Could not load font: {}", fontPath);
                }
            }
            
            log.warn("No Vietnamese-supporting font found, using default fonts");
        } catch (com.lowagie.text.DocumentException e) {
            log.error("Error registering Vietnamese font: {}", e.getMessage());
        }
    }

    /**
     * Thông tin cửa hàng để hiển thị trên hóa đơn
     */
    public static class StoreInfo {
        public String name = "MiniVerse Bookstore";
        public String address = "10/80c Song Hành Xa Lộ Hà Nội, Phường Tân Phú, Thủ Đức, Thành phố Hồ Chí Minh";
        public String phone = "0329222698";
        public String email = "miniverse@gmail.com";
        public String website = "www.miniverse.vn";
        public String taxCode = "0123456789";
    }
}
