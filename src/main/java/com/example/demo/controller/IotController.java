package com.example.demo.controller;

import com.example.demo.mqtt.MqttGateway;
import com.example.demo.mqtt.MqttStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * IotController: Cung cấp các API để điều khiển và giả lập thiết bị IoT.
 * 
 * - API Control: Dùng để gửi lệnh OPEN/CLOSE từ Backend xuống Box qua MQTT (Topic: .../cmd).
 * - API Simulate: Dùng để giả lập việc thiết bị gửi tin nhắn thanh toán lên Backend (Topic: .../evt).
 * - Log4j2: Mọi hoạt động gửi tin được log lại để kỹ thuật viên có thể trace lỗi trong logs/api.log.
 */
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
@Log4j2
public class IotController {

    private final MqttGateway mqttGateway;
    private final MqttStatusService mqttStatusService;

    /**
     * Kiểm tra trạng thái "sống" của kết nối MQTT.
     * Tránh việc gửi lệnh khi Broker đang offline dẫn đến treo ứng dụng.
     */
    private boolean isMqttConnected() {
        return mqttStatusService.isConnected();
    }

    /**
     * API Gửi lệnh điều khiển trực tiếp xuống thiết bị.
     * Luồng: Client -> API -> MQTT Gateway -> Broker -> IoT Device.
     */
    @PostMapping("/box/{deviceId}/control")
    public Map<String, Object> controlBox(
            @PathVariable String deviceId,
            @RequestParam String action) {

        if (!isMqttConnected()) {
            log.error("[ERROR] MQTT Broker is offline. Cannot send action: {}", action);
            return createErrorResponse("MQTT Broker is offline. Cannot send command.");
        }

        String topic = String.format("iot/v1/tenant1/qrbox/%s/cmd", deviceId);
        String payload = String.format("{\"command\": \"%s\", \"timestamp\": %d}", 
                                        action, System.currentTimeMillis());

        log.info("--- [SEND] Sending Command to IoT ---");
        log.info("Device: {}, Command: {}", deviceId, action);
        
        // MQTT Gateway chịu trách nhiệm đẩy tin nhắn vào đường ống Outbound
        mqttGateway.sendToMqtt(topic, 1, payload);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("device", deviceId);
        response.put("sent_command", action);
        return response;
    }

    /**
     * API Giả lập thiết bị báo thanh toán thành công.
     * Giúp Tester có thể test luồng xử lý giao dịch mà không cần thiết bị thật.
     */
    @PostMapping("/box/{deviceId}/simulate-payment")
    public Map<String, Object> simulatePayment(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> request) {

        if (!isMqttConnected()) {
            log.error("[ERROR] MQTT Broker is offline. Cannot simulate payment.");
            return createErrorResponse("MQTT Broker is offline. Cannot simulate payment.");
        }

        long amount = request.containsKey("amount") ? Long.parseLong(request.get("amount").toString()) : 50000;
        String orderId = "SIM-ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        String topic = String.format("iot/v1/tenant1/qrbox/%s/evt", deviceId);
        String payload = String.format(
            "{\"event\": \"payment_success\", \"amount\": %d, \"orderId\": \"%s\"}", 
            amount, orderId
        );

        log.info("--- [SIMULATOR] Simulating Payment Success from Device ---");
        log.info("Event: payment_success, OrderId: {}, Amount: {}", orderId, amount);
        
        // Giả lập gửi MQTT Event từ "Thiết bị" lên Backend
        mqttGateway.sendToMqtt(topic, 1, payload);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "simulated");
        response.put("orderId", orderId);
        response.put("amount", amount);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
}

