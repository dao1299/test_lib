package com.vtnet.netat.core.keywords;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.utils.SecureText;
import io.qameta.allure.Step;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cung cấp các keyword tiện ích chung để xử lý dữ liệu, chuỗi, ngày tháng,
 * và các tác vụ hệ thống khác.
 */
public class UtilityKeyword extends BaseKeyword {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @NetatKeyword(name = "getCurrentDateTime", description = "Lấy và trả về chuỗi ngày giờ hiện tại theo một định dạng cho trước. " + "Sử dụng các mẫu định dạng chuẩn của Java như 'yyyy' cho năm, 'MM' cho tháng, " + "'dd' cho ngày, 'HH' cho giờ (24h), 'mm' cho phút và 'ss' cho giây.", category = "Utility",subCategory="DateTime", parameters = {"dateTimeFormat: String - Định dạng ngày giờ (ví dụ: 'dd/MM/yyyy HH:mm:ss')"}, returnValue = "String - Chuỗi ngày giờ hiện tại theo định dạng được chỉ định", example = "// Lấy ngày giờ hiện tại với định dạng yyyy-MM-dd_HH-mm-ss\n" + "String timestamp = utilityKeyword.getCurrentDateTime(\"yyyy-MM-dd_HH-mm-ss\");\n" + "// Kết quả có thể là: 2025-09-15_11-30-45", note = "Áp dụng cho tất cả nền tảng. Có thể throw IllegalArgumentException nếu định dạng ngày giờ không hợp lệ.")
    @Step("Get current date and time with format: {0}")
    public String getCurrentDateTime(String dateTimeFormat) {
        return execute(() -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
            return now.format(formatter);
        }, dateTimeFormat);
    }

    @NetatKeyword(name = "extractTextByRegex", description = "Trích xuất một phần của chuỗi văn bản dựa trên một biểu thức chính quy (regex) và một nhóm (group) cụ thể. " + "Phương thức này tìm kiếm sự xuất hiện đầu tiên của mẫu regex trong chuỗi văn bản và trả về giá trị của nhóm được chỉ định.", category = "Utility",subCategory="String", parameters = {"text: String - Chuỗi văn bản nguồn cần trích xuất dữ liệu", "regex: String - Biểu thức chính quy để tìm kiếm mẫu trong chuỗi văn bản", "group: int - Chỉ số của nhóm cần trích xuất (0 cho toàn bộ kết quả khớp, 1 cho nhóm đầu tiên, v.v.)"}, returnValue = "String - Chuỗi văn bản được trích xuất từ nhóm được chỉ định, hoặc null nếu không tìm thấy kết quả khớp", example = "// Lấy mã đơn hàng '12345' từ chuỗi 'Mã đơn hàng của bạn là DH-12345'\n" + "String orderCode = utilityKeyword.extractTextByRegex(\"Mã đơn hàng của bạn là DH-12345\", \"DH-(\\\\d+)\", 1);\n" + "// orderCode sẽ có giá trị \"12345\"", note = "Áp dụng cho tất cả nền tảng. Có thể throw PatternSyntaxException nếu biểu thức chính quy không hợp lệ, " + "hoặc IndexOutOfBoundsException nếu chỉ số nhóm không tồn tại.")
    @Step("Extract text from '{0}' using regex '{1}'")
    public String extractTextByRegex(String text, String regex, int group) {
        return execute(() -> {
            Pattern patt = Pattern.compile(regex);
            Matcher mat = patt.matcher(text);
            if (mat.find()) {
                return mat.group(group);
            }
            logger.warn("No match found for regex '{}' in the input text.", regex);
            return null; // Trả về null nếu không tìm thấy
        }, text, regex, group);
    }

    @NetatKeyword(name = "getValueFromJson", description = "Lấy một giá trị từ một chuỗi JSON bằng cách sử dụng cú pháp JSON Pointer. " + "JSON Pointer là một chuỗi bắt đầu bằng dấu gạch chéo (/) và tiếp theo là tên thuộc tính, " + "cho phép truy cập vào các phần tử lồng nhau trong cấu trúc JSON.", category = "Utility",subCategory="String", parameters = {"jsonString: String - Chuỗi JSON nguồn cần truy vấn", "jsonPointer: String - Đường dẫn đến giá trị cần lấy theo cú pháp JSON Pointer (ví dụ: '/user/name', '/data/0/id')"}, returnValue = "String - Giá trị được truy xuất dưới dạng chuỗi, hoặc null nếu không tìm thấy", example = "// Giả sử có chuỗi JSON: {\"data\":{\"user\":{\"name\":\"John Doe\",\"age\":30}}}\n" + "String userName = utilityKeyword.getValueFromJson(jsonString, \"/data/user/name\");\n" + "// userName sẽ có giá trị \"John Doe\"", note = "Áp dụng cho tất cả nền tảng. Có thể throw JsonProcessingException nếu chuỗi JSON không hợp lệ, " + "hoặc IllegalArgumentException nếu JSON Pointer không hợp lệ.")
    @Step("Get value from JSON with path: {1}")
    public String getValueFromJson(String jsonString, String jsonPointer) {
        return execute(() -> {
            try {
                JsonNode rootNode = objectMapper.readTree(jsonString);
                JsonNode targetNode = rootNode.at(jsonPointer);
                return targetNode.isMissingNode() ? null : targetNode.asText();
            } catch (Exception e) {
                throw new RuntimeException("Error processing JSON: " + e.getMessage(), e);
            }
        }, jsonString, jsonPointer);
    }

    @NetatKeyword(name = "getValueFromXml", description = "Lấy một giá trị từ một chuỗi XML bằng cách sử dụng một biểu thức XPath. " + "XPath là một ngôn ngữ truy vấn cho phép chọn các nút trong tài liệu XML dựa trên các tiêu chí khác nhau " + "như đường dẫn, thuộc tính, và vị trí.", category = "Utility",subCategory="String", parameters = {"xmlString: String - Chuỗi XML nguồn cần truy vấn", "xpathExpression: String - Biểu thức XPath để tìm và trích xuất giá trị"}, returnValue = "String - Giá trị được truy xuất từ tài liệu XML, hoặc null nếu không tìm thấy", example = "// Giả sử có chuỗi XML: <books><book id=\"bk101\"><title>XML Developer's Guide</title></book></books>\n" + "String bookTitle = utilityKeyword.getValueFromXml(xmlString, \"//book[@id='bk101']/title/text()\");\n" + "// bookTitle sẽ có giá trị \"XML Developer's Guide\"", note = "Áp dụng cho tất cả nền tảng. Có thể throw SAXException nếu chuỗi XML không hợp lệ, " + "hoặc XPathExpressionException nếu biểu thức XPath không hợp lệ.")
    @Step("Get value from XML with XPath: {1}")
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
                throw new RuntimeException("Error processing XML: " + e.getMessage(), e);
            }
        }, xmlString, xpathExpression);
    }

    @NetatKeyword(name = "executeCommand", description = "Thực thi một lệnh trên command line của hệ điều hành và chờ cho đến khi nó hoàn thành. " + "Phương thức này cho phép thực thi các lệnh hệ thống từ trong kịch bản kiểm thử, hữu ích cho các tác vụ " + "như khởi động/dừng dịch vụ, xóa tệp, hoặc các tác vụ hệ thống khác.", category = "Utility",subCategory="Command", parameters = {"command: String... - Lệnh và các tham số của nó, mỗi phần tử trong mảng là một phần riêng biệt của lệnh"}, returnValue = "void - Không trả về giá trị", example = "// Dừng tất cả các tiến trình Chrome trên Windows\n" + "utilityKeyword.executeCommand(\"taskkill\", \"/F\", \"/IM\", \"chrome.exe\");\n\n" + "// Liệt kê các tệp trong thư mục hiện tại trên Linux/Mac\n" + "utilityKeyword.executeCommand(\"ls\", \"-la\");", note = "Áp dụng cho tất cả nền tảng. Cần quyền thực thi lệnh trên hệ điều hành. " + "Có thể throw IOException nếu có lỗi khi thực thi lệnh, hoặc SecurityException nếu không có quyền thực thi lệnh.")
    @Step("Execute command: {0}")
    public void executeCommand(String... command) {
        execute(() -> {
            try {
                Process process = new ProcessBuilder(command).start();
                int exitCode = process.waitFor();
                logger.info("Command '{}' executed with exit code: {}", String.join(" ", command), exitCode);
                if (exitCode != 0) {
                    logger.warn("The command may not have executed successfully.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Error executing command line.", e);
            }
            return null;
        }, (Object[]) command);
    }

    @NetatKeyword(
            name = "generateRandomString",
            description = "Tạo ra một chuỗi ký tự ngẫu nhiên với độ dài và loại ký tự được chỉ định.",
            category = "Utility",
            subCategory = "Data Generation",
            parameters = {
                    "length: int - Độ dài mong muốn của chuỗi.",
                    "type: String - Loại ký tự: 'ALPHABETIC' (chỉ chữ), 'NUMERIC' (chỉ số), hoặc 'ALPHANUMERIC' (cả chữ và số)."
            },
            returnValue = "String|Chuỗi ngẫu nhiên đã được tạo ra.",
            example = "String randomEmail = utilityKeyword.generateRandomString(10, \"ALPHANUMERIC\") + \"@test.com\";"
    )
    @Step("Generate random string of length {0} with type {1}")
    public String generateRandomString(int length, String type) {
        return execute(() -> {
            switch (type.toUpperCase()) {
                case "ALPHABETIC":
                    return RandomStringUtils.randomAlphabetic(length);
                case "NUMERIC":
                    return RandomStringUtils.randomNumeric(length);
                case "ALPHANUMERIC":
                default:
                    return RandomStringUtils.randomAlphanumeric(length);
            }
        }, length, type);
    }

    @NetatKeyword(
            name = "generateRandomIntegerNumber",
            description = "Tạo ra một số nguyên ngẫu nhiên trong một khoảng cho trước (bao gồm cả min và max).",
            category = "Utility",
            subCategory = "Data Generation",
            parameters = {
                    "min: int - Giá trị nhỏ nhất của khoảng.",
                    "max: int - Giá trị lớn nhất của khoảng."
            },
            returnValue = "int|Số nguyên ngẫu nhiên đã được tạo ra.",
            example = "int randomAge = utilityKeyword.generateRandomNumber(18, 65);"
    )
    @Step("Generate random number between {0} and {1}")
    public int generateRandomIntegerNumber(int min, int max) {
        return execute(() -> ThreadLocalRandom.current().nextInt(min, max + 1), min, max);
    }


    @NetatKeyword(
            name = "modifyDateTime",
            description = "Thực hiện cộng hoặc trừ một khoảng thời gian (ngày, tháng, năm...) vào một mốc thời gian gốc và trả về kết quả dưới dạng chuỗi đã được định dạng.",
            category = "Utility",
            subCategory = "DateTime",
            parameters = {
                    "baseDateTimeString: String - Mốc thời gian gốc dưới dạng chuỗi, hoặc điền 'NOW' để sử dụng thời gian hiện tại.",
                    "inputFormat: String - Định dạng của mốc thời gian gốc (ví dụ: 'dd/MM/yyyy'). Bỏ trống nếu mốc thời gian gốc là 'NOW'.",
                    "amount: int - Số lượng đơn vị thời gian cần cộng (số dương) hoặc trừ (số âm).",
                    "unit: String - Đơn vị thời gian. Hỗ trợ: YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS.",
                    "outputFormat: String - Định dạng mong muốn cho chuỗi kết quả trả về."
            },
            returnValue = "String|Chuỗi ngày giờ mới sau khi đã tính toán, được định dạng theo outputFormat.",
            example = "// Lấy ngày của 7 ngày sau kể từ hôm nay\n" +
                    "utilityKeyword.modifyDateTime(\"NOW\", \"\", 7, \"DAYS\", \"dd/MM/yyyy\");\n\n" +
                    "// Lấy ngày của 3 tháng trước ngày '15/09/2025'\n" +
                    "utilityKeyword.modifyDateTime(\"15/09/2025\", \"dd/MM/yyyy\", -3, \"MONTHS\", \"dd/MM/yyyy\");",
            note = "Tham số 'unit' không phân biệt chữ hoa/thường. Nếu định dạng ngày tháng đầu vào không chính xác, keyword sẽ gây ra lỗi."
    )
    @Step("Modify date-time: {0} by {2} {3}")
    public String modifyDateTime(String baseDateTimeString, String inputFormat, int amount, String unit, String outputFormat) {
        return execute(() -> {
            LocalDateTime baseDateTime;

            // Xác định ngày giờ gốc
            if ("NOW".equalsIgnoreCase(baseDateTimeString)) {
                baseDateTime = LocalDateTime.now();
            } else {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat);
                baseDateTime = LocalDateTime.parse(baseDateTimeString, inputFormatter);
            }

            // Chuyển đổi đơn vị từ String sang ChronoUnit
            ChronoUnit chronoUnit;
            try {
                chronoUnit = ChronoUnit.valueOf(unit.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported time unit: " + unit + ". Supported values are: YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS.");
            }

            // Thực hiện phép tính cộng/trừ
            LocalDateTime resultDateTime = baseDateTime.plus(amount, chronoUnit);

            // Định dạng và trả về kết quả
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
            return resultDateTime.format(outputFormatter);

        }, baseDateTimeString, inputFormat, amount, unit, outputFormat);
    }
}
