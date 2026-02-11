package fit.hutech.BuiBaoHan.constants;

/**
 * Phương thức thanh toán
 */
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng"),
    VIETQR("Chuyển khoản VietQR"),
    VISA("Thẻ Visa/Mastercard"),
    MOMO("Ví MoMo"),
    ZALOPAY("ZaloPay"),
    VNPAY("VNPay"),
    BANK_TRANSFER("Chuyển khoản ngân hàng");

    public final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
