package com.vtnet.netat.core.keywords;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import io.qameta.allure.Step;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cung cấp các keyword tiện ích chung để xử lý dữ liệu, chuỗi, ngày tháng,
 * và các tác vụ hệ thống khác.
 */
public class UtilityKeyword extends BaseKeyword {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @NetatKeyword(
            name = "getCurrentDateTime",
            description = "Lấy và trả về chuỗi ngày giờ hiện tại theo một định dạng cho trước.",
            category = "Utility/DateTime",
            parameters = {"String: dateTimeFormat - Định dạng ngày giờ (ví dụ: 'dd/MM/yyyy HH:mm:ss')."},
            example = "String timestamp = utilityKeyword.getCurrentDateTime(\"yyyy-MM-dd_HH-mm-ss\");"
    )
    @Step("Lấy ngày giờ hiện tại với định dạng: {0}")
    public String getCurrentDateTime(String dateTimeFormat) {
        return execute(() -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
            return now.format(formatter);
        }, dateTimeFormat);
    }

    @NetatKeyword(
            name = "extractTextByRegex",
            description = "Trích xuất một phần của chuỗi văn bản dựa trên một biểu thức chính quy (regex) và một nhóm (group) cụ thể.",
            category = "Utility/String",
            parameters = {
                    "String: text - Chuỗi văn bản nguồn.",
                    "String: regex - Biểu thức chính quy để tìm kiếm.",
                    "int: group - Chỉ số của nhóm cần trích xuất (ví dụ: 1 cho nhóm đầu tiên)."
            },
            example = "// Lấy mã đơn hàng '12345' từ chuỗi 'Mã đơn hàng của bạn là DH-12345'\n" +
                    "utilityKeyword.extractTextByRegex(text, \"DH-(\\\\d+)\", 1);"
    )
    @Step("Trích xuất văn bản từ '{0}' bằng regex '{1}'")
    public String extractTextByRegex(String text, String regex, int group) {
        return execute(() -> {
            Pattern patt = Pattern.compile(regex);
            Matcher mat = patt.matcher(text);
            if (mat.find()) {
                return mat.group(group);
            }
            logger.warn("Không tìm thấy kết quả khớp với regex '{}' trong chuỗi văn bản.", regex);
            return null; // Trả về null nếu không tìm thấy
        }, text, regex, group);
    }

    @NetatKeyword(
            name = "getValueFromJson",
            description = "Lấy một giá trị từ một chuỗi JSON bằng cách sử dụng cú pháp JSON Pointer (ví dụ: '/user/name').",
            category = "Utility/String",
            parameters = {
                    "String: jsonString - Chuỗi JSON nguồn.",
                    "String: jsonPointer - Đường dẫn đến giá trị cần lấy."
            },
            example = "String userName = utilityKeyword.getValueFromJson(json, \"/data/user/name\");"
    )
    @Step("Lấy giá trị từ JSON với đường dẫn: {1}")
    public String getValueFromJson(String jsonString, String jsonPointer) {
        return execute(() -> {
            try {
                JsonNode rootNode = objectMapper.readTree(jsonString);
                JsonNode targetNode = rootNode.at(jsonPointer);
                return targetNode.isMissingNode() ? null : targetNode.asText();
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi xử lý JSON: " + e.getMessage(), e);
            }
        }, jsonString, jsonPointer);
    }

    @NetatKeyword(
            name = "getValueFromXml",
            description = "Lấy một giá trị từ một chuỗi XML bằng cách sử dụng một biểu thức XPath.",
            category = "Utility/String",
            parameters = {
                    "String: xmlString - Chuỗi XML nguồn.",
                    "String: xpathExpression - Biểu thức XPath để tìm giá trị."
            },
            example = "String bookTitle = utilityKeyword.getValueFromXml(xml, \"//book[@id='bk101']/title/text()\");"
    )
    @Step("Lấy giá trị từ XML với XPath: {1}")
    public String getValueFromXml(String xmlString, String xpathExpression) {
        return execute(() -> {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.compile(xpathExpression).evaluate(doc, XPathConstants.NODESET);
                if (nodeList != null && nodeList.getLength() > 0) {
                    return nodeList.item(0).getNodeValue();
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi xử lý XML: " + e.getMessage(), e);
            }
        }, xmlString, xpathExpression);
    }

    @NetatKeyword(
            name = "executeCommand",
            description = "Thực thi một lệnh trên command line của hệ điều hành và chờ cho đến khi nó hoàn thành.",
            category = "Utility/Command",
            parameters = {"String...: command - Lệnh và các tham số của nó."},
            example = "utilityKeyword.executeCommand(\"taskkill\", \"/F\", \"/IM\", \"chrome.exe\");"
    )
    @Step("Thực thi lệnh: {0}")
    public void executeCommand(String... command) {
        execute(() -> {
            try {
                Process process = new ProcessBuilder(command).start();
                int exitCode = process.waitFor();
                logger.info("Lệnh '{}' đã thực thi với mã thoát: {}", String.join(" ", command), exitCode);
                if (exitCode != 0) {
                    logger.warn("Lệnh có thể đã không thực thi thành công.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi thực thi lệnh command line.", e);
            }
            return null;
        }, (Object[]) command);
    }
}