package fit.hutech.BuiBaoHan.services;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Slider Captcha Service - Tạo và xác thực slider puzzle captcha.
 * 
 * Workflow:
 * 1. GET /api/captcha/slider → trả về ảnh nền + puzzle piece + token
 * 2. User kéo slider đến đúng vị trí
 * 3. POST /api/captcha/slider/verify với token và positionX
 * 4. Server so sánh positionX với expected (tolerance ±5px)
 */
@Service
@Slf4j
public class SliderCaptchaService {

    private static final int PUZZLE_WIDTH = 50;
    private static final int PUZZLE_HEIGHT = 50;
    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 150;
    private static final int TOLERANCE = 5; // ±5 pixels
    private static final long EXPIRY_MS = TimeUnit.MINUTES.toMillis(5); // 5 minutes

    private final SecureRandom random = new SecureRandom();
    
    // Lưu token → expected X position và thời gian tạo
    private final ConcurrentHashMap<String, CaptchaData> captchaStore = new ConcurrentHashMap<>();

    // Danh sách ảnh nền mặc định
    private static final String[] BACKGROUND_IMAGES = {
            "static/images/captcha/bg1.jpg",
            "static/images/captcha/bg2.jpg",
            "static/images/captcha/bg3.jpg"
    };

    /**
     * Generate slider captcha
     * @return SliderCaptchaResponse với base64 images và token
     */
    public SliderCaptchaResponse generateCaptcha() {
        try {
            // Load random background image (hoặc generate solid color nếu không có ảnh)
            BufferedImage background = loadOrCreateBackground();
            
            // Random position X (từ 50 đến IMAGE_WIDTH - PUZZLE_WIDTH - 50)
            int posX = 50 + random.nextInt(IMAGE_WIDTH - PUZZLE_WIDTH - 100);
            int posY = (IMAGE_HEIGHT - PUZZLE_HEIGHT) / 2;

            // Tạo puzzle piece (cắt từ ảnh nền)
            BufferedImage puzzle = createPuzzlePiece(background, posX, posY);

            // Tạo ảnh nền với "lỗ" puzzle
            BufferedImage backgroundWithHole = createBackgroundWithHole(background, posX, posY);

            // Generate token và lưu position
            String token = UUID.randomUUID().toString();
            captchaStore.put(token, new CaptchaData(posX, System.currentTimeMillis()));

            // Convert to Base64
            String bgBase64 = toBase64(backgroundWithHole, "png");
            String puzzleBase64 = toBase64(puzzle, "png");

            log.debug("Generated slider captcha: token={}, posX={}", token, posX);

            return new SliderCaptchaResponse(token, bgBase64, puzzleBase64, posY);

        } catch (Exception e) {
            log.error("Failed to generate slider captcha: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate captcha", e);
        }
    }

    /**
     * Verify slider captcha
     * @param token Token từ generateCaptcha
     * @param positionX Vị trí X mà user kéo đến
     * @return true nếu đúng vị trí (±tolerance)
     */
    public boolean verifyCaptcha(String token, int positionX) {
        if (token == null || token.isBlank()) {
            log.warn("Slider captcha verify: empty token");
            return false;
        }

        CaptchaData data = captchaStore.remove(token);
        if (data == null) {
            log.warn("Slider captcha verify: token not found or expired");
            return false;
        }

        // Check expiry
        if (System.currentTimeMillis() - data.createdAt > EXPIRY_MS) {
            log.warn("Slider captcha verify: token expired");
            return false;
        }

        // Check position với tolerance
        int diff = Math.abs(data.expectedX - positionX);
        boolean success = diff <= TOLERANCE;

        log.debug("Slider captcha verify: expected={}, actual={}, diff={}, success={}",
                data.expectedX, positionX, diff, success);

        return success;
    }

    /**
     * Load ảnh nền từ resources hoặc tạo gradient nếu không có
     */
    private BufferedImage loadOrCreateBackground() {
        // Thử load từ resources
        for (String path : BACKGROUND_IMAGES) {
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) {
                            // Resize nếu cần
                            return resizeImage(img, IMAGE_WIDTH, IMAGE_HEIGHT);
                        }
                    }
                }
            } catch (IOException ignored) {
                // Thử ảnh tiếp theo
            }
        }

        // Không có ảnh → tạo gradient background
        log.debug("No background images found, creating gradient");
        return createGradientBackground();
    }

    /**
     * Tạo gradient background đẹp mắt
     */
    private BufferedImage createGradientBackground() {
        BufferedImage img = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Random gradient colors
        Color[] colors = {
                new Color(52, 152, 219),  // Blue
                new Color(155, 89, 182),  // Purple
                new Color(46, 204, 113),  // Green
                new Color(241, 196, 15),  // Yellow
                new Color(231, 76, 60)    // Red
        };
        
        Color color1 = colors[random.nextInt(colors.length)];
        Color color2 = colors[random.nextInt(colors.length)];

        // Draw gradient
        for (int x = 0; x < IMAGE_WIDTH; x++) {
            float ratio = (float) x / IMAGE_WIDTH;
            int r = (int) (color1.getRed() * (1 - ratio) + color2.getRed() * ratio);
            int g = (int) (color1.getGreen() * (1 - ratio) + color2.getGreen() * ratio);
            int b = (int) (color1.getBlue() * (1 - ratio) + color2.getBlue() * ratio);
            g2d.setColor(new Color(r, g, b));
            g2d.drawLine(x, 0, x, IMAGE_HEIGHT);
        }

        // Add some random circles for visual interest
        for (int i = 0; i < 5; i++) {
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            int size = 20 + random.nextInt(40);
            g2d.setColor(new Color(255, 255, 255, 50));
            g2d.fillOval(x, y, size, size);
        }

        g2d.dispose();
        return img;
    }

    /**
     * Tạo puzzle piece (hình vuông với notch)
     */
    private BufferedImage createPuzzlePiece(BufferedImage background, int posX, int posY) {
        BufferedImage puzzle = new BufferedImage(PUZZLE_WIDTH, PUZZLE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = puzzle.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Copy khu vực từ background
        for (int x = 0; x < PUZZLE_WIDTH; x++) {
            for (int y = 0; y < PUZZLE_HEIGHT; y++) {
                int bgX = posX + x;
                int bgY = posY + y;
                if (bgX >= 0 && bgX < background.getWidth() && bgY >= 0 && bgY < background.getHeight()) {
                    puzzle.setRGB(x, y, background.getRGB(bgX, bgY));
                }
            }
        }

        // Thêm border trắng
        g2d.setColor(Color.WHITE);
        g2d.drawRect(0, 0, PUZZLE_WIDTH - 1, PUZZLE_HEIGHT - 1);
        g2d.drawRect(1, 1, PUZZLE_WIDTH - 3, PUZZLE_HEIGHT - 3);

        g2d.dispose();
        return puzzle;
    }

    /**
     * Tạo background với "lỗ" (hole) tại vị trí puzzle
     */
    private BufferedImage createBackgroundWithHole(BufferedImage background, int posX, int posY) {
        BufferedImage result = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(background, 0, 0, null);

        // Vẽ lỗ (màu tối hơn)
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(posX, posY, PUZZLE_WIDTH, PUZZLE_HEIGHT);
        
        // Border lỗ
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.drawRect(posX, posY, PUZZLE_WIDTH - 1, PUZZLE_HEIGHT - 1);

        g2d.dispose();
        return result;
    }

    /**
     * Resize image
     */
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Convert BufferedImage to Base64
     */
    private String toBase64(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return "data:image/" + format + ";base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Cleanup expired captchas (chạy mỗi 10 phút)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupExpiredCaptchas() {
        long now = System.currentTimeMillis();
        captchaStore.entrySet().removeIf(entry -> 
                now - entry.getValue().createdAt > EXPIRY_MS);
        log.debug("Cleaned up expired captchas, remaining: {}", captchaStore.size());
    }

    // ========== Inner classes ==========

    private record CaptchaData(int expectedX, long createdAt) {}

    public record SliderCaptchaResponse(
            String token,
            String backgroundImage,
            String puzzleImage,
            int puzzleY
    ) {}
}
