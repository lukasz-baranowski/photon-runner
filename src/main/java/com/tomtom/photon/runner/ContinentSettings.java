package com.tomtom.photon.runner;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

/**
 * Continent setting to run photon-converter
 * 
 * Sample format of continents.cfg:
 * RWR;13.10;562a90ee-fe27-40ef-ad1a-01463bf45662:1000000;AS_,AF_,AN_,SA_,OC_,EU_;
 * 
 * @author baranows
 */
public class ContinentSettings {
    private final String name;
    private final String version;
    private final UUID branch;
    private final Long transactionVersion;
    private final List<String> countries;

    public static ContinentSettings build(String line) {
        Builder builder = ContinentSettings.builder();
        String [] args = line.split(";");
        builder.name(args[0]);
        builder.version(args[1]);

        String [] branchAndVersion = args[2].split(":");
        builder.branch(branchAndVersion[0]);
        builder.transactionVersion(branchAndVersion[1]);

        builder.countries(args[3]);
        return builder.build();
    }
    
    public static Builder builder() {
        return new Builder();
    }

    private ContinentSettings(final Builder builder) {
        this.name = builder.name;
        this.version = builder.version;
        this.branch = builder.branch;
        this.transactionVersion = builder.transactionVersion;
        this.countries = builder.countries;
    }
    
    public static class Builder {
        private String name;
        private String version;
        private UUID branch;
        private Long transactionVersion;
        private List<String> countries;

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

        public Builder transactionVersion(final String transactionVersion) {
            this.transactionVersion = Long.valueOf(transactionVersion);
            return this;
        }
        public Builder countries(final String countries) {
            String [] countriesTab = countries.split(",");
            this.countries = Lists.newArrayList(countriesTab);
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
    
    public Long getTransactionVersion() {
        return transactionVersion;
    }
    
    public List<String> getCountries() {
        return countries;
    }

    public String getBranchAndVersion() {
        return branch.toString() + ":" + transactionVersion.toString();
    }

}
