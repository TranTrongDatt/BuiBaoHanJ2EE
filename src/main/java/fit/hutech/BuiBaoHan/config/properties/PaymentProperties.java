package fit.hutech.BuiBaoHan.config.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Payment configuration properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    
    /**
     * Default currency
     */
    private String currency = "VND";
    
    /**
     * Order expiration in minutes
     */
    private int orderExpirationMinutes = 30;
    
    /**
     * VNPay configuration
     */
    private VnPay vnpay = new VnPay();
    
    /**
     * MoMo configuration
     */
    private MoMo momo = new MoMo();
    
    /**
     * COD (Cash on Delivery) configuration
     */
    private Cod cod = new Cod();
    
    /**
     * Available payment methods
     */
    private Map<String, Boolean> methods = new HashMap<>() {{
        put("vnpay", true);
        put("momo", true);
        put("cod", true);
        put("balance", true);
    }};
    
    @Data
    public static class VnPay {
        /**
         * VNPay Terminal ID
         */
        private String tmnCode;
        
        /**
         * VNPay secret key
         */
        private String hashSecret;
        
        /**
         * VNPay API URL
         */
        private String url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        
        /**
         * Return URL after payment
         */
        private String returnUrl;
        
        /**
         * Version
         */
        private String version = "2.1.0";
        
        /**
         * Command
         */
        private String command = "pay";
        
        /**
         * Order type
         */
        private String orderType = "other";
    }
    
    @Data
    public static class MoMo {
        /**
         * MoMo Partner Code
         */
        private String partnerCode;
        
        /**
         * MoMo Access Key
         */
        private String accessKey;
        
        /**
         * MoMo Secret Key
         */
        private String secretKey;
        
        /**
         * MoMo API endpoint
         */
        private String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
        
        /**
         * Return URL after payment
         */
        private String returnUrl;
        
        /**
         * IPN URL for notification
         */
        private String ipnUrl;
    }
    
    @Data
    public static class Cod {
        /**
         * Enable COD
         */
        private boolean enabled = true;
        
        /**
         * Maximum order amount for COD
         */
        private long maxAmount = 5000000;
        
        /**
         * Minimum order amount for COD
         */
        private long minAmount = 0;
        
        /**
         * COD fee
         */
        private long fee = 0;
    }
}
