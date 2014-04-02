package com.tomtom.photon.runner.threads;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.tomtom.photon.runner.PhotonRunner;
import com.tomtom.photon.runner.conf.ContinentSettings;

public class HadoopRunner implements Callable<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HadoopRunner.class);

    private final SendRunner sendTask;

    private final File sentOut;
    private final File hadoopOut;

    public HadoopRunner(final String out, final SendRunner sendTask) {
        this.sentOut = new File(out, PhotonRunner.SENT_DIR);
        this.hadoopOut = new File(out, PhotonRunner.DONE_DIR);
        hadoopOut.mkdirs();
        this.sendTask = sendTask;
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

    private void runTask() throws Exception {
        while (true) {
            
            for (File continentSentOut : sentOut.listFiles()) {
                List<File> toProcessList = getFilesToProcess(continentSentOut);
                for (File toProcess : toProcessList) {
                    final String name = getName(toProcess);

                    File doneMarker = new File(toProcess, name + ".done");
                    if (!doneMarker.exists()) {
                        LOGGER.info("About to run on hadoop " + name);

                        runPhotonConverter();
                        doneMarker.createNewFile();
                    }
                }
            }
            TimeUnit.MINUTES.sleep(10);
        }
    }

    private String getName(File toProcess) {
        return toProcess.getName().toString().substring(0, 3);
    }

    private List<File> getFilesToProcess(File directory) {
        File[] toProcess = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".json.done")) {
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

    private void runPhotonConverter() throws InterruptedException {
        LOGGER.info("Running hadoop...");
    }

    
    
    
    
    
    private void runPhoton(List<ContinentSettings> continents) throws IOException {
        for (ContinentSettings con : continents) {
//              final String command = createPhotonCommand(con, country);
//              runCommand(command);
        }
    }

    private String createPhotonCommand(ContinentSettings con, String country) {
        final StringBuilder photon = new StringBuilder();
        photon.append("hadoop ");
//        photon.append(" --config ").append(this.hadoopConfig);
//        photon.append(" --jar ").append(this.photonConverterJar);
//        photon.append(" --model ").append("ttomshp");
//        photon.append(" --type ").append("CUSTOM");
//        photon.append(" --zone ").append(country);
//        photon.append(" --version ").append(con.getVersion());
//        photon.append(" --format ").append("SHP");
//        photon.append(" --jobConfig ").append(this.jobConfig);
        photon.append(" --branchAndVersion ").append(con.getBranchAndVersion());
        return photon.toString();
    }

    private void runCommand(String command) throws IOException {
        LOGGER.info("Running command: " + command);
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process;
        try {
            process = builder.start();
            InputStream in = process.getInputStream();
            InputStream err = process.getErrorStream();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
    
}
