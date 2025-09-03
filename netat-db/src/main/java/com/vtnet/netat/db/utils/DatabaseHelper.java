package com.vtnet.netat.db.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vtnet.netat.db.config.DatabaseProfile;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class DatabaseHelper {
    private static final String PROFILE_REPO_PATH = Paths.get(
            System.getProperty("user.dir"), "src", "test", "resources", "database_profiles").toString();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DatabaseHelper() {}

    public static DatabaseProfile getProfile(String relativeProfilePath) {
        String fullPath = Paths.get(PROFILE_REPO_PATH, relativeProfilePath + ".json").toString();
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get(fullPath)));
            return MAPPER.readValue(jsonContent, DatabaseProfile.class);
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file database profile: " + fullPath, e);
        }
    }
}