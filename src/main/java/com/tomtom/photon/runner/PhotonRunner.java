package com.tomtom.photon.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.teleatlas.global.common.cli.AbstractArgs4jTool;
import com.teleatlas.global.common.cli.ToolExecutionException;
import com.tomtom.photon.tools.zonemaker.Params;
import com.tomtom.photon.tools.zonemaker.ZoneMaker;

public class PhotonRunner extends AbstractArgs4jTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhotonRunner.class);

    private enum ZoneMakerMode {
        FETCH,
        SEND;
    }

    private static final List<String> ADM_MODE_CONTINENTS = Arrays.asList("NAM"); 

    @Option(name = "--continents", usage = "Sets continents config file", aliases = "-c", required = true)
    private File continentsFile;

    @Option(name = "--zoneMakerJar", usage = "Sets zone-maker jar", aliases = "-zmj", required = true)
    private String zoneMakerJar;

    @Option(name = "--countryConfig", usage = "Sets country_config.xml file", aliases = "-cc", required = true)
    private String countryConfig;

    @Option(name = "--accessPoint", usage = "Sets access-point-ws url", aliases = "-ap", required = true)
    private String accessPointWs;

    @Option(name = "--zoningservice", usage = "Sets zoningservice url", aliases = "-zs", required = true)
    private String zoningService;

    @Option(name = "--zoneMakerOut", usage = "Sets zone-maker output dir", aliases = "-zmo", required = true)
    private String zoneMakerOut;

    @Option(name = "--hadoopConfig", usage = "Sets hadoop config dir", aliases = "-hcd", required = true)
    private String hadoopConfig;

    @Option(name = "--photonConverterJar", usage = "Sets photonConverterJar jar", aliases = "-pcj", required = true)
    private String photonConverterJar;

    @Option(name = "--jobConfig", usage = "Sets job-config.xml file", aliases = "-jc", required = true)
    private String jobConfig;

    // Main method
    public void run() throws IOException {
        if (!continentsFile.exists()) {
            throw new IllegalArgumentException("No such file: " + this.continentsFile.getAbsolutePath());
        }
        List<ContinentSettings> continents = readConfig(this.continentsFile);

        runZoneMakerInMode(ZoneMakerMode.FETCH, continents);
        runZoneMakerInMode(ZoneMakerMode.SEND, continents);

        runPhoton(continents);
    }

    private void runZoneMakerInMode(ZoneMakerMode mode, List<ContinentSettings> continents) throws IOException {
        for (ContinentSettings con : continents) {
            Params p = createZoneMakerParams(mode, con);
            ZoneMaker zm = new ZoneMaker(p);
            zm.run();
        }
    }

    private Params createZoneMakerParams(ZoneMakerMode mode, ContinentSettings con) {
        Params p = new Params();
        p.setCountryConfig(this.countryConfig);
        p.setEndpointUrl(this.accessPointWs);
        p.setOutputDir(this.zoneMakerOut);
        p.setJournalVersion(con.getTransactionVersion());
        p.setRegionName(con.getName());
        p.setWorkMode(Params.WORK_MODE.valueOf(mode.toString()));
        p.setZoneType(Params.ZONE_TYPE.valueOf("COUNTRY"));
        p.setZoningUrl(this.zoningService);
        p.setRegionVersion(con.getVersion());
        if (ADM_MODE_CONTINENTS.contains(con.getName())) {
            p.setAdministrativeLevel(Params.ADMINISTRATIVE_LEVEL.ORDER1);
        }
        return p;
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

    private String createZoneMakerCommand(ContinentSettings con, ZoneMakerMode mode) {
        final StringBuilder zone_maker = new StringBuilder();
        zone_maker.append("java -jar ").append(this.zoneMakerJar);
        zone_maker.append(" -c ").append(this.countryConfig);
        zone_maker.append(" -url ").append(this.accessPointWs);
        zone_maker.append(" -out ").append(this.zoneMakerOut);
        zone_maker.append(" -out ").append(this.zoneMakerOut);
        zone_maker.append(" -mod ").append(mode.toString());
        zone_maker.append(" -rn ").append(con.getName());
        zone_maker.append(" -rver ").append(con.getVersion());
        zone_maker.append(" -type ").append("COUNTRY");
        zone_maker.append(" -zone ").append(this.zoningService);
        if (ADM_MODE_CONTINENTS.contains(con.getName())) {
            zone_maker.append(" -adm ").append("ORDER1");
        }
        return zone_maker.toString();
    }

    @SuppressWarnings("unused")
    private void runCommand(String command) throws IOException {
        log("Running command: " + command);
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        InputStream in = process.getInputStream();
        InputStream err = process.getErrorStream();
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
        System.out.println(log);
        LOGGER.debug(log);
    }

    @Override
    public String getName() {
        return "PhotonRunner";
    }

    @Override
    public void execute() throws ToolExecutionException {
        try {
            run();
        } catch (IOException e) {
            throw new ToolExecutionException(e.getMessage(), e);
        }
    }

}
