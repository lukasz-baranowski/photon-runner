package com.tomtom.photon.runner.threads;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tomtom.photon.runner.PhotonRunner;

public class HadoopRunner implements Callable<Void> {
	private static final Logger LOGGER = LoggerFactory.getLogger(HadoopRunner.class);

	private final SendRunner sendTask;

	private final File sentOut;

	private final String hadoopConfig;
	private final String jobConfig;
	private final String photonConverterJar;

	private final Set<String> currentlyProcessed = Sets.<String> newHashSet();

	private final Semaphore s = new Semaphore(1);

	public HadoopRunner(String out, String hadoopConfig, String jobConfig, String photonConverterJar, SendRunner sendTask) {
        this.sentOut = new File(out, PhotonRunner.SENT_DIR);
        this.sendTask = sendTask;
        this.hadoopConfig = hadoopConfig;
        this.jobConfig = jobConfig;
        this.photonConverterJar = photonConverterJar;
    }

    @Override
	public Void call() throws Exception {
		try {
			runTask();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	private void runTask() throws IOException, InterruptedException {
		while (true) {

			Optional<File> datasetToProcess = getNextDatasetToProcess();

			if (sendTask.finished && !datasetToProcess.isPresent()) {
				LOGGER.info("Finished");
				break;
			}
			if (datasetToProcess.isPresent()) {
			    File datasetToProcessFile = datasetToProcess.get();
				String name = getName(datasetToProcessFile);
				LOGGER.info("About to run on hadoop " + name);

				runPhotonConverter(datasetToProcessFile, name);

				s.acquire();
				try {
					currentlyProcessed.remove(name);
				} finally {
					s.release();
				}
				File doneMarker = new File(datasetToProcessFile.getAbsolutePath() + ".done");
				doneMarker.createNewFile();
				LOGGER.info("Done on hadoop " + name);

			} else {
				TimeUnit.SECONDS.sleep(10);
			}
		}
	}

	private Optional<File> getNextDatasetToProcess() throws IOException, InterruptedException {
		s.acquire();
		try {
			for (File continentSentOut : sentOut.listFiles()) {
				List<File> toProcessList = getFilesToProcess(continentSentOut);
				for (File toProcess : toProcessList) {
					File doneMarker = new File(toProcess.getAbsolutePath() + ".done");
					if (!doneMarker.exists() && !currentlyProcessed.contains(toProcess.getName())) {
						currentlyProcessed.add(toProcess.getName());
						return Optional.of(toProcess);
					}
				}
			}
		} finally {
			s.release();
		}
		return Optional.absent();
	}

	private String getName(File toProcess) {
		return toProcess.getName().toString().substring(0, 3);
	}

	private List<File> getFilesToProcess(File directory) {
		File[] toProcess = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".json") && !name.startsWith("$")) { // countries
																		// only
																		// - $
																		// are
																		// no
																		// countries
					return true;
				}
				return false;
			}
		});
		if (toProcess.length != 0) {
			return Arrays.asList(toProcess);
		} else {
			return Lists.newArrayList();
		}
	}

	private void runPhotonConverter(File datasetToProcessFile, String name) throws InterruptedException, IOException {
	    final Properties props = SendRunner.readPropertiesFile(datasetToProcessFile.getParentFile());
	    final String command = createPhotonCommand(props, name);
	    LOGGER.info("Running hadoop...");
	    LOGGER.info(command);
	    runCommand(command);
	}

    private String createPhotonCommand(Properties props, String name) {
        final StringBuilder photon = new StringBuilder();
        photon.append("hadoop ");
        photon.append(" --config ").append(this.hadoopConfig);
        photon.append(" --jar ").append(this.photonConverterJar);
        photon.append(" --model ").append("ttomshp");
        photon.append(" --type ").append("CUSTOM");
        photon.append(" --zone ").append(name);
        photon.append(" --version ").append(props.getProperty("version"));
        photon.append(" --format ").append("SHP");
        photon.append(" --jobConfig ").append(this.jobConfig);
        photon.append(" --branchAndVersion ").append(props.getProperty("branchAndVersion"));
        return photon.toString();
    }

	@SuppressWarnings("unused")
    private void runCommand(String command) throws IOException {
		LOGGER.info(command);
		ProcessBuilder builder = new ProcessBuilder(command);
		Process process;
		try {
			process = builder.start();
			InputStream in = process.getInputStream();
			InputStream err = process.getErrorStream();
			int res = process.waitFor();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

}
