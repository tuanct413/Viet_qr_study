package com.example.demo.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Scanner;

/**
 * MÔ PHỎNG THIẾT BỊ (QR BOX)
 * Đây là một class độc lập, không cần chạy Spring.
 * Nó mô phỏng một thiết bị IoT nhận lệnh từ Backend và gửi event thanh toán.
 */
/**
 * MÔ PHỎNG THIẾT BỊ (QR BOX)
 * Đây là một class độc lập, giúp ta test hệ thống mà không cần phần cứng thật.
 * 
 * - MQTT Client: Tự đóng vai trò là một thiết bị IoT kết nối vào Broker.
 * - Subscribe: Nghe lệnh từ Backend (Topic: .../cmd).
 * - Publish: Gửi báo cáo thanh toán thành công lên Backend (Topic: .../evt).
 */
public class DeviceSimulator {

    public static void main(String[] args) {
        String broker = "tcp://localhost:1883";
        String clientId = "qr-box-001"; // ID định danh của thiết bị
        String tenantId = "tenant1";

        // Topic để nhận lệnh Đóng/Mở từ Backend
        String cmdTopic = String.format("iot/v1/%s/qrbox/%s/cmd", tenantId, clientId);
        // Topic để gửi báo cáo thanh toán thành công lên Backend
        String evtTopic = String.format("iot/v1/%s/qrbox/%s/evt", tenantId, clientId);

        try {
            // Khởi tạo MQTT Client với bộ nhớ tạm (MemoryPersistence)
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            System.out.println("Connecting to broker: " + broker);
            client.connect(options);
            System.out.println("Connected!");

            // 1. Đăng ký lắng nghe Topic lệnh từ Backend (Subscribe)
            client.subscribe(cmdTopic, (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("\n[DEVICE RECV] Lệnh từ Backend: " + payload);
                if (payload.contains("open")) {
                    System.out.println(">>> ĐANG THỰC HIỆN MỞ CỬA BOX...");
                } else if (payload.contains("close")) {
                    System.out.println(">>> ĐANG THỰC HIỆN ĐÓNG CỬA BOX...");
                }
            });

            System.out.println("Subscribed to: " + cmdTopic);
            System.out.println("Gõ 'pay' để giả lập gửi Event thanh toán thành công, 'exit' để thoát.");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if ("pay".equalsIgnoreCase(input)) {
                    // Giả lập nội dung tin nhắn thanh toán
                    String eventPayload = "{\"event\": \"payment_success\", \"amount\": 50000, \"orderId\": \"ORDER-TEST-001\"}";
                    MqttMessage message = new MqttMessage(eventPayload.getBytes());
                    message.setQos(1); // Đảm bảo Backend nhận được tin nhắn
                    
                    // Gửi tin nhắn lên Broker (Publish)
                    client.publish(evtTopic, message);
                    System.out.println("[DEVICE SEND] Đã báo cáo thanh toán thành công lên Server!");
                } else if ("exit".equalsIgnoreCase(input)) {
                    break;
                }
            }

            client.disconnect();
            System.out.println("Disconnected.");
            System.exit(0);

        } catch (MqttException e) {
            System.err.println("Lỗi MQTT Simulator: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

