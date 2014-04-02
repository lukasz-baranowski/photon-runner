package com.tomtom.photon.runner.test;

import java.util.UUID;

import junit.framework.TestCase;

import com.tomtom.photon.runner.ContinentSettings;


public class ContinentSettingsBuilderTest extends TestCase {

    private final String line = "RWR;13.10;562a90ee-fe27-40ef-ad1a-01463bf45662:1000000;";
    
    public void testBuilder() {
        ContinentSettings continentSettings = ContinentSettings.build(line);
        
        assertEquals("RWR", continentSettings.getName());
        assertEquals("13.10", continentSettings.getVersion());
        assertTrue(continentSettings.getBranch().compareTo(UUID.fromString("562a90ee-fe27-40ef-ad1a-01463bf45662")) == 0);
        assertEquals("1000000", continentSettings.getJournalVersion().toString());
        assertEquals("562a90ee-fe27-40ef-ad1a-01463bf45662:1000000", continentSettings.getBranchAndVersion());
    }
}
