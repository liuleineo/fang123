package com.fang123.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fang123.config.AiProperties;
import com.fang123.config.TokenHubProperties;
import com.fang123.dto.AiParseResult;
import com.fang123.dto.AiParseResult.ParsedFields;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiParseService {

    private final AiProperties aiProperties;
    private final TokenHubProperties tokenHubProperties;
    private final CosService cosService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /** AI 解析提示词 */
    private static final String PARSE_PROMPT = """
            你是一个楼盘信息提取助手。请从以下OCR识别的文本中提取楼盘信息，返回严格的JSON格式（不要包含```json标记，直接返回纯JSON）。

            需要提取的字段（无法确定的设为null）：
            {
              "projectName": "楼盘名称",
              "shortName": "楼盘简称",
              "district": "行政区（如余杭区、西湖区）",
              "plate": "板块（如未来科技城、钱江世纪城）",
              "avgUnitPrice": "均价，元/㎡，纯数字",
              "areaMin": "最小户型面积，㎡，纯数字",
              "areaMax": "最大户型面积，㎡，纯数字",
              "buildArea": "总建面，㎡，纯数字",
              "landArea": "占地面积，㎡，纯数字",
              "projectCompany": "开发公司",
              "brandList": "开发品牌，多个用逗号分隔",
              "projectAddress": "楼盘地址",
              "salesAddress": "售楼部位置/售楼处地址",
              "houseType": "楼盘类型：1=住宅,2=公寓,3=商铺,4=别墅",
              "decorateType": "装修类型：1=精装,2=毛坯,3=简装",
              "buildingTotal": "楼栋总数，纯数字",
              "houseTotal": "总户数，纯数字",
              "greenRate": "绿地率，%，纯数字",
              "plotRatio": "容积率，纯数字",
              "propertyCompany": "物业公司",
              "deliveryDate": "交房时间，格式YYYY-MM-DD",
              "salesTel": "售楼电话",
              "propertyRightYear": "产权年限，纯数字",
              "minTotalPrice": "最低总价，万元，纯数字",
              "maxTotalPrice": "最高总价，万元，纯数字",
              "priceTag": "价格标签，如改善、刚需",
              "landNo": "宗地编号",
              "landPrice": "拿地总价，万元，纯数字",
              "landUnitPrice": "楼面价，元/㎡，纯数字",
              "propertyFeeHigh": "小高/洋房物业费，元/㎡·月，纯数字",
              "propertyFeeVilla": "排屋别墅物业费，元/㎡·月，纯数字",
              "parkTotal": "总车位数，纯数字",
              "parkSellNum": "可售车位数，纯数字",
              "parkRatio": "车位配比，文本格式如1:1.2",
              "facadeMaterial": "外立面材料",
              "selfHoldRate": "自持率，%，纯数字",
              "floorHeightMin": "最低层高，m，纯数字",
              "floorHeightMax": "最高层高，m，纯数字",
              "floorMin": "最低楼层数，纯数字",
              "floorMax": "最高楼层数，纯数字",
              "longitude": "经度",
              "latitude": "纬度",
              "eduSupport": "教育配套",
              "trafficSupport": "交通配套",
              "medicalSupport": "医疗配套",
              "businessSupport": "商业配套",
              "viewSupport": "景观配套",
              "communityFacility": "小区配套",
              "showHouseDesc": "样板房说明"
            }

            规则：
            1. 只返回JSON，不要有任何其他文字
            2. 无法确定的字段值设为null
            3. 数字型字段只返回数字，严格去掉任何单位（元、㎡、万、%等）
            4. 如果看到范围值如"90-140㎡"，areaMin=90, areaMax=140
            5. 文本型字段保持原文

            OCR文本：
            """;

    /**
     * 上传图片到 COS，调用 OCR 识别文字，调用 TokenHub 大模型解析楼盘字段
     */
    public AiParseResult parse(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("请至少上传一张图片");
        }

        List<String> imageUrls = new ArrayList<>();
        StringBuilder ocrTextBuilder = new StringBuilder();

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            try {
                byte[] imageBytes = file.getBytes();
                String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

                try {
                    String url = cosService.uploadFile(file, "ai-loupan");
                    imageUrls.add(url);
                    log.info("AI parse: image {} uploaded to COS", i + 1);
                } catch (Exception e) {
                    log.error("AI parse: COS upload failed for image {}", i + 1, e);
                    imageUrls.add(null);
                }

                String text = doOcrWithBase64(imageBase64);
                ocrTextBuilder.append("【图片").append(i + 1).append("】\n").append(text).append("\n\n");
                log.info("AI parse: OCR done for image {}, {} chars", i + 1, text.length());
            } catch (Exception e) {
                log.error("AI parse: failed for image {}", i + 1, e);
                ocrTextBuilder.append("【图片").append(i + 1).append("】处理失败：").append(e.getMessage()).append("\n\n");
            }
        }

        String ocrText = ocrTextBuilder.toString().trim();
        if (ocrText.isEmpty()) {
            throw new RuntimeException("所有图片OCR识别均失败，请检查图片清晰度");
        }

        // 调用 TokenHub 大模型解析
        ParsedFields fields;
        try {
            fields = doTokenHubParse(ocrText);
            log.info("AI parse: TokenHub parsing done");
        } catch (Exception e) {
            log.error("AI parse: TokenHub parsing failed", e);
            throw new RuntimeException("AI解析失败: " + e.getMessage());
        }

        AiParseResult result = new AiParseResult();
        result.setImageUrls(imageUrls);
        result.setOcrText(ocrText);
        result.setFields(fields);
        return result;
    }

    /** 调用腾讯云 OCR 通用印刷体识别（base64 方式） */
    private String doOcrWithBase64(String imageBase64) throws TencentCloudSDKException {
        Credential cred = new Credential(aiProperties.getSecretId(), aiProperties.getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ocr.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        OcrClient client = new OcrClient(cred, aiProperties.getRegion(), clientProfile);

        GeneralBasicOCRRequest req = new GeneralBasicOCRRequest();
        req.setImageBase64(imageBase64);

        GeneralBasicOCRResponse resp = client.GeneralBasicOCR(req);
        StringBuilder sb = new StringBuilder();
        if (resp.getTextDetections() != null) {
            for (var detection : resp.getTextDetections()) {
                if (detection.getDetectedText() != null) {
                    sb.append(detection.getDetectedText()).append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    /** 调用 TokenHub OpenAI 兼容接口解析楼盘字段 */
    private ParsedFields doTokenHubParse(String ocrText) throws JsonProcessingException {
        String url = tokenHubProperties.getBaseUrl() + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + tokenHubProperties.getApiKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", tokenHubProperties.getModel());
        body.put("stream", false);
        body.put("temperature", 0.1);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是一个专业的楼盘信息提取助手。请严格按照JSON格式返回提取结果，不要添加任何额外的文字或标记。"));
        messages.add(Map.of("role", "user", "content", PARSE_PROMPT + ocrText));
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.debug("TokenHub request: model={}, text length={}", tokenHubProperties.getModel(), ocrText.length());
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");
        log.debug("TokenHub response: {}", content);

        // 清理可能的 markdown 代码块标记
        content = content.trim();
        if (content.startsWith("```json")) content = content.substring(7);
        if (content.startsWith("```")) content = content.substring(3);
        if (content.endsWith("```")) content = content.substring(0, content.length() - 3);
        content = content.trim();

        return objectMapper.readValue(content, ParsedFields.class);
    }
}
