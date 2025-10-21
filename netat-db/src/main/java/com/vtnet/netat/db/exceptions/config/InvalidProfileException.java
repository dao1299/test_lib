package com.vtnet.netat.db.exceptions.config;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when attempting to use a database profile that doesn't exist.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class InvalidProfileException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    private final String requestedProfile;
    private final List<String> availableProfiles;

    protected InvalidProfileException(Builder builder) {
        super(builder);
        this.requestedProfile = builder.requestedProfile;
        this.availableProfiles = new ArrayList<>(builder.availableProfiles);
    }

    /**
     * Gets the profile name that was requested.
     *
     * @return requested profile name
     */
    public String getRequestedProfile() {
        return requestedProfile;
    }

    /**
     * Gets the list of available profiles.
     *
     * @return list of available profile names
     */
    public List<String> getAvailableProfiles() {
        return new ArrayList<>(availableProfiles);
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append(String.format("Database profile '%s' not found.\n\n", requestedProfile));

        if (!availableProfiles.isEmpty()) {
            suggestion.append("Available profiles:\n");
            availableProfiles.forEach(profile ->
                    suggestion.append(String.format("  - %s\n", profile)));
            suggestion.append("\n");

            // Suggest similar profile names (simple Levenshtein-like matching)
            List<String> similar = findSimilarProfiles(requestedProfile, availableProfiles);
            if (!similar.isEmpty()) {
                suggestion.append("Did you mean:\n");
                similar.forEach(profile ->
                        suggestion.append(String.format("  - %s\n", profile)));
                suggestion.append("\n");
            }
        } else {
            suggestion.append("No database profiles are currently configured.\n\n");
        }

        suggestion.append("Resolution Steps:\n");
        suggestion.append("1. Check profile name spelling\n");
        suggestion.append("2. Verify profile JSON file exists in database_profiles directory\n");
        suggestion.append("3. Ensure profile is properly formatted\n");
        suggestion.append("4. Check profile name in test configuration\n");

        return suggestion.toString();
    }

    /**
     * Finds profile names similar to the requested one.
     *
     * @param requested the requested profile name
     * @param available list of available profiles
     * @return list of similar profile names
     */
    private List<String> findSimilarProfiles(String requested, List<String> available) {
        List<String> similar = new ArrayList<>();
        String reqLower = requested.toLowerCase();

        for (String profile : available) {
            String profLower = profile.toLowerCase();

            // Check for substring match
            if (profLower.contains(reqLower) || reqLower.contains(profLower)) {
                similar.add(profile);
                continue;
            }

            // Check for similar prefix
            int commonPrefix = 0;
            for (int i = 0; i < Math.min(reqLower.length(), profLower.length()); i++) {
                if (reqLower.charAt(i) == profLower.charAt(i)) {
                    commonPrefix++;
                } else {
                    break;
                }
            }

            if (commonPrefix >= 3) {
                similar.add(profile);
            }
        }

        return similar;
    }

    /**
     * Creates a new builder for InvalidProfileException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for InvalidProfileException.
     */
    public static class Builder extends ConfigurationException.Builder {
        private String requestedProfile;
        private List<String> availableProfiles = new ArrayList<>();

        public Builder() {
            severity(ErrorSeverity.ERROR);
        }

        public Builder requestedProfile(String requestedProfile) {
            this.requestedProfile = requestedProfile;
            addContext("requestedProfile", requestedProfile);
            return this;
        }

        public Builder availableProfiles(List<String> availableProfiles) {
            this.availableProfiles = new ArrayList<>(availableProfiles);
            return this;
        }

        @Override
        public InvalidProfileException build() {
            if (requestedProfile != null) {
                message(String.format("Database profile '%s' not found", requestedProfile));
            }
            return new InvalidProfileException(this);
        }
    }
}