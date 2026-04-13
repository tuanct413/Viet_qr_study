package com.example.demo.mqtt;

import com.example.demo.entity.Transaction;
import com.example.demo.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MqttMessageHandler: Bộ não xử lý tin nhắn của hệ thống.
 * Chuyên trách việc "nghe" các dữ liệu từ IoT gửi lên (Inbound).
 * 
 * - MQTT: Lắng nghe topic '.../evt' (Sự kiện từ thiết bị).
 * - Log4j2: Tự động ghi lại nội dung tin nhắn nhận được vào log để hậu kiểm.
 * - MongoDB: Sau khi nhận tin thanh toán, tiến hành lưu trữ vào Database.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Handler chính cho luồng nhận tin nhắn.
     * Khi có bất kỳ tin nhắn nào đổ về kênh 'mqttInboundChannel', hàm này sẽ được kích hoạt.
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler handler() {
        return message -> {
            // Lấy thông tin Topic và Nội dung tin nhắn
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String payload = (String) message.getPayload();
            
            log.info("--- [RECV] New Message from IoT ---");
            log.info("Topic: {}", topic);
            log.info("Payload: {}", payload);
            
            try {
                // 1. Phân tích JSON payload từ thiết bị
                JsonNode jsonNode = objectMapper.readTree(payload);
                String event = jsonNode.has("event") ? jsonNode.get("event").asText() : "";
                
                // Chỉ xử lý nếu đó là sự kiện thanh toán thành công
                if ("payment_success".equals(event)) {
                    log.info(">>> Phát hiện sự kiện thanh toán thành công từ thiết bị!");

                    // 2. Trích xuất thông tin nghiệp vụ
                    String orderId = jsonNode.has("orderId") ? jsonNode.get("orderId").asText() : "N/A";
                    Long amount = jsonNode.has("amount") ? jsonNode.get("amount").asLong() : 0L;
                    
                    // Logic trích xuất deviceId từ đường dẫn topic (iot/v1/tenant1/qrbox/{deviceId}/evt)
                    String deviceId = "unknown";
                    if (topic != null) {
                        String[] parts = topic.split("/");
                        if (parts.length >= 5) {
                            deviceId = parts[4];
                        }
                    }

                    // 3. Tiến hành Persistence (Lưu trữ) vào MongoDB
                    Transaction transaction = Transaction.builder()
                            .orderId(orderId)
                            .amount(amount)
                            .deviceId(deviceId)
                            .eventType(event)
                            .createdAt(LocalDateTime.now())
                            .build();

                    transactionRepository.save(transaction);
                    
                    // Log trạng thái để Admin có thể kiểm tra qua log file
                    log.info(">>> SUCCESS: Đã lưu giao dịch vào MongoDB. Device={}, OrderId={}", 
                             deviceId, orderId);
                }
            } catch (Exception e) {
                log.error("[ERROR] Lỗi xử lý tin nhắn MQTT: {}", e.getMessage());
            }
        };
    }
}

