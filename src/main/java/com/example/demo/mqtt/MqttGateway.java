package com.example.demo.mqtt;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * MqttGateway: Cửa ngõ (Gateway) để gửi dữ liệu từ Java ra MQTT.
 * 
 * - Đây là một interface được Spring Integration tự động cung cấp bộ gõ (Proxy).
 * - Thay vì viết code gửi tin phức tạp, ta chỉ cần gọi các hàm trong này.
 * - Mọi tin nhắn truyền qua đây sẽ được đẩy vào 'mqttOutboundChannel' đã cấu hình trong MqttConfig.
 */
@Component
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttGateway {
    
    /**
     * Gửi message tới MQTT topic cụ thể.
     * @param topic Địa chỉ nhận tin (Topic).
     * @param payload Nội dung tin nhắn (Chuỗi JSON).
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String payload);

    /**
     * Gửi message tới MQTT topic cụ thể kèm theo QoS (Độ tin cậy).
     * @param qos 0: Gửi và quên; 1: Đảm bảo đến ít nhất 1 lần; 2: Đảm bảo đến đúng 1 lần.
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) int qos, String payload);
}

