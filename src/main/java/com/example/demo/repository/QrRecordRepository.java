package com.example.demo.repository;

import com.example.demo.entity.QrRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrRecordRepository extends MongoRepository<QrRecord, String> {

    /**
     * Tìm QR theo orderId.
     * Dùng cho:
     * - Idempotent Generate: Nếu QR đã tồn tại thì trả lại thay vì tạo mới.
     * - Polling: Lấy expectedAmount để so sánh khi nhận webhook.
     */
    Optional<QrRecord> findByOrderId(String orderId);

    /**
     * Tìm QR dựa trên nội dung chuyển khoản.
     * Dùng khi webhook gửi về bị mất orderId nhưng vẫn chứa mã VQR... trong content.
     */
    Optional<QrRecord> findByContent(String content);
}
