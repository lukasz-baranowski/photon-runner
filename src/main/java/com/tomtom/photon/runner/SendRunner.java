package com.tomtom.photon.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.tomtom.photon.tools.zonemaker.Params;
import com.tomtom.photon.tools.zonemaker.ZoneMaker;

public class SendRunner implements Callable<Void> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SendRunner.class);

	private final ZoneMakerConf zoneMakerConf;
	private final FetchRunner fetchTask;

	private final File fetchOut;
	private final File sentOut;

	public SendRunner(ZoneMakerConf zoneMakerCnf, FetchRunner fetchTask) {
	    zoneMakerConf = zoneMakerCnf;
		fetchOut = new File(zoneMakerCnf.getOut(), PhotonRunner.FETCHED_DIR);
		sentOut = new File(zoneMakerCnf.getOut(), PhotonRunner.SENT_DIR);
		sentOut.mkdirs();
		this.fetchTask = fetchTask;
	}

	@Override
	public Void call() throws Exception {
	    boolean finishedProcessingAll = false;
		while (true) {
		    if (finishedProcessingAll) {
		        break;
		    }

		    for (File continentFetchDir : fetchOut.listFiles()) {
	            Optional<File> fileToSend = getNextFileToSend(continentFetchDir);
	            if (fetchTask.finished && !fileToSend.isPresent()) {
	                finishedProcessingAll = true;
	            }

	            if (!fileToSend.isPresent()) {
	                TimeUnit.SECONDS.sleep(2);
	                break;
	            }

	            moveFileFromFetchedToSent(fileToSend);

	            File doneMarker = new File(sentOut, fileToSend.get().getName() + ".done");
	            if (!doneMarker.exists()) {
                    Params p = createParamsFile(continentFetchDir);
                    LOGGER.info("Sending " + fileToSend.get().getName());
                    runZoneMaker(p);
                    doneMarker.createNewFile();
	            }

		    }
		}
		LOGGER.info("Finished");
		return null;
	}

	private void moveFileFromFetchedToSent(Optional<File> fileToSend) {
	    if (fileToSend.isPresent()) {
	        File f = fileToSend.get();
	        LOGGER.info("Moving to sent " + f.getName());
	        File moved = new File(sentOut, f.getName());
	        if (moved.exists()) {
	            f.delete();
	        } else {
	            f.renameTo(new File(sentOut, f.getName()));
	        }
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

	private Params createParamsFile(File continentFetchDir) {
        Params p = zoneMakerConf.createBasicParams(Params.WORK_MODE.SEND);
        Properties props = readPropertiesFile(continentFetchDir);
        p.setOutputDir(sentOut.getAbsolutePath());
        // p.setJournalVersion(props.getProperty("journalVersion"));
        p.setRegionName(props.getProperty("name"));
        p.setRegionVersion(props.getProperty("version"));
        if (ZoneMakerConf.ADM_MODE_CONTINENTS.contains(props.getProperty("name", ""))) {
            p.setAdministrativeLevel(Params.ADMINISTRATIVE_LEVEL.ORDER1);
        }
        return p;
	}

	private Properties readPropertiesFile(File continentFetchDir) {
	    final String continentName = continentFetchDir.getName();
        final Properties props = new Properties();
        try {
            File f = new File(continentFetchDir, "$_$" + continentName + ".properties");
            InputStream is = new FileInputStream(f);
            props.load(is);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return props;
	}

}
