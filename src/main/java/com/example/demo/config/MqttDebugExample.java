package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ĐÂY LÀ VÍ DỤ MẪU VỀ CÁCH DEBUG LỖI MQTT
 * Bạn có thể áp dụng mô hình này khi tích hợp thư viện MQTT (như Paho) vào project.
 */
@Slf4j
@Component
public class MqttDebugExample {

    // Giả lập phương thức xử lý khi mất kết nối MQTT
    public void onConnectionLost(Throwable cause) {
        // Log ERROR khi có sự cố nghiêm trọng
        log.error("MQTT_DEBUG: Connection Lost! Reason: {} at: {}", 
                cause.getMessage(), System.currentTimeMillis());
        
        // Log DEBUG để xem chi tiết StackTrace phục vụ gỡ lỗi sâu
        log.debug("MQTT_DEBUG: Detailed StackTrace: ", cause);
    }

    // Giả lập phương thức nhận tin nhắn từ MQTT Broker
    public void onMessageArrived(String topic, String payload) {
        // Log INFO để theo dõi luồng tin nhắn cơ bản
        log.info("MQTT_DEBUG: Message arrived on Topic: {}", topic);
        
        // Log DEBUG để soi nội dung Payload thô (giúp kiểm tra định dạng JSON...)
        log.debug("MQTT_DEBUG: Raw Payload: {}", payload);
        
        try {
            processMessage(payload);
        } catch (Exception e) {
            // Log ERROR kèm theo context (Topic, Payload) để biết tin nhắn nào gây lỗi
            log.error("MQTT_DEBUG: Failed to process message on Topic: {} | Payload: {} | Error: {}", 
                    topic, payload, e.getMessage());
        }
    }

    private void processMessage(String payload) throws Exception {
        // Logic xử lý tin nhắn
        if (payload.contains("error")) {
            throw new Exception("Invalid format detected in device data");
        }
    }
}
