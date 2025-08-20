package com.vtnet.netat.tools.gendoc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vtnet.netat.core.annotations.NetatKeyword;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class KeywordMetadataGenerator {

    public static void main(String[] args) throws Exception {
        // 1. Quét toàn bộ package "com.vtnet.netat" để tìm các keyword
        Reflections reflections = new Reflections("com.vtnet.netat", new MethodAnnotationsScanner());
        Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(NetatKeyword.class);

        List<KeywordInfo> keywordInfos = new ArrayList<>();


        for (Method method : annotatedMethods) {
            NetatKeyword annotation = method.getAnnotation(NetatKeyword.class);

            KeywordInfo info = new KeywordInfo();
            info.setName(annotation.name());
            info.setDescription(annotation.description());
            info.setCategory(annotation.category());
            info.setExample(annotation.example());

            // 2. Tự động trích xuất thông tin tham số từ chính phương thức
            List<ParameterInfo> parameterInfos = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                ParameterInfo paramInfo = new ParameterInfo();
                paramInfo.setType(parameter.getType().getSimpleName()); // Lấy tên kiểu dữ liệu (vd: String, ObjectUI)
                paramInfo.setName(parameter.getName()); // Lấy tên tham số (vd: url, uiObject)
                parameterInfos.add(paramInfo);
            }
            info.setParameters(parameterInfos);

            keywordInfos.add(info);
        }

        // 3. Chuyển đổi danh sách thành chuỗi JSON và ghi ra file
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Format cho JSON đẹp hơn

        // Ghi file vào thư mục gốc của project
        File outputFile = new File("keywords.json");
        mapper.writeValue(outputFile, keywordInfos);

        System.out.println("Đã sinh thành công file metadata tại: " + outputFile.getAbsolutePath());
    }
}
