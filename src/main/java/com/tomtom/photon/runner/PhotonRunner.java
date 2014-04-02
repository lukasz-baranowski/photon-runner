package com.tomtom.photon.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.teleatlas.global.common.cli.AbstractArgs4jTool;

public class PhotonRunner extends AbstractArgs4jTool {
	private static final Logger LOGGER = LoggerFactory.getLogger(PhotonRunner.class);

	public static String FETCHED_DIR = "fetched";
	public static String SENT_DIR = "sent";
	public static String DONE_DIR = "done";

	@Option(name = "--continents", usage = "Sets continents config file", aliases = "-c", required = true)
	private File continentsFile;

	@Option(name = "--countryConfig", usage = "Sets country_config.xml file", aliases = "-cc", required = true)
	private String countryConfig;

	@Option(name = "--accessPoint", usage = "Sets access-point-ws url", aliases = "-ap", required = true)
	private String accessPointWs;

	@Option(name = "--zoningservice", usage = "Sets zoningservice url", aliases = "-zs", required = true)
	private String zoningService;

	@Option(name = "--out", usage = "Exchange dir", aliases = "-out", required = true)
	private String out;

	@Option(name = "--hadoopConfig", usage = "Sets hadoop config dir", aliases = "-hcd", required = true)
	private String hadoopConfig;

	@Option(name = "--photonConverterJar", usage = "Sets photonConverterJar jar", aliases = "-pcj", required = true)
	private String photonConverterJar;

	@Option(name = "--jobConfig", usage = "Sets job-config.xml file", aliases = "-jc", required = true)
	private String jobConfig;

	public void run() {
		try {
			if (!continentsFile.exists()) {
				throw new IllegalArgumentException("No such file: " + this.continentsFile.getAbsolutePath());
			}
			final List<ContinentSettings> continents = readConfig(this.continentsFile);

			ExecutorService pool = Executors.newFixedThreadPool(3);

			FetchRunner fetchTask = new FetchRunner(continents, this.out, this.countryConfig, this.accessPointWs, this.zoningService);
			pool.submit(fetchTask);
			pool.submit(new SendRunner(this.out,this.zoningService,fetchTask));
			// fetch(ZoneMakerMode.SEND, continents);

			// runPhoton(continents);
			pool.awaitTermination(365, TimeUnit.DAYS);
		} catch (Exception e) {
			log(e);
		}
	}

	private void runPhoton(List<ContinentSettings> continents) throws IOException {
		for (ContinentSettings con : continents) {
			for (String country : con.getCountries()) {
				final String command = createPhotonCommand(con, country);
				runCommand(command);
			}
		}
	}

	private String createPhotonCommand(ContinentSettings con, String country) {
		final StringBuilder photon = new StringBuilder();
		photon.append("hadoop ");
		photon.append(" --config ").append(this.hadoopConfig);
		photon.append(" --jar ").append(this.photonConverterJar);
		photon.append(" --model ").append("ttomshp");
		photon.append(" --type ").append("CUSTOM");
		photon.append(" --zone ").append(country);
		photon.append(" --version ").append(con.getVersion());
		photon.append(" --format ").append("SHP");
		photon.append(" --jobConfig ").append(this.jobConfig);
		photon.append(" --branchAndVersion ").append(con.getBranchAndVersion());
		return photon.toString();
	}

	private void runCommand(String command) throws IOException {
		log("Running command: " + command);
		ProcessBuilder builder = new ProcessBuilder(command);
		Process process;
		try {
			process = builder.start();
			InputStream in = process.getInputStream();
			InputStream err = process.getErrorStream();
		} catch (Exception e) {
		    log(e);
		}
	}

	private List<ContinentSettings> readConfig(File config) throws IOException {
		List<ContinentSettings> result = Lists.newLinkedList();
		log("Reading file: " + config.getAbsolutePath());
		BufferedReader br = new BufferedReader(new FileReader(config));
		try {
			String line = br.readLine();
			while (line != null && !line.isEmpty()) {
				log("Line read: " + line);
				result.add(ContinentSettings.build(line));
				line = br.readLine();
			}
		} finally {
			br.close();
		}
		return result;
	}

	private void log(String log) {
		LOGGER.debug(log);
	}

    private void log(Exception e) {
        LOGGER.error(e.getMessage(), e);
    }

	@Override
	public String getName() {
		return "PhotonRunner";
	}

	@Override
	public void execute() {
			run();
	}

}
