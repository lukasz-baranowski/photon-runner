package com.tomtom.photon.runner.conf;

import java.util.Arrays;
import java.util.List;

import com.tomtom.photon.tools.zonemaker.Params;


public class ZoneMakerConf {
    public static final List<String> ADM_MODE_CONTINENTS = Arrays.asList("NAM");
    
    private String out;
    private String countryConfig;
    private String accessPointWs;
    private String zoningService;
    
    public Params createBasicParams(Params.WORK_MODE mode) {
        Params p = new Params();
        p.setCountryConfig(countryConfig);
        p.setEndpointUrl(accessPointWs);
        p.setWorkMode(mode);
        p.setZoneType(Params.ZONE_TYPE.COUNTRY);
        p.setZoningUrl(zoningService);
        return p;
    }

    public static ZoneMakerConf valueOf(String out, String countryConfig, String accessPointWs, String zoningService) {
        return new ZoneMakerConf(out, countryConfig, accessPointWs, zoningService);
    }

    private ZoneMakerConf(String out, String countryConfig, String accessPointWs, String zoningService) {
        this.out = out;
        this.countryConfig = countryConfig;
        this.accessPointWs = accessPointWs;
        this.zoningService = zoningService;
    }

    public String getOut() {
        return out;
    }

    public String getCountryConfig() {
        return countryConfig;
    }

    public String getAccessPointWs() {
        return accessPointWs;
    }

    public String getZoningService() {
        return zoningService;
    }

}
