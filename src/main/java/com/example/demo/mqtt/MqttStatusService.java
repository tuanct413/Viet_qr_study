package com.example.demo.mqtt;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttIntegrationEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.stereotype.Service;

/**
 * MqttStatusService: Trạm giám sát trạng thái kết nối MQTT.
 * Lớp này lắng nghe các sự kiện (Events) phát ra từ Spring Integration MQTT 
 * để biết lúc nào hệ thống Online hoặc Offline.
 * 
 * - Log4j2: Báo cáo trạng thái kết nối ngay lập tức lên console và file log.
 */
@Service
@Log4j2
public class MqttStatusService {

    @Getter
    private boolean connected = false;

    /**
     * Lắng nghe các sự kiện tích hợp MQTT.
     * Tự động phản ứng khi có sự thay đổi về trạng thái kết nối.
     */
    @EventListener
    public void handleMqttEvent(MqttIntegrationEvent event) {
        if (event instanceof MqttSubscribedEvent) {
            // Xảy ra khi kết nối thành công và đã đăng ký lắng nghe topic xong
            log.info(">>> [STATUS] MQTT Connected and Subscribed!");
            connected = true;
        } else if (event instanceof MqttConnectionFailedEvent) {
            // Xảy ra khi Broker bị sập hoặc sai thông tin đăng nhập
            log.error(">>> [STATUS] MQTT Connection Failed! Please check EMQX/Docker.");
            connected = false;
        }
    }
}

