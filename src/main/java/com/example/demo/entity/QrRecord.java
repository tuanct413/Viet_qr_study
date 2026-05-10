package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ thông tin QR đã tạo.
 *
 * - orderId: Key định danh đơn hàng, UNIQUE — dùng cho idempotent generate.
 * - expectedAmount: Dùng để kiểm tra sai lệch số tiền tại webhook.
 * - qrCode / qrLink / transactionRefId: Dữ liệu từ response của VietQR Gateway.
 * - status: PENDING → PAID / EXPIRED / UNDERPAID
 */
@Document(collection = "qr_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrRecord {

    @Id
    private String id;

    /** orderId của đối tác — UNIQUE key để tránh tạo QR trùng khi timeout/retry */
    @Indexed(unique = true)
    private String orderId;

    /** Số tiền kỳ vọng khi tạo QR */
    private Long expectedAmount;

    /** Chuỗi EMVCo QR trả về từ VietQR */
    private String qrCode;

    /** Link QR hiển thị dạng URL */
    private String qrLink;

    /** transactionRefId từ VietQR — dùng để polling trạng thái */
    private String transactionRefId;

    /** Nội dung chuyển khoản (chứa mã VQR...) — dùng để tìm đơn khi mất orderId */
    @Indexed
    private String content;

    /** Thời điểm tạo QR */
    private LocalDateTime createdAt;

    /**
     * Trạng thái QR:
     * - PENDING: Chưa thanh toán
     * - PAID: Đã thanh toán đủ
     * - UNDERPAID: Thanh toán thiếu
    /** Trạng thái đơn hàng (PENDING, PAID, ...) */
    private String status;

    /** Chữ ký (sign) gửi lên lúc tạo QR — dùng để đối soát Webhook (Correlation ID) */
    private String sign;
}
