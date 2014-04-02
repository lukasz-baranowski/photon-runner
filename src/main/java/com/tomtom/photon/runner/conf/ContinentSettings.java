package com.tomtom.photon.runner.conf;

import java.util.UUID;

/**
 * Continent setting to run photon-converter
 * 
 * Sample format of continents.cfg:
 * RWR;13.10;562a90ee-fe27-40ef-ad1a-01463bf45662:1000000
 * 
 * @author baranows
 */
public class ContinentSettings {
    private final String name;
    private final String version;
    private final UUID branch;
    private final Long journalVersion;

    public static ContinentSettings build(String line) {
        Builder builder = ContinentSettings.builder();
        String [] args = line.split(";");
        builder.name(args[0]);
        builder.version(args[1]);

        String [] branchAndVersion = args[2].split(":");
        builder.branch(branchAndVersion[0]);
        builder.journalVersion(branchAndVersion[1]);

        return builder.build();
    }
    
    public static Builder builder() {
        return new Builder();
    }

    private ContinentSettings(final Builder builder) {
        this.name = builder.name;
        this.version = builder.version;
        this.branch = builder.branch;
        this.journalVersion = builder.journalVersion;
    }

    public static class Builder {
        private String name;
        private String version;
        private UUID branch;
        private Long journalVersion;

        public Builder name(final String name) {
            this.name = name.toUpperCase();
            return this;
        }

        public Builder version(final String version) {
            this.version = version;
            return this;
        }

        public Builder branch(final String branch) {
            this.branch = UUID.fromString(branch);
            return this;
        }

        public Builder journalVersion(final String journalVersion) {
            this.journalVersion = Long.valueOf(journalVersion);
            return this;
        }

        public ContinentSettings build() {
            return new ContinentSettings(this);
        }
    }

    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public UUID getBranch() {
        return branch;
    }
    
    public Long getJournalVersion() {
        return journalVersion;
    }

    public String getBranchAndVersion() {
        return branch.toString() + ":" + journalVersion.toString();
    }

}
