package com.tomtom.photon.runner;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class SendRunner implements Callable<Void> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FetchRunner.class);

	private final String zoningService;
	private final FetchRunner fetchTask;

	private final File fetchOut;

	private final File sentOut;

	public SendRunner(String out, String zoningService, FetchRunner fetchTask) {
		fetchOut = new File(out, PhotonRunner.FETCHED_DIR);
		sentOut = new File(out, PhotonRunner.SENT_DIR);
		fetchOut.mkdirs();
		sentOut.mkdirs();
		this.zoningService = zoningService;
		this.fetchTask = fetchTask;
	}

	@Override
	public Void call() throws Exception {
		
		
		while(true) {
		
			if (fetchTask.finished && !getNextFileToSend().isPresent()) {
				break;
			}
			TimeUnit.SECONDS.sleep(10);
		}
		LOGGER.info("Finished");
		return null;
		
	}

	private Optional<File> getNextFileToSend() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
