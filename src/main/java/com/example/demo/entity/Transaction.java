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
 * Entity lưu trữ giao dịch — dùng cho cả MQTT (IoT) và VietQR Webhook.
 *
 * - transactionId : Idempotency key từ VietQR (tránh xử lý 2 lần).
 * - expectedAmount: Số tiền kỳ vọng khi tạo QR (phát hiện underpayment).
 * - eventType      : Loại sự kiện MQTT (payment_success) — legacy field.
 * - status         : PENDING | SUCCESS | UNDERPAID | DUPLICATE
 */
@Document(collection = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    private String id;

    /** ID giao dịch từ VietQR — Idempotency Key, UNIQUE */
    @Indexed(unique = true, sparse = true)
    private String transactionId;

    /** Mã đơn hàng của đối tác */
    private String orderId;

    /** Số tiền thực tế nhận từ VietQR callback / MQTT */
    private Long amount;

    /** Số tiền kỳ vọng khi tạo QR — dùng để phát hiện underpayment */
    private Long expectedAmount;

    /** Loại giao dịch: C (ghi có) hoặc D (ghi nợ) */
    private String transType;

    /** Nội dung chuyển tiền */
    private String content;

    /** Số tài khoản ngân hàng */
    private String bankAccount;

    /** Device IoT liên kết (từ MQTT topic) */
    private String deviceId;

    /** Loại sự kiện MQTT — legacy: payment_success */
    private String eventType;

    /** Trạng thái: PENDING | SUCCESS | UNDERPAID | DUPLICATE */
    private String status;

    /** Thời điểm nhận webhook hoặc sự kiện MQTT */
    private LocalDateTime createdAt;
}
