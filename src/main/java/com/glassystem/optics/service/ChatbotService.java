package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.ChatbotMessageRequest;
import com.glassystem.optics.dto.response.LensResponse;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.enums.ProductVariantStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotService {

    final RestTemplate restTemplate;
    final ProductService productService;
    final ProductVariantService productVariantService;
    final LensService lensService;

    @Value("${openai.api-key:${OPENAI_API_KEY:}}")  // cấu hình openai trong .yaml
    String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    String openAiBaseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    String openAiModel;

    static final String SYSTEM_PROMPT = "Bạn là chuyên gia tư vấn kính mắt tại cửa hàng OptiCare.\n\n"  // luật cho AI
            + "⚠️ QUY TẮC BẮT BUỘC - TUYỆT ĐỐI TUÂN THỦ:\n"
            + "1. CHỈ ĐƯỢC gợi ý và recommend sản phẩm CÓ TRONG DANH SÁCH SẢN PHẨM CỦA CỬA HÀNG bên dưới.\n"
            + "2. TUYỆT ĐỐI KHÔNG được tự bịa ra, tưởng tượng, hay gợi ý bất kỳ sản phẩm nào KHÔNG CÓ trong danh sách.\n"
            + "3. Nếu không có sản phẩm nào phù hợp, hãy nói rõ cửa hàng chưa có sản phẩm phù hợp và gợi ý gần nhất trong kho.\n"
            + "4. Khi gợi ý sản phẩm, PHẢI dùng đúng dữ liệu: tên, thương hiệu, giá, màu sắc, kích thước có sẵn.\n"
            + "5. Nếu sản phẩm có biến thể, hãy liệt kê biến thể (màu/size/giá) cụ thể.\n\n"
            + "PHONG CÁCH:\n"
            + "- Thân thiện, tiếng Việt tự nhiên\n"
            + "- Trả lời ngắn gọn, dễ đọc, có bullet khi cần\n";

    public String chat(List<ChatbotMessageRequest> messages) {
        String systemMessage = SYSTEM_PROMPT + buildStoreContext();  // build 1 đoạn prompt cho AI


        List<Map<String, String>> fullMessages = new ArrayList<>();
        fullMessages.add(Map.of("role", "system", "content", systemMessage)); //mess đầu tiên lun là system (luật+data)
        for (ChatbotMessageRequest m : messages) {
            fullMessages.add(Map.of("role", m.getRole(), "content", m.getContent())); // mess của user và assistant(lưu lịch sử chat)
        }

        HttpHeaders headers = new HttpHeaders(); // cho request
        headers.setContentType(MediaType.APPLICATION_JSON); // content type là json
        if (openAiApiKey != null && !openAiApiKey.isBlank()) { //ko null,ko rỗng
            headers.setBearerAuth(openAiApiKey);
        }

        Map<String, Object> body = Map.of(
                "model", openAiModel,
                "messages", fullMessages,
                "temperature", 0.6,
                "max_tokens", 900
        ); // json request body cho openai

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers); // tạo httpEntity=body+headers

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                openAiBaseUrl + "/chat/completions",
                entity,
                Map.class
        ); //Gửi request tới OpenAI

        if (response == null) {
            return "Xin lỗi, hiện tại tôi không thể trả lời.";
        }
        //bodyJSON+header->HttpEntity->Gửi POST request tới OpenAI->Nhận JSON response->Check null → tránh lỗi

        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) { //không phải List hoặc rỗng return
            return "Xin lỗi, hiện tại tôi không thể trả lời.";
        }

        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstChoice)) { // có phải là map ko,có các field gì
            return "Xin lỗi, hiện tại tôi không thể trả lời.";
        }

        Object messageObj = firstChoice.get("message");
        if (!(messageObj instanceof Map<?, ?> msg)) { //message đúng format ko
            return "Xin lỗi, hiện tại tôi không thể trả lời.";
        }

        Object contentObj = msg.get("content"); //content là câu trả lời của AI
        return contentObj != null ? contentObj.toString() : "Xin lỗi, hiện tại tôi không thể trả lời.";
    }

    private String buildStoreContext() {
        List<ProductResponse> products = productService.getProducts();
        List<LensResponse> lenses = lensService.getLenses();

        StringBuilder sb = new StringBuilder(); // tạo đoạn chuỗi context có cấu trúc cho AI dễ đọc hiểu hơn

        sb.append("\n\n===== DANH SÁCH SẢN PHẨM HIỆN CÓ =====\n");
        int count = 0;
        for (ProductResponse p : products) {  // mỗi lần lặp → biến p là 1 sản phẩm
            if (p == null || p.getStatus() == null || !"ACTIVE".equalsIgnoreCase(p.getStatus().name())) {  //bỏ qua nếu p,status null,INACTIVE
                continue;
            }
            count++;
        sb.append("\n📦 ").append(p.getName())
                    .append(" | Brand: ").append(nullSafe(p.getBrand()))
                    .append(" | Category: ").append(p.getCategory() != null ? p.getCategory() : "N/A")
                    .append(" | Frame: ").append(nullSafe(p.getFrameType()))
                    .append(" | Material: ").append(nullSafe(p.getFrameMaterial()))
                    .append(" | Shape: ").append(nullSafe(p.getShape()))
                    .append(" | Gender: ").append(nullSafe(p.getGender()))
                    .append(" | Price: ").append(priceRange(p));

            List<ProductVariantResponse> variants = productVariantService.getActiveVariantsByProductId(p.getId());
            if (variants != null && !variants.isEmpty()) {
                sb.append("\n  - Variants:\n");
                for (ProductVariantResponse v : variants) {
                    if (v == null || v.getStatus() != ProductVariantStatus.ACTIVE) {
                        continue;
                    }
                    sb.append("    + ").append(nullSafe(v.getColorName()))
                            .append(" | Finish: ").append(nullSafe(v.getFrameFinish()))
                            .append(" | Size: ").append(nullSafe(v.getSizeLabel()))
                            .append(" | Lens/Bridge/Temple: ").append(num(v.getLensWidthMm())).append("/")
                            .append(num(v.getBridgeWidthMm())).append("/")
                            .append(num(v.getTempleLengthMm()))
                            .append(" | Price: ").append(v.getPrice() != null ? v.getPrice().toString() : "Liên hệ")
                            .append("\n");
                }
            }
            sb.append("\n");
            if (count >= 50) {
                break;
            }
        }

        if (count == 0) {
            sb.append("(Chưa có sản phẩm nào)\n");
        }

        sb.append("\n===== DANH SÁCH TRÒNG KÍNH =====\n");
        if (lenses != null && !lenses.isEmpty()) {
            for (LensResponse l : lenses) {
                sb.append("- ").append(l.getName())
                        .append(" | Material: ").append(nullSafe(l.getMaterial()))
                        .append(" | Price: ").append(l.getPrice() != null ? l.getPrice().toString() : "Liên hệ")
                        .append(" | Desc: ").append(nullSafe(l.getDescription()))
                        .append("\n");
            }
        } else {
            sb.append("(Chưa có tròng kính)\n");
        }

        return sb.toString();
    }

    private static String priceRange(ProductResponse p) {
        if (p.getMinPrice() != null && p.getMaxPrice() != null) {
            return p.getMinPrice().toString() + " - " + p.getMaxPrice().toString();
        }
        return "Liên hệ"; //1 trong 2 min hoặc max null trả về lien he
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "N/A" : s;
    }

    private static String num(Integer v) {
        return v == null ? "?" : v.toString();
    } // xu li integer có thể bị null thì trả về '?'
}
