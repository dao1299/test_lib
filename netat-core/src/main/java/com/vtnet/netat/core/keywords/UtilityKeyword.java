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
            description = "Lấy và trả về chuỗi ngày giờ hiện tại theo một định dạng cho trước. Sử dụng các mẫu định dạng chuẩn của Java như 'yyyy' cho năm, 'MM' cho tháng, 'dd' cho ngày, 'HH' cho giờ (24h), 'mm' cho phút và 'ss' cho giây.",
            category = "Utility/DateTime",
            parameters = {"String: dateTimeFormat - Định dạng ngày giờ (ví dụ: 'dd/MM/yyyy HH:mm:ss')."},
            returnValue = "String: Chuỗi ngày giờ hiện tại theo định dạng được chỉ định",
            example = "// Lấy ngày giờ hiện tại với định dạng yyyy-MM-dd_HH-mm-ss\n" +
                    "String timestamp = utilityKeyword.getCurrentDateTime(\"yyyy-MM-dd_HH-mm-ss\");\n" +
                    "// Kết quả có thể là: 2025-09-15_11-30-45",
            prerequisites = {},
            exceptions = {"IllegalArgumentException: Nếu định dạng ngày giờ không hợp lệ"},
            platform = "ALL",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"datetime", "utility", "formatting"}
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
            description = "Trích xuất một phần của chuỗi văn bản dựa trên một biểu thức chính quy (regex) và một nhóm (group) cụ thể. Phương thức này tìm kiếm sự xuất hiện đầu tiên của mẫu regex trong chuỗi văn bản và trả về giá trị của nhóm được chỉ định.",
            category = "Utility/String",
            parameters = {
                    "String: text - Chuỗi văn bản nguồn cần trích xuất dữ liệu.",
                    "String: regex - Biểu thức chính quy để tìm kiếm mẫu trong chuỗi văn bản.",
                    "int: group - Chỉ số của nhóm cần trích xuất (0 cho toàn bộ kết quả khớp, 1 cho nhóm đầu tiên, v.v.)."
            },
            returnValue = "String: Chuỗi văn bản được trích xuất từ nhóm được chỉ định, hoặc null nếu không tìm thấy kết quả khớp",
            example = "// Lấy mã đơn hàng '12345' từ chuỗi 'Mã đơn hàng của bạn là DH-12345'\n" +
                    "String orderCode = utilityKeyword.extractTextByRegex(\"Mã đơn hàng của bạn là DH-12345\", \"DH-(\\\\d+)\", 1);\n" +
                    "// orderCode sẽ có giá trị \"12345\"",
            prerequisites = {},
            exceptions = {"PatternSyntaxException: Nếu biểu thức chính quy không hợp lệ",
                    "IndexOutOfBoundsException: Nếu chỉ số nhóm không tồn tại"},
            platform = "ALL",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"string", "regex", "extraction", "pattern-matching"}
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
            description = "Lấy một giá trị từ một chuỗi JSON bằng cách sử dụng cú pháp JSON Pointer. JSON Pointer là một chuỗi bắt đầu bằng dấu gạch chéo (/) và tiếp theo là tên thuộc tính, cho phép truy cập vào các phần tử lồng nhau trong cấu trúc JSON.",
            category = "Utility/String",
            parameters = {
                    "String: jsonString - Chuỗi JSON nguồn cần truy vấn.",
                    "String: jsonPointer - Đường dẫn đến giá trị cần lấy theo cú pháp JSON Pointer (ví dụ: '/user/name', '/data/0/id')."
            },
            returnValue = "String: Giá trị được truy xuất dưới dạng chuỗi, hoặc null nếu không tìm thấy",
            example = "// Giả sử có chuỗi JSON: {\"data\":{\"user\":{\"name\":\"John Doe\",\"age\":30}}}\n" +
                    "String userName = utilityKeyword.getValueFromJson(jsonString, \"/data/user/name\");\n" +
                    "// userName sẽ có giá trị \"John Doe\"",
            prerequisites = {},
            exceptions = {"JsonProcessingException: Nếu chuỗi JSON không hợp lệ",
                    "IllegalArgumentException: Nếu JSON Pointer không hợp lệ"},
            platform = "ALL",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"json", "parsing", "data-extraction"}
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
            description = "Lấy một giá trị từ một chuỗi XML bằng cách sử dụng một biểu thức XPath. XPath là một ngôn ngữ truy vấn cho phép chọn các nút trong tài liệu XML dựa trên các tiêu chí khác nhau như đường dẫn, thuộc tính, và vị trí.",
            category = "Utility/String",
            parameters = {
                    "String: xmlString - Chuỗi XML nguồn cần truy vấn.",
                    "String: xpathExpression - Biểu thức XPath để tìm và trích xuất giá trị."
            },
            returnValue = "String: Giá trị được truy xuất từ tài liệu XML, hoặc null nếu không tìm thấy",
            example = "// Giả sử có chuỗi XML: <books><book id=\"bk101\"><title>XML Developer's Guide</title></book></books>\n" +
                    "String bookTitle = utilityKeyword.getValueFromXml(xmlString, \"//book[@id='bk101']/title/text()\");\n" +
                    "// bookTitle sẽ có giá trị \"XML Developer's Guide\"",
            prerequisites = {},
            exceptions = {"SAXException: Nếu chuỗi XML không hợp lệ",
                    "XPathExpressionException: Nếu biểu thức XPath không hợp lệ"},
            platform = "ALL",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"xml", "xpath", "parsing", "data-extraction"}
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
            description = "Thực thi một lệnh trên command line của hệ điều hành và chờ cho đến khi nó hoàn thành. Phương thức này cho phép thực thi các lệnh hệ thống từ trong kịch bản kiểm thử, hữu ích cho các tác vụ như khởi động/dừng dịch vụ, xóa tệp, hoặc các tác vụ hệ thống khác.",
            category = "Utility/Command",
            parameters = {"String...: command - Lệnh và các tham số của nó, mỗi phần tử trong mảng là một phần riêng biệt của lệnh."},
            returnValue = "void: Không trả về giá trị",
            example = "// Dừng tất cả các tiến trình Chrome trên Windows\n" +
                    "utilityKeyword.executeCommand(\"taskkill\", \"/F\", \"/IM\", \"chrome.exe\");\n\n" +
                    "// Liệt kê các tệp trong thư mục hiện tại trên Linux/Mac\n" +
                    "utilityKeyword.executeCommand(\"ls\", \"-la\");",
            prerequisites = {"Quyền thực thi lệnh trên hệ điều hành"},
            exceptions = {"IOException: Nếu có lỗi khi thực thi lệnh",
                    "SecurityException: Nếu không có quyền thực thi lệnh"},
            platform = "ALL",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"command", "system", "shell", "process"}
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
