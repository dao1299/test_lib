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
            info.setSubCategory(annotation.subCategory());
            info.setExample(annotation.example());

            // Xử lý returnValue (định dạng mới: "Type - Description")
            info.setReturnValue(annotation.returnValue());

            // Xử lý note (thay thế cho prerequisites, exceptions, platform, systemImpact, stability, tags)
            info.setNote(annotation.note());

            // Thêm thông tin về lớp và phương thức
            info.setClassName(method.getDeclaringClass().getName());
            info.setMethodName(method.getName());
            info.setReturnType(method.getReturnType().getSimpleName());

            // 2. Xử lý thông tin tham số (định dạng mới)
            List<ParameterInfo> parameterInfos = new ArrayList<>();

            // Lấy các thông tin tham số từ annotation (định dạng mới: "name: Type - Description")
            String[] annotationParams = annotation.parameters();

            // Lấy thông tin từ phương thức thực tế
            Parameter[] methodParams = method.getParameters();

            // Xử lý trường hợp parameters rỗng (chỉ có {})
            if (annotationParams.length == 1 && annotationParams[0].trim().isEmpty()) {
                // Không có tham số, chỉ sử dụng thông tin từ phương thức nếu có
                for (Parameter parameter : methodParams) {
                    ParameterInfo paramInfo = new ParameterInfo();
                    paramInfo.setType(parameter.getType().getSimpleName());
                    paramInfo.setName(parameter.getName());
                    paramInfo.setDescription("");
                    parameterInfos.add(paramInfo);
                }
            } else if (annotationParams.length == methodParams.length) {
                // Số lượng tham số từ annotation khớp với số lượng tham số của phương thức
                for (int i = 0; i < methodParams.length; i++) {
                    ParameterInfo paramInfo = new ParameterInfo();
                    paramInfo.setType(methodParams[i].getType().getSimpleName());
                    paramInfo.setName(methodParams[i].getName());

                    String annotationParamDesc = annotationParams[i];
                    if (annotationParamDesc != null && !annotationParamDesc.trim().isEmpty()) {

                        String[] parts = annotationParamDesc.split(" - ", 2);
                        if (parts.length > 1) {
                            paramInfo.setDescription(parts[1]);
                        } else {
                            paramInfo.setDescription("");
                        }

                        // Lấy tên và type từ phần đầu
                        String nameAndType = parts[0];
                        String[] nameTypeParts = nameAndType.split(": ", 2);
                        if (nameTypeParts.length > 1) {
                            // Có thể override tên nếu cần
                            // paramInfo.setName(nameTypeParts[0]);
                            // Type đã được lấy từ method parameter
                        }
                    } else {
                        paramInfo.setDescription("");
                    }

                    parameterInfos.add(paramInfo);
                }
            } else {
                // Nếu không khớp, chỉ sử dụng thông tin từ phương thức
                for (Parameter parameter : methodParams) {
                    ParameterInfo paramInfo = new ParameterInfo();
                    paramInfo.setType(parameter.getType().getSimpleName());
                    paramInfo.setName(parameter.getName());
                    paramInfo.setDescription("");
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
