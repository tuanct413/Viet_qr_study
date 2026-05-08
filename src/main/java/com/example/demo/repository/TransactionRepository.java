package com.example.demo.repository;

import com.example.demo.entity.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    /**
     * Tìm giao dịch theo transactionId từ VietQR.
     * Dùng cho Idempotency: Nếu đã tồn tại thì không xử lý lại.
     */
    Optional<Transaction> findByTransactionId(String transactionId);

    /**
     * Tìm giao dịch theo orderId.
     * Dùng cho Polling: Kiểm tra trạng thái đơn hàng.
     */
    Optional<Transaction> findByOrderId(String orderId);
}
