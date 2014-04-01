package com.tomtom.photon.runner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tomtom.photon.tools.zonemaker.Params;

final class FetchRunner implements Callable<Void> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FetchRunner.class);
	public static final List<String> ADM_MODE_CONTINENTS = Arrays.asList("NAM");
	/**
	 * 
	 */
	private final List<ContinentSettings> continents;
	private final String out;
	private final String countryConfig;
	private final String accessPointWs;
	private final String zoningService;

	protected boolean finished = false;  


	protected FetchRunner(List<ContinentSettings> continents, String out, String countryConfig, String accessPointWs, String zoningService) {
		super();
		this.continents = continents;
		this.out = out;
		this.countryConfig = countryConfig;
		this.accessPointWs = accessPointWs;
		this.zoningService = zoningService;
	}

	@Override
	public Void call() throws Exception {
		fetch();
		return null;
	}
	
	private void fetch() throws IOException {
		File fetchOut = new File(out, PhotonRunner.FETCHED_DIR);
		fetchOut.mkdirs();
		for (ContinentSettings con : continents) {
			LOGGER.debug("Fetch: " + con.getName());
			File doneMarker = new File(fetchOut, con.getName() + ".done");
			if (doneMarker.exists()) {
				LOGGER.info("Already fetched skipping.");
				continue;
			}
			Params p = new Params();
			p.setCountryConfig(countryConfig);
			p.setEndpointUrl(accessPointWs);
			p.setOutputDir(fetchOut.getAbsolutePath());
			p.setJournalVersion(con.getTransactionVersion());
			p.setRegionName(con.getName());
			p.setWorkMode(Params.WORK_MODE.FETCH);
			p.setZoneType(Params.ZONE_TYPE.COUNTRY);
			p.setZoningUrl(zoningService);
			p.setRegionVersion(con.getVersion());
			if (ADM_MODE_CONTINENTS.contains(con.getName())) {
				p.setAdministrativeLevel(Params.ADMINISTRATIVE_LEVEL.ORDER1);
			}
			// ZoneMaker zm = new ZoneMaker(p);
			// zm.run();
			doneMarker.createNewFile();
			LOGGER.debug("Done: " + con.getName());
		}
		finished = true;
		LOGGER.info("Finished");
	}
}