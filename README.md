# IoT MQTT Integration Project - QR Box Simulation

## 1. Mục tiêu dự án (Goals)
Dự án này tập trung vào việc thực hành tích hợp hệ thống **Backend Spring Boot** với các thiết bị **IoT (QR Box)** thông qua giao thức truyền tin **MQTT** và lưu trữ dữ liệu vào **MongoDB**.

thực hành cách xây dựng một hệ thống điều khiển thiết bị từ xa và xử lý sự kiện thanh toán thời gian thực theo mô hình **Pub/Sub (Publish-Subscribe)**.

## 2. Công nghệ sử dụng (Technology Stack)
*   **Java Spring Boot**: Framework chính để xây dựng Backend.
*   **MQTT (EMQX Broker)**: Giao thức truyền tin siêu nhẹ cho IoT.
*   **MongoDB**: Cơ sở dữ liệu NoSQL để lưu trữ lịch sử giao dịch.
*   **Docker & Docker Compose**: Tự động hóa việc cài đặt EMQX và MongoDB.
*   **Log4j2**: Hệ thống ghi log chuyên nghiệp để theo dõi (trace) lỗi và tin nhắn.

## 3. Kiến trúc hệ thống (Architecture)
Hệ thống hoạt động theo luồng song phương:

### Luồng 1: Thiết bị báo thanh toán (Device -> Backend)
1.  **Thiết bị (IoT/Simulator)** gửi tin nhắn lên MQTT Topic: `iot/v1/tenant1/qrbox/{deviceId}/evt`.
2.  **Backend** lắng nghe tin nhắn đó qua `MqttMessageHandler`.
3.  **Hệ thống** phân tích JSON, ghi log bằng **Log4j2** và lưu vào **MongoDB**.

### Luồng 2: Điều khiển thiết bị (Backend -> Device)
1.  **Admin** gọi API `/api/iot/box/{deviceId}/control?action=open`.
2.  **Backend** gửi tin nhắn xuống MQTT Topic: `iot/v1/tenant1/qrbox/{deviceId}/cmd`.
3.  **Thiết bị** nhận lệnh và thực hiện hành động (mở/đóng cửa).

## 4. Các thành phần chính (Core Components)
*   `MqttConfig.java`: Cấu hình kết nối, kênh Inbound/Outbound và cơ chế tự động kết nối lại.
*   `IotController.java`: Chứa các API REST để điều khiển thiết bị và giả lập thanh toán.
*   `MqttMessageHandler.java`: Xử lý logic khi có tin nhắn từ thiết bị đổ về.
*   `MqttStatusService.java`: Giám sát trạng thái kết nối Online/Offline của MQTT Broker.
*   `DeviceSimulator.java`: Công cụ giả lập một thiết bị IoT thật để test mà không cần phần cứng.

## 5. Hướng dẫn chạy dự án (How to Run)

### Yêu cầu:
*   Đã cài đặt **Docker Desktop**.
*   Đã cài đặt **Java 8+** và **Maven**.

### Các bước thực hiện:
1.  **Chạy Backend**: Mở terminal tại thư mục gốc và gõ:
    ```bash
    mvn spring-boot:run
    ```
    *(Hệ thống sẽ tự động bật Docker EMQX và MongoDB cho bạn).*

2.  **Chạy Simulator (Nếu muốn test luồng thiết bị)**:
    ```bash
    mvn exec:java
    ```

## 6. Danh sách API chính

| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| **POST** | `/api/iot/box/{deviceId}/control?action=open` | Gửi lệnh mở hộp xuống thiết bị qua MQTT |
| **POST** | `/api/iot/box/{deviceId}/simulate-payment` | Giả lập thiết bị báo thanh toán thành công (Body: `{"amount": 100000}`) |

## 7. Giám sát hệ thống (Monitoring)
*   **Logs**: Kiểm tra file `logs/api.log` để xem vết tin nhắn MQTT và lỗi của hệ thống.
*   **MQTT Dashboard**: Truy cập `http://localhost:18083` (User: `admin`, Pass: `public`) để xem trạng thái Broker EMQX.
*   **Database**: Dùng **MongoDB Compass** kết nối `mongodb://localhost:27017` để xem dữ liệu giao dịch.

---
*Dự án này giúp bạn nắm vững cách làm việc với IoT, giao thức MQTT và tư duy xử lý dữ liệu hướng sự kiện.*
