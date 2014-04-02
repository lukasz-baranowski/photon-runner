package com.tomtom.photon.runner.threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tomtom.photon.runner.PhotonRunner;
import com.tomtom.photon.runner.conf.ContinentSettings;
import com.tomtom.photon.runner.conf.ZoneMakerConf;
import com.tomtom.photon.tools.zonemaker.Params;
import com.tomtom.photon.tools.zonemaker.ZoneMaker;

public class FetchRunner implements Callable<Void> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FetchRunner.class);

	private final List<ContinentSettings> continents;
	private final ZoneMakerConf zoneMakerConf;

	protected boolean finished = false;

	public FetchRunner(List<ContinentSettings> continents, ZoneMakerConf zoneMakerConf) {
		super();
		this.continents = continents;
		this.zoneMakerConf = zoneMakerConf;
	}

	@Override
	public Void call() throws Exception {
        try {
            fetch();
			LOGGER.info("Finished");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }finally {
        	finished = true;
        }
		return null;
	}

	private void fetch() throws IOException {
		for (ContinentSettings con : continents) {
            final File continentFetchOut = prepareFileSystem(con);

			LOGGER.info("Fetch: " + con.getName());
			File doneMarker = new File(continentFetchOut, con.getName() + ".done");
			if (doneMarker.exists()) {
				LOGGER.info("Already fetched skipping.");
				continue;
			}
			runZoneMaker(continentFetchOut, con);

			doneMarker.createNewFile();
			LOGGER.info("Done: " + con.getName());
		}
		
	}

	private File prepareFileSystem(ContinentSettings con) {
        final String continentDir = PhotonRunner.FETCHED_DIR + File.separator + con.getName();
        File fetchOut = new File(zoneMakerConf.getOut(), continentDir);
        fetchOut.mkdirs();
        savePropertiesFile(fetchOut, con);
        return fetchOut;
	}

    private void runZoneMaker(File continentfetchOut, ContinentSettings con) {
        Params p = createParams(continentfetchOut, con);
        ZoneMaker zm = new ZoneMaker(p);
        zm.run();
	}

    private Params createParams(File continentfetchOut, ContinentSettings con) {
        Params p = zoneMakerConf.createBasicParams(Params.WORK_MODE.FETCH);
        p.setOutputDir(continentfetchOut.getAbsolutePath());
        //p.setJournalVersion(con.getJournalVersion());
        p.setRegionName(con.getName());
        p.setRegionVersion(con.getVersion());
        if (ZoneMakerConf.ADM_MODE_CONTINENTS.contains(con.getName())) {
        	p.setAdministrativeLevel(Params.ADMINISTRATIVE_LEVEL.ORDER1);
        }
        return p;
    }

    private void savePropertiesFile(File continentfetchOut, ContinentSettings con) {
        try {
            Properties props = new Properties();
            props.setProperty("name", con.getName());
            props.setProperty("version", con.getVersion());
            props.setProperty("branch", "" + con.getBranch());
            props.setProperty("journalVersion", "" + con.getJournalVersion());
            File f = new File(continentfetchOut, "$_$" + con.getName() + ".properties");
            OutputStream out = new FileOutputStream(f);
            props.store(out, "Continent settings");
        }
        catch (Exception e ) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}