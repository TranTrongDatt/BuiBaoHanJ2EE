package fit.hutech.BuiBaoHan.constants;

/**
 * Loại tin nhắn chat
 */
public enum MessageType {
    TEXT,       // Tin nhắn văn bản thông thường
    IMAGE,      // Tin nhắn hình ảnh
    FILE,       // Tin nhắn file đính kèm
    SYSTEM,     // Tin nhắn hệ thống
    JOIN,       // Thông báo user tham gia phòng
    LEAVE,      // Thông báo user rời phòng
    TYPING      // User đang nhập
}
