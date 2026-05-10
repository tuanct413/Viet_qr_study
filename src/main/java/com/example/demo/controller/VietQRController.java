package com.example.demo.controller;

import com.example.demo.entity.QrRecord;
import com.example.demo.entity.Transaction;
import com.example.demo.repository.QrRecordRepository;
import com.example.demo.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("")
public class VietQRController {

    private static final Logger logger = LoggerFactory.getLogger(VietQRController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${jwt.secret}")
    private String JWT_SECRET;
    @Value("${vietqr.username}")
    private String VIETQR_USERNAME;
    @Value("${vietqr.password}")
    private String VIETQR_PASSWORD;
    @Value("${vietqr.token-url}")
    private String VIETQR_TOKEN_URL;
    @Value("${vietqr.generate-url}")
    private String VIETQR_GENERATE_URL;
    @Value("${vietqr.secret-key}")
    private String VIETQR_SECRET_KEY;

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private QrRecordRepository qrRecordRepo;

    // =========================================================================
    // [EXISTING] Lấy Bearer Token từ VietQR Gateway
    // =========================================================================
    @GetMapping("/api/get-token")
    public ResponseEntity<Object> getFrontendToken() {
        String token = getVietQRAccessToken();
        if (token == null) {
            return ResponseEntity.status(500).body("Không thể lấy Token từ VietQR.");
        }
        Map<String, String> tokenResp = new LinkedHashMap<>();
        tokenResp.put("access_token", token);
        return ResponseEntity.ok(tokenResp);
    }

    private String getVietQRAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String auth = VIETQR_USERNAME + ":" + VIETQR_PASSWORD;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(VIETQR_TOKEN_URL, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            logger.error("Lỗi lấy Token: {}", e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // [CHECKLIST #10] Tạo mã QR với Idempotent + Timeout handling
    //
    // Logic:
    // 1. Tính sign = HMAC_SHA256(sorted_values, secretKey)
    // 2. Kiểm tra orderId đã tồn tại trong DB chưa → trả lại QR cũ nếu có
    // 3. Gọi VietQR Gateway để tạo QR mới
    // 4. Nếu timeout → trả 504 với hướng dẫn retry cùng orderId
    // 5. Lưu QR vào MongoDB với expectedAmount để dùng khi verify webhook
    // =========================================================================
    @PostMapping("/api/generate-qr")
    public ResponseEntity<Object> generateQR(
            @RequestBody Map<String, Object> requestBody,
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Thiếu Token xác thực!");
        }

        String orderId = (String) requestBody.get("orderId");
        Long expectedAmount = requestBody.get("amount") != null
                ? Long.valueOf(requestBody.get("amount").toString()) : null;

        // --- [#10] IDEMPOTENT: Kiểm tra QR đã được tạo trước đó chưa? ---
        if (orderId != null) {
            Optional<QrRecord> existing = qrRecordRepo.findByOrderId(orderId);
            if (existing.isPresent()) {
                logger.info(">>> [IDEMPOTENT] orderId={} da ton tai - tra QR cu", orderId);
                QrRecord qr = existing.get();
                Map<String, Object> cached = new LinkedHashMap<>();
                cached.put("qrCode", qr.getQrCode());
                cached.put("qrLink", qr.getQrLink());
                cached.put("transactionRefId", qr.getTransactionRefId());
                cached.put("orderId", qr.getOrderId());
                cached.put("status", qr.getStatus());
                cached.put("cached", true);
                return ResponseEntity.ok(cached);
            }
        }

        // --- [#6] SIGNATURE: Tính sign trước khi gửi lên VietQR Gateway ---
        String sign = buildRequestSign(requestBody);
        requestBody.put("sign", sign);
        logger.info(">>> [SIGN] Generated sign={} for orderId={}", sign, orderId);

        // --- [#10] Lưu QR (Tạm) vào DB trước để lưu expectedAmount dù Gateway lỗi ---
        QrRecord qrRecord = null;
        if (orderId != null) {
            qrRecord = QrRecord.builder()
                    .orderId(orderId)
                    .expectedAmount(expectedAmount)
                    .sign(sign)
                    .status("PENDING")
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            qrRecordRepo.save(qrRecord);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(VIETQR_GENERATE_URL, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Cập nhật lại QrRecord khi có kết quả từ Gateway
                if (qrRecord != null) {
                    qrRecord.setQrCode((String) body.get("qrCode"));
                    qrRecord.setQrLink((String) body.get("qrLink"));
                    qrRecord.setTransactionRefId((String) body.get("transactionRefId"));
                    qrRecordRepo.save(qrRecord);
                    logger.info(">>> [QR SAVED] orderId={}, expectedAmount={}", orderId, expectedAmount);
                }
                return ResponseEntity.ok(body);
            }
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (ResourceAccessException e) {
            // --- [#10] TIMEOUT: VietQR chưa kịp response, hướng dẫn retry cùng orderId ---
            logger.error(">>> [TIMEOUT] VietQR Gateway timeout cho orderId={}: {}", orderId, e.getMessage());
            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("error", true);
            errorBody.put("errorReason", "GATEWAY_TIMEOUT");
            errorBody.put("message", "Gateway chưa phản hồi. Hãy thử lại sau 5 giây với cùng orderId.");
            errorBody.put("orderId", orderId);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(errorBody);

        } catch (Exception e) {
            logger.error(">>> [ERROR] Lỗi tạo mã QR: {}", e.getMessage());
            return ResponseEntity.status(500).body("Lỗi tạo mã QR: " + e.getMessage());
        }
    }

    // =========================================================================
    // [CHECKLIST #8] Polling API — Kiểm tra trạng thái QR/đơn hàng
    //
    // Frontend/IoT gọi API này mỗi 3s để biết đơn hàng đã được thanh toán chưa.
    // Trả về PENDING nếu chưa có webhook về, SUCCESS/UNDERPAID nếu đã xử lý.
    // =========================================================================
    @GetMapping("/api/qr/status")
    public ResponseEntity<Object> checkQRStatus(@RequestParam String orderId) {
        logger.info(">>> [POLLING] Check status for orderId={}", orderId);

        // 1. Kiểm tra trong transactions DB (webhook đã về chưa)
        Optional<Transaction> tx = transactionRepo.findByOrderId(orderId);
        if (tx.isPresent()) {
            Transaction t = tx.get();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderId", orderId);
            result.put("status", t.getStatus());
            result.put("amount", t.getAmount());
            result.put("expectedAmount", t.getExpectedAmount());
            result.put("transactionId", t.getTransactionId());
            result.put("createdAt", t.getCreatedAt().toString());
            logger.info(">>> [POLLING RESULT] orderId={} → status={}", orderId, t.getStatus());
            return ResponseEntity.ok(result);
        }

        // 2. Kiểm tra QR còn hạn không (QR sống 5 phút)
        Optional<QrRecord> qr = qrRecordRepo.findByOrderId(orderId);
        if (qr.isPresent()) {
            QrRecord record = qr.get();
            boolean expired = record.getCreatedAt()
                    .plusMinutes(5).isBefore(LocalDateTime.now());
            if (expired) {
                record.setStatus("EXPIRED");
                qrRecordRepo.save(record);
                Map<String, Object> expResp = new LinkedHashMap<>();
                expResp.put("orderId", orderId);
                expResp.put("status", "EXPIRED");
                expResp.put("message", "Ma QR da het han (> 5 phut)");
                return ResponseEntity.ok(expResp);
            }
            Map<String, Object> pendingResp = new LinkedHashMap<>();
            pendingResp.put("orderId", orderId);
            pendingResp.put("status", "PENDING");
            pendingResp.put("message", "Chua nhan duoc thanh toan, vui long thu lai sau.");
            return ResponseEntity.ok(pendingResp);
        }

        Map<String, Object> notFound = new LinkedHashMap<>();
        notFound.put("error", true);
        notFound.put("message", "Khong tim thay don hang: " + orderId);
        return ResponseEntity.status(404).body(notFound);
    }

    // =========================================================================
    // [EXISTING] Endpoint cấp Token cho VietQR Gateway gọi vào
    // =========================================================================
    @PostMapping(value = {"/api/merchant-get-token", "/api/token_generate"})
    public ResponseEntity<Object> issueToken(HttpServletRequest request,
                                              @RequestBody(required = false) Map<String, Object> body) {
        logger.info(">>> [UNIFIED CALL] URI: {}", request.getRequestURI());

        boolean hasSignHeader = request.getHeader("sign") != null
                || request.getHeader("signature") != null
                || request.getHeader("x-signature") != null;
        if (body != null && (body.containsKey("transactionid")
                || body.containsKey("amount")
                || body.containsKey("terminalCode")
                || hasSignHeader)) {
            return handleWebhook(request, body);
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String jwtToken = Jwts.builder()
                    .setSubject("VietQR-Mock")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 300_000))
                    .signWith(SignatureAlgorithm.HS256, JWT_SECRET.getBytes())
                    .compact();
            Map<String, Object> tokenResp2 = new LinkedHashMap<>();
            tokenResp2.put("access_token", jwtToken);
            tokenResp2.put("token_type", "Bearer");
            tokenResp2.put("expires_in", 300);
            return ResponseEntity.ok(tokenResp2);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // =========================================================================
    // [EXISTING] Endpoint nhận Webhook từ VietQR Gateway
    // =========================================================================
    @PostMapping("/bank/api/transaction-sync")
    public ResponseEntity<Object> transactionSync(HttpServletRequest request,
                                                   @RequestBody Map<String, Object> body) {
        return handleWebhook(request, body);
    }

    // =========================================================================
    // [CHECKLIST #6] Xác thực chữ ký Webhook
    // [CHECKLIST #7] Idempotency bằng transactionId
    // [CHECKLIST #9] Kiểm tra sai lệch số tiền
    // =========================================================================
    private ResponseEntity<Object> handleWebhook(HttpServletRequest request,
                                                   Map<String, Object> body) {
        // --- Log đầy đủ để trace ---
        logRequest(request, body);

        // --- Lấy sign từ header hoặc body ---
        String receivedSign = resolveSign(request, body);
        logger.info(">>> [WEBHOOK] Received sign=[{}] for orderId={}", receivedSign, body.get("orderId"));

        // ---- [#6] XÁC THỰC CHỮ KÝ WEBHOOK ----
        if (receivedSign != null && !receivedSign.isEmpty()) {
            boolean signValid = false;
            String orderId = (String) body.get("orderId");
            if (orderId != null) {
                Optional<QrRecord> qrOpt = qrRecordRepo.findByOrderId(orderId);
                if (qrOpt.isPresent() && qrOpt.get().getSign() != null) {
                    signValid = receivedSign.equals(qrOpt.get().getSign());
                }
            }

            if (!signValid) {
                logger.warn(">>> [SECURITY] INVALID SIGNATURE! Possible fake webhook. orderId={}", orderId);
                // Trả 200 OK để Gateway không retry, nhưng KHÔNG xử lý nghiệp vụ
                Map<String, Object> fakeResp = new LinkedHashMap<>();
                fakeResp.put("error", true);
                fakeResp.put("errorReason", "INVALID_SIGNATURE");
                fakeResp.put("toastMessage", "Chữ ký không hợp lệ");
                fakeResp.put("object", null);
                return ResponseEntity.ok(fakeResp);
            }
            logger.info(">>> [SECURITY] Signature VALID ✓");
        } else {
            logger.warn(">>> [SECURITY] Webhook không có chữ ký — đang chạy ở chế độ debug");
        }

        // ---- [#7] IDEMPOTENCY ----
        String txId = (String) body.get("transactionid");
        if (txId != null) {
            Optional<Transaction> existing = transactionRepo.findByTransactionId(txId);
            if (existing.isPresent()) {
                logger.info(">>> [IDEMPOTENT] Duplicate webhook txId={} — bỏ qua, trả 200 OK", txId);
                return ResponseEntity.ok(buildSuccessResponse(txId));
            }
        }

        // ---- [#9] KIỂM TRA SAI LỆCH SỐ TIỀN ----
        String orderId = (String) body.get("orderId");
        Long paidAmount = body.get("amount") != null
                ? Long.valueOf(body.get("amount").toString()) : null;
        Long expectedAmount = null;
        String finalStatus = "SUCCESS";

        if (orderId != null) {
            Optional<QrRecord> qrOpt = qrRecordRepo.findByOrderId(orderId);
            if (qrOpt.isPresent()) {
                expectedAmount = qrOpt.get().getExpectedAmount();
                if (expectedAmount != null && paidAmount != null && paidAmount < expectedAmount) {
                    finalStatus = "UNDERPAID";
                    logger.warn(">>> [AMOUNT MISMATCH] orderId={} | Expected={} | Paid={}",
                            orderId, expectedAmount, paidAmount);
                    // Cập nhật trạng thái QR
                    QrRecord qr = qrOpt.get();
                    qr.setStatus("UNDERPAID");
                    qrRecordRepo.save(qr);
                } else {
                    // Cập nhật trạng thái QR thành PAID
                    QrRecord qr = qrOpt.get();
                    qr.setStatus("PAID");
                    qrRecordRepo.save(qr);
                }
            }
        }

        // ---- Lưu giao dịch vào MongoDB ----
        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .orderId(orderId)
                .amount(paidAmount)
                .expectedAmount(expectedAmount)
                .transType((String) body.get("transType"))
                .content((String) body.get("content"))
                .bankAccount((String) body.get("bankaccount"))
                .status(finalStatus)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            transactionRepo.save(tx);
            logger.info(">>> [DB SAVED] txId={}, orderId={}, status={}", txId, orderId, finalStatus);
        } catch (Exception e) {
            // Bắt lỗi duplicate key từ MongoDB (safety net cho idempotency)
            logger.warn(">>> [IDEMPOTENT-SAFE] Duplicate save prevented for txId={}: {}", txId, e.getMessage());
            return ResponseEntity.ok(buildSuccessResponse(txId));
        }

        // ---- Nếu UNDERPAID: Không thực hiện nghiệp vụ, chỉ log ----
        if ("UNDERPAID".equals(finalStatus)) {
            logger.warn(">>> [BUSINESS] orderId={} UNDERPAID — Không kích hoạt dịch vụ!", orderId);
            // TODO: Notify operator / refund logic tại đây
        } else {
            logger.info(">>> [BUSINESS] orderId={} SUCCESS — Tiến hành kích hoạt dịch vụ...", orderId);
            // TODO: unlock IoT device, update order, notify user, etc.
        }

        return ResponseEntity.ok(buildSuccessResponse(txId));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * [#6] Xác thực chữ ký webhook.
     * Thuật toán: HMAC-SHA256(sorted_values_concat, secretKey) → Base64
     */
    private boolean verifyWebhookSignature(Map<String, Object> body, String receivedSign) {
        try {
            // Tạo sorted map, loại bỏ field "sign"
            TreeMap<String, Object> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            sorted.putAll(body);
            sorted.remove("sign");

            // Nối các giá trị (cùng logic với Gateway gửi lên)
            StringBuilder sb = new StringBuilder();
            for (Object v : sorted.values()) {
                if (v != null) {
                    sb.append(v instanceof Number
                            ? String.format("%.0f", ((Number) v).doubleValue())
                            : v.toString());
                }
            }
            String dataToVerify = sb.toString();
            String expectedSign = hmacSHA256(dataToVerify, VIETQR_SECRET_KEY);

            logger.info(">>> [SIGN] DataToVerify=[{}]", dataToVerify);
            logger.info(">>> [SIGN] Expected=[{}] | Received=[{}]", expectedSign, receivedSign);

            return receivedSign.equals(expectedSign);
        } catch (Exception e) {
            logger.error(">>> [SIGN ERROR] {}", e.getMessage());
            return false;
        }
    }

    /**
     * [#6] Tính Signature cho request tạo QR.
     * Sorted concat các giá trị field (không bao gồm sign) → HMAC-SHA256 → Base64
     */
    private String buildRequestSign(Map<String, Object> body) {
        try {
            // Dùng List thay vì TreeMap để sắp xếp các key
            List<String> keys = new ArrayList<>(body.keySet());
            keys.remove("sign"); // Bỏ trường sign (nếu có)
            Collections.sort(keys); // Sắp xếp key theo thứ tự Alphabet

            StringBuilder sb = new StringBuilder();
            // Lặp qua danh sách key đã sắp xếp để lấy value
            for (String key : keys) {
                Object v = body.get(key);
                if (v != null && !v.toString().isEmpty()) {
                    sb.append(v instanceof Number
                            ? String.format("%.0f", ((Number) v).doubleValue())
                            : v.toString());
                }
            }
            String dataToVerify = sb.toString();
            logger.info(">>> [BUILD SIGN] DataToVerify=[{}]", dataToVerify);
            return hmacSHA256(dataToVerify, VIETQR_SECRET_KEY);
        } catch (Exception e) {
            logger.error(">>> [SIGN BUILD ERROR] {}", e.getMessage());
            return "";
        }
    }

    /** Tính HMAC-SHA256, trả về chuỗi Base64 */
    private String hmacSHA256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.error("hmacSHA256 error: {}", e.getMessage());
            return null;
        }
    }

    /** Lấy chữ ký từ header hoặc body */
    private String resolveSign(HttpServletRequest request, Map<String, Object> body) {
        String sign = request.getHeader("sign");
        if (sign == null) sign = request.getHeader("signature");
        if (sign == null) sign = request.getHeader("x-signature");
        if (sign == null) sign = (String) body.get("sign");
        if (sign == null) sign = request.getParameter("sign");
        return sign;
    }

    /** Tạo response thành công chuẩn VietQR */
    private Map<String, Object> buildSuccessResponse(String txId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", false);
        response.put("errorReason", null);
        response.put("toastMessage", "Success");
        Map<String, String> obj = new LinkedHashMap<>();
        obj.put("reftransactionid", txId != null ? txId : "");
        response.put("object", obj);
        return response;
    }

    /** Log đầy đủ request để debug */
    private void logRequest(HttpServletRequest request, Map<String, Object> body) {
        try {
            logger.info(">>> [WEBHOOK BODY] {}", objectMapper.writeValueAsString(body));
        } catch (Exception ignored) {}

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            logger.debug(">>> [HEADER] {}={}", name, request.getHeader(name));
        }
    }
}
