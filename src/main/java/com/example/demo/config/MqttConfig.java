package com.example.demo.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * MqttConfig: Cấu hình hệ thống MQTT cho ứng dụng.
 * File này đóng vai trò là "Tổng đài liên lạc", thiết lập cách thức Backend nói chuyện với IoT.
 * 
 * - MQTT: Sử dụng để truyền tin nhắn nhanh, nhẹ giữa Server và Thiết bị (QR Box).
 * - Log4j2: Được sử dụng xuyên suốt để theo dõi trạng thái kết nối và vết tin nhắn (Tracing).
 */
@Configuration
@IntegrationComponentScan("com.example.demo.mqtt")
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.default-topic}")
    private String defaultTopic;

    /**
     * Cấu hình kết nối tới Broker (EMQX).
     * Thiết lập các thông số như Username, Password, và cơ chế tự động kết nối lại.
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { brokerUrl });
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        // Tự động kết nối lại: Quan trọng để hệ thống tự phục hồi khi Broker khởi động chậm hoặc mất mạng
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        factory.setConnectionOptions(options);
        return factory;
    }

    // --- LUỒNG GỬI TIN (OUTBOUND: Backend -> Device) ---

    /**
     * Kênh dẫn tin nhắn từ Code Java ra ngoài.
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * Xử lý việc đẩy tin nhắn lên Broker MQTT.
     * Log4j2 sẽ ghi lại vết khi lệnh được gửi đi thành công.
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId + "-out", mqttClientFactory());
        messageHandler.setAsync(true); // Gửi bất đồng bộ để không làm treo API
        messageHandler.setDefaultTopic("iot/v1/tenant1/qrbox/default/cmd");
        return messageHandler;
    }

    // --- LUỒNG NHẬN TIN (INBOUND: Device -> Backend) ---

    /**
     * Kênh nhận tin nhắn từ thiết bị đổ về Backend.
     */
    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * Adapter đóng vai trò "người nghe" (Listener).
     * Nó liên tục nghe trên các topic Event (evt) để phát hiện thanh toán.
     */
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "-in", mqttClientFactory(),
                        defaultTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1); // Đảm bảo tin nhắn thanh toán được gửi đến ít nhất 1 lần
        adapter.setOutputChannel(mqttInboundChannel());
        
        // Cơ chế tự phục hồi kết nối Inbound sau mỗi 5 giây
        adapter.setRecoveryInterval(5000);
        return adapter;
    }
}

}
