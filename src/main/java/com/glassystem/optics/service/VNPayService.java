package com.glassystem.optics.service;

import com.glassystem.optics.configuration.VNPayConfig;
import com.glassystem.optics.entity.Payment;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    public String createPaymentUrl(Payment payment, String returnUrlBase) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = String.valueOf(payment.getId());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);

        // Số tiền nhân 100 theo quy định VNPay
        long amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrder().getId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrlBase + VNPayConfig.vnp_Returnurl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Sắp xếp các tham số theo Alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build Hash Data: fieldName=fieldValue (Đã encode)
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    // Build Query String: fieldName=fieldValue (Đã encode)
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()))
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    if (i < fieldNames.size() - 1) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            return "";
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return VNPayConfig.vnp_PayUrl + "?" + queryUrl;
    }

    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // Khi verify, VNPay yêu cầu hash lại các field nhận được (đã sort) để so sánh
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = fields.get(fieldName);
            sb.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
            if (i < fieldNames.size() - 1) {
                sb.append("&");
            }
        }

        String signValue = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());
        if (signValue.equalsIgnoreCase(vnp_SecureHash)) {
            return "00".equals(request.getParameter("vnp_TransactionStatus")) ? 1 : 0;
        } else {
            return -1; // Sai chữ ký
        }
    }

    public boolean refund(Payment payment, String requestId, String ipAddress) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "refund";
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
        String vnp_TransactionType = "02"; // 02: Hoàn tiền toàn phần, 03: Hoàn tiền một phần
        String vnp_TxnRef = payment.getId();
        long amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String vnp_OrderInfo = "Hoan tien don hang " + vnp_TxnRef;

        // VNPay yêu cầu định dạng yyyyMMddHHmmss
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        String vnp_TransactionDate = vnp_CreateDate; // Thường lấy bằng CreateDate nếu không lưu TransactionDate riêng
        String vnp_CreateBy = "Admin_System";

        // Tạo dữ liệu để Hash
        String hashData = requestId + "|" + vnp_Version + "|" + vnp_Command + "|" + vnp_TmnCode + "|" +
                vnp_TransactionType + "|" + vnp_TxnRef + "|" + amount + "|" +
                vnp_TransactionDate + "|" + vnp_CreateDate + "|" + vnp_CreateBy + "|" + vnp_OrderInfo;

        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData);

        // Build Body (Sử dụng Map để Jackson/Gson tự convert sang JSON)
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("vnp_RequestId", requestId);
        requestParams.put("vnp_Version", vnp_Version);
        requestParams.put("vnp_Command", vnp_Command);
        requestParams.put("vnp_TmnCode", vnp_TmnCode);
        requestParams.put("vnp_TransactionType", vnp_TransactionType);
        requestParams.put("vnp_TxnRef", vnp_TxnRef);
        requestParams.put("vnp_Amount", String.valueOf(amount));
        requestParams.put("vnp_OrderInfo", vnp_OrderInfo);
        requestParams.put("vnp_TransactionDate", vnp_TransactionDate);
        requestParams.put("vnp_CreateDate", vnp_CreateDate);
        requestParams.put("vnp_CreateBy", vnp_CreateBy);
        requestParams.put("vnp_IpAddr", ipAddress);
        requestParams.put("vnp_SecureHash", vnp_SecureHash);

        // Lưu ý: Bạn cần dùng RestTemplate hoặc WebClient để gọi POST tới VNPayConfig.vnp_ApiUrl
        // Giả sử sử dụng RestTemplate:
         ResponseEntity<Map> response = restTemplate.postForEntity(VNPayConfig.vnp_ApiUrl, requestParams, Map.class);
         return "00".equals(response.getBody().get("vnp_ResponseCode"));

        System.out.println("Gửi yêu cầu hoàn tiền cho đơn: " + vnp_TxnRef + " với Hash: " + vnp_SecureHash);
        return true; // Mock trả về true
    }
}