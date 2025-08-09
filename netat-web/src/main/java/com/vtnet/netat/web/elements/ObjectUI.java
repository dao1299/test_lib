    package com.vtnet.netat.web.elements;

    import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
    import com.fasterxml.jackson.databind.ObjectMapper;

    import java.util.Collections;
    import java.util.Comparator;
    import java.util.List;
    import java.util.stream.Collectors;

    /**
     * Đại diện cho một đối tượng (element) trên giao diện người dùng (UI).
     * Mỗi ObjectUI chứa một tên định danh và một danh sách các 'Locator'
     * để tìm thấy đối tượng đó trên các nền tảng hoặc trong các điều kiện khác nhau.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class ObjectUI {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        private String name;
        private List<Locator> locators;

        /**
         * Constructor mặc định.
         */
        public ObjectUI() {
            this.locators = Collections.emptyList();
        }

        /**
         * Constructor với các tham số cần thiết.
         *
         * @param name     Tên của đối tượng UI, ví dụ: "loginButton", "usernameInput".
         * @param locators Danh sách các đối tượng Locator để tìm thấy phần tử này.
         */
        public ObjectUI(String name, List<Locator> locators) {
            this.name = name;
            this.locators = locators;
        }

        // --- Getters and Setters ---

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Locator> getLocators() {
            return locators;
        }

        public void setLocators(List<Locator> locators) {
            this.locators = locators;
        }

        // --- Convenience Methods ---

        /**
         * Cung cấp một danh sách các locator đang hoạt động (`active=true`)
         * và đã được sắp xếp theo độ ưu tiên (`priority`).
         * <p>
         * Phương thức này rất hữu ích cho các lớp Keyword, giúp chúng không cần
         * tự thực hiện logic lọc và sắp xếp.
         *
         * @return Một danh sách các Locator đã được lọc và sắp xếp.
         */
        public List<Locator> getActiveAndSortedLocators() {
            if (locators == null || locators.isEmpty()) {
                return Collections.emptyList();
            }

            return locators.stream()
                    .filter(Locator::isActive) // Chỉ lấy các locator đang hoạt động
                    .sorted(Comparator.comparingInt(Locator::getPriority)) // Sắp xếp theo priority
                    .collect(Collectors.toList());
        }

        /**
         * Cung cấp một chuỗi đại diện cho đối tượng, hữu ích cho việc gỡ lỗi (debugging).
         *
         * @return Chuỗi mô tả ObjectUI.
         */
        @Override
        public String toString() {
            return "ObjectUI{" +
                    "name='" + name + '\'' +
                    ", locators=" + (locators != null ? locators.size() : 0) + " locators" +
                    '}';
        }

        /**
         * Chuyển đổi đối tượng hiện tại thành chuỗi JSON.
         *
         * @return Chuỗi JSON đại diện cho đối tượng này
         * @throws RuntimeException nếu có lỗi trong quá trình chuyển đổi JSON
         */
        public String toJson() {
            try {
                return objectMapper.writeValueAsString(this);
            } catch (Exception e) {
                throw new RuntimeException("Error converting ObjectUI to JSON", e);
            }
        }


    }
