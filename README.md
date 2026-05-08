# IoT & VietQR Payment Integration System
> **Dự án:** `Viet_qr_study` — Backend Spring Boot xử lý thanh toán VietQR và điều khiển thiết bị IoT qua MQTT.

## 1. Tổng quan (Project Overview)
Dự án này là một giải pháp tích hợp thanh toán tự động qua mã **VietQR** (chuẩn EMVCo) để kích hoạt các thiết bị **IoT** (như QR Box, máy bán hàng tự động).

**Các tính năng cốt lõi:**
*   **VietQR Gateway:** Khởi tạo mã QR động, xác thực chữ ký HMAC-SHA256, xử lý Webhook IPN từ ngân hàng.
*   **IoT Control:** Điều khiển thiết bị từ xa qua giao thức **MQTT (EMQX)**.
*   **Persistence:** Lưu trữ lịch sử giao dịch và trạng thái đơn hàng vào **MongoDB**.

---

## 2. Công nghệ sử dụng (Technology Stack)
*   **Java 8 / Spring Boot 2.7.x**
*   **MongoDB**: Lưu trữ giao dịch và bản ghi QR.
*   **MQTT (EMQX Broker)**: Truyền tin thời gian thực cho IoT.
*   **Docker Compose**: Tự động hóa cài đặt hạ tầng (MongoDB, EMQX, Ngrok).
*   **Security**: HMAC-SHA256 Signature Verification & Two-way Webhook Authentication.

---

## 3. Hướng dẫn cài đặt & Chạy dự án (Getting Started)

### Yêu cầu hệ thống:
*   **Java 8** hoặc mới hơn.
*   **Maven** 3.x.
*   **Docker Desktop** (Để chạy Database và Broker).

### Các bước thực hiện:

1.  **Cấu hình VietQR Keys:**
    Mở file `src/main/resources/application.properties` và điền thông tin Sandbox/Production:
    ```properties
    vietqr.username=your_access_key
    vietqr.password=your_secret_key
    vietqr.secret-key=your_secret_key
    ```

2.  **Khởi động ứng dụng:**
    Chạy lệnh sau tại thư mục gốc:
    ```bash
    mvn spring-boot:run
    ```
    *(Hệ thống sẽ tự động bật các container MongoDB, EMQX và Ngrok Tunnel).*

3.  **Xem Báo cáo Kỹ thuật:**
    Mọi chi tiết về kiến trúc, luồng dữ liệu (Sequence Diagram) và hướng dẫn bảo mật đều có trong file:
    👉 [TECHNICAL_REPORT.md](./TECHNICAL_REPORT.md)

---

## 4. Danh sách API chính

### A. Nghiệp vụ VietQR
| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| **POST** | `/api/generate-qr` | Khởi tạo mã QR động cho đơn hàng |
| **GET** | `/api/qr/status` | Polling trạng thái đơn hàng (orderId) |
| **POST** | `/bank/api/transaction-sync` | Webhook nhận thông báo tiền về (Xác thực Sign) |

### B. Điều khiển IoT
| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| **POST** | `/api/iot/box/{deviceId}/control` | Gửi lệnh (open/close) xuống thiết bị qua MQTT |

---

## 5. Hướng dẫn Kiểm thử (Testing Guide)

### Giả lập Webhook thành công (Simulate IPN):
Bạn có thể dùng Postman để gửi một request mẫu vào endpoint Webhook để kiểm tra logic xác thực:

**Request:** `POST http://localhost:8081/bank/api/transaction-sync`  
**Headers:**  
- `Content-Type`: `application/json`  
- `sign`: `Obvk2s0C1dEmyJH3nkOL1+TTrQ9T2o7Y6/BRlvemtVc=` (Ví dụ)

**Body:**
```json
{
    "transactionid": "TX_TEST_DONE_01",
    "amount": 50000,
    "orderId": "ORD_TEST_DONE_01",
    "content": "TEST THANH TOAN",
    "bankaccount": "0002086343228",
    "transType": "C"
}
```

---

## 6. Giám sát (Monitoring)
*   **Logs:** Xem log tại console hoặc file `logs/application.log`.
*   **MQTT Dashboard:** `http://localhost:18083` (User: `admin`, Pass: `public`).
*   **Database:** `mongodb://localhost:27017` (Database: `product_db`).
