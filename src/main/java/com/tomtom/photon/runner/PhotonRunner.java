package com.tomtom.photon.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.teleatlas.global.common.cli.AbstractArgs4jTool;
import com.tomtom.photon.runner.conf.ContinentSettings;
import com.tomtom.photon.runner.conf.ZoneMakerConf;
import com.tomtom.photon.runner.threads.FetchRunner;
import com.tomtom.photon.runner.threads.HadoopRunner;
import com.tomtom.photon.runner.threads.SendRunner;

public class PhotonRunner extends AbstractArgs4jTool {
	private static final Logger LOGGER = LoggerFactory.getLogger(PhotonRunner.class);

	public static final String FETCHED_DIR = "fetched";
	public static final String SENT_DIR = "sent";
	public static final String DONE_DIR = "done";

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
			final ZoneMakerConf zoneMakerConf = ZoneMakerConf.valueOf(this.out, this.countryConfig, this.accessPointWs, this.zoningService);

			ExecutorService pool = Executors.newFixedThreadPool(3);

			FetchRunner fetchTask = new FetchRunner(continents, zoneMakerConf);
			SendRunner sendTask = new SendRunner(zoneMakerConf, fetchTask);
			HadoopRunner hadoopTask = new HadoopRunner(this.out, sendTask);
			pool.submit(fetchTask);
            pool.submit(sendTask);
            pool.submit(hadoopTask);

			pool.awaitTermination(365, TimeUnit.DAYS);
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
		LOGGER.info(log);
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
