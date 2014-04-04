package com.tomtom.photon.runner.threads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tomtom.photon.runner.PhotonRunner;

public class HadoopRunner implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HadoopRunner.class);

    private final String wbmOUT = "output/wbm_rio/";
    private final SendRunner sendTask;

    private final File sentOut;

    private final String hadoopConfig;
    private final String jobConfig;
    private final String photonConverterJar;

    private final Set<String> currentlyProcessed = Sets.<String>newHashSet();

    private final Semaphore s = new Semaphore(1);

    private final File dest;

    public HadoopRunner(String out, String hadoopConfig, String jobConfig, String photonConverterJar, String destinationDir, SendRunner sendTask) {
        this.sentOut = new File(out, PhotonRunner.SENT_DIR);
        this.sendTask = sendTask;
        this.hadoopConfig = hadoopConfig;
        this.jobConfig = jobConfig;
        this.photonConverterJar = photonConverterJar;
        this.dest = new File(destinationDir);
        dest.mkdirs();
    }

    @Override
    public Void call() throws Exception {
        try {
            runTask();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
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
                LOGGER.info("Done on hadoop " + name);
                LOGGER.info("Moving tifascii " + name);
                File source = new File(wbmOUT, name);
                File lastDest = new File(dest, name);
                source.renameTo(lastDest);
                LOGGER.info("Moved to " + lastDest);
                doneMarker.createNewFile();

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
        final List<String> command = createPhotonCommand(props, name, readZoneversion(datasetToProcessFile));
        LOGGER.info("Running hadoop...");
        runCommand(command);
    }

    private String readZoneversion(File datasetToProcessFile) throws IOException {
        Pattern pattern = Pattern.compile("\"version\" : \"([0-9\\.]+)\",");
        FileReader input = new FileReader(datasetToProcessFile);
        try {
            LineIterator lineIterator = IOUtils.lineIterator(input);

            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }

        } finally {
            input.close();
        }
        return null;
    }

    private List<String> createPhotonCommand(Properties props, String name, String version) {
        final List<String> photon = Lists.newArrayList();
        photon.add("hadoop");
        photon.add("--config");
        photon.add(this.hadoopConfig);
        photon.add("jar");
        photon.add(this.photonConverterJar);
        photon.add("--config");
        photon.add(this.jobConfig);
        photon.add("--model");
        photon.add("wbm_rio");
        photon.add("--type");
        photon.add("COUNTRY");
        photon.add("--zone");
        photon.add(name);
        photon.add("--version");
        photon.add(version);
        photon.add("--format");
        photon.add("TIFF_ASCII");
        photon.add("--branchAndVersion");
        photon.add(props.getProperty("branchAndVersion"));
        return photon;
    }

    private void runCommand(List<String> command) throws IOException {
        LOGGER.info(command.toString());
        ProcessBuilder builder = new ProcessBuilder(command.toArray(new String[] {}));
        try {
            final Process process = builder.start();
            // Handle stdout...
            new Thread() {

                @Override
				public void run() {
                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    try {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null)
                            System.out.println(inputLine);
                        in.close();
                    } catch (Exception anExc) {
                        anExc.printStackTrace();
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }
            }.start();

            // Handle stderr...
            new Thread() {

                @Override
				public void run() {
                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    try {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null)
                            System.out.println(inputLine);
                    } catch (Exception anExc) {
                        anExc.printStackTrace();
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }
            }.start();

            int res = process.waitFor();
            LOGGER.info("Output: " + res);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
