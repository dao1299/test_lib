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
import java.util.Arrays;
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

            // Thêm các trường mới từ annotation
            info.setReturnValue(annotation.returnValue());
            info.setPrerequisites(Arrays.asList(annotation.prerequisites()));
            info.setExceptions(Arrays.asList(annotation.exceptions()));
            info.setPlatform(annotation.platform());
            info.setSystemImpact(annotation.systemImpact());
            info.setStability(annotation.stability());
            info.setTags(Arrays.asList(annotation.tags()));

            // Thêm thông tin về lớp và phương thức
            info.setClassName(method.getDeclaringClass().getName());
            info.setMethodName(method.getName());
            info.setReturnType(method.getReturnType().getSimpleName());

            // 2. Xử lý thông tin tham số
            List<ParameterInfo> parameterInfos = new ArrayList<>();

            // Trước tiên, lấy các thông tin tham số từ annotation
            String[] annotationParams = annotation.parameters();

            // Sau đó, kết hợp với thông tin từ phương thức thực tế
            Parameter[] methodParams = method.getParameters();

            // Nếu số lượng tham số từ annotation khớp với số lượng tham số của phương thức
            if (annotationParams.length == methodParams.length) {
                for (int i = 0; i < methodParams.length; i++) {
                    ParameterInfo paramInfo = new ParameterInfo();
                    paramInfo.setType(methodParams[i].getType().getSimpleName());
                    paramInfo.setName(methodParams[i].getName());

                    // Phân tích chuỗi mô tả tham số từ annotation
                    String annotationParamDesc = annotationParams[i];
                    if (annotationParamDesc != null && !annotationParamDesc.isEmpty()) {
                        // Thường có định dạng "Type: name - description"
                        String[] parts = annotationParamDesc.split(" - ", 2);
                        if (parts.length > 0) {
                            String typeAndName = parts[0];
                            String[] typeNameParts = typeAndName.split(": ", 2);
                            if (typeNameParts.length > 1) {
                                paramInfo.setDescription(parts.length > 1 ? parts[1] : "");
                            }
                        }
                    }

                    parameterInfos.add(paramInfo);
                }
            } else {
                // Nếu không khớp, chỉ sử dụng thông tin từ phương thức
                for (Parameter parameter : methodParams) {
                    ParameterInfo paramInfo = new ParameterInfo();
                    paramInfo.setType(parameter.getType().getSimpleName());
                    paramInfo.setName(parameter.getName());
                    parameterInfos.add(paramInfo);
                }
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
        System.out.println("Tổng số keyword đã xử lý: " + keywordInfos.size());
    }
}
