package com.tomtom.photon.runner.threads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.teleatlas.global.common.util.IOUtil;
import com.tomtom.photon.runner.PhotonRunner;
import com.tomtom.photon.runner.conf.ZoneMakerConf;
import com.tomtom.photon.tools.zonemaker.Params;
import com.tomtom.photon.tools.zonemaker.ZoneMaker;

public class SendRunner implements Callable<Void> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SendRunner.class);

	private final ZoneMakerConf zoneMakerConf;
	private final FetchRunner fetchTask;

	private final File fetchOut;
	private final File sentOut;

	protected boolean finished = false;

	private final File staging;

	public SendRunner(ZoneMakerConf zoneMakerCnf, FetchRunner fetchTask) {
		zoneMakerConf = zoneMakerCnf;
		fetchOut = new File(zoneMakerCnf.getOut(), PhotonRunner.FETCHED_DIR);
		sentOut = new File(zoneMakerCnf.getOut(), PhotonRunner.SENT_DIR);
		staging = new File(zoneMakerCnf.getOut(),"tmp");
		sentOut.mkdirs();
		staging.mkdirs();
		this.fetchTask = fetchTask;
	}

	@Override
	public Void call() throws Exception {
		try {
			runSend();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			finished = true;
		}
		return null;
	}

	private void runSend() throws Exception {
		fetchOut.mkdirs();
		while (true) {
			Optional<File> nextFileToSend = getNextFileToSend();

			if (fetchTask.finished && !nextFileToSend.isPresent()) {
				break;
			}
			
			if (nextFileToSend.isPresent()) {
				File file = nextFileToSend.get();
				File toBeSent = moveJsonFileFromFetchedToBeSent(file);
				File doneMarker = new File(file.getAbsolutePath() + ".done");
				if (!doneMarker.exists()) {
					String name = file.getName();
					Params p = createParamsFile(file.getParentFile(),name);
					LOGGER.info("Sending " + name);
					// runZoneMaker(p);
					LOGGER.info("Sent to zoning " + name);
					moveJsonFileFromStagingToSent(file,toBeSent);
					doneMarker.createNewFile();
				}
			}else {
				TimeUnit.SECONDS.sleep(2);
			}

		}
		LOGGER.info("Finished");
	}

	private void moveJsonFileFromStagingToSent(File file, File toBeSent) {
		File destDir = new File(sentOut,file.getParentFile().getName());
		toBeSent.renameTo(new File(destDir,toBeSent.getName()));
	}

	private File moveJsonFileFromFetchedToBeSent(File nextFileToSend) {
		File dest = new File(staging,nextFileToSend.getName());
		IOUtil.rmDir(staging);
		staging.mkdirs();
		nextFileToSend.renameTo(dest);
		return dest;
	}

	private Optional<File> getNextFileToSend() throws IOException {
		for (File continentFetchOut : fetchOut.listFiles()) {
			Optional<File> fileToSend = getNextFileToSend(continentFetchOut);
			if (fileToSend.isPresent()) {
				return fileToSend;
			}
		}
		return Optional.absent();
	}

	private File prepareFileSystem(File continentFetchOut) throws IOException {
		File continentSentOut = new File(sentOut, continentFetchOut.getName());
		if (!continentSentOut.exists()) {
			continentSentOut.mkdirs();
			copyPropertiesFile(continentFetchOut, continentSentOut);
		}
		return continentSentOut;
	}

	private void copyPropertiesFile(File continentFetchOut, File continentSentOut) throws IOException {
		File properties = new File(continentFetchOut, "$_$" + continentFetchOut.getName() + ".properties");
		File copied = new File(continentSentOut, properties.getName());
		if (!copied.exists()) {
			LOGGER.info("Copying to sent " + properties.getName());
			Files.copy(properties, copied);
		}
	}


	private void runZoneMaker(Params p) {
		ZoneMaker zm = new ZoneMaker(p);
		zm.run();
	}

	private Optional<File> getNextFileToSend(File directory) {
		File[] files = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".json")) {
					return true;
				}
				return false;
			}
		});

		if (files.length == 0) {
			return Optional.absent();
		} else {
			return Optional.of(files[0]);
		}
	}

	private Params createParamsFile(File continentSentDir, String name) {
		Params p = zoneMakerConf.createBasicParams(Params.WORK_MODE.SEND);
		Properties props = readPropertiesFile(continentSentDir);
		p.setOutputDir(staging.getAbsolutePath());
		// p.setJournalVersion(props.getProperty("journalVersion"));
		p.setRegionName(props.getProperty("name"));
		p.setRegionVersion(props.getProperty("version"));
		if (ZoneMakerConf.ADM_MODE_CONTINENTS.contains(props.getProperty("name", ""))) {
			p.setAdministrativeLevel(Params.ADMINISTRATIVE_LEVEL.ORDER1);
		}
		return p;
	}

	private Properties readPropertiesFile(File continentSentDir) {
		final String continentName = continentSentDir.getName();
		final Properties props = new Properties();
		try {
			File f = new File(continentSentDir, "$_$" + continentName + ".properties");
			InputStream is = new FileInputStream(f);
			props.load(is);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return props;
	}

}
