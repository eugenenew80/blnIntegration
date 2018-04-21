package bln.integration.imp.oic.schedule;

import bln.integration.entity.TelemetryRaw;
import bln.integration.imp.ImportRunner;
import bln.integration.imp.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AutoOicDataImp implements ImportRunner {
	private static final Logger logger = LoggerFactory.getLogger(AutoOicDataImp.class);

	//@Scheduled(cron = "*/15 * * * * *")
	public void run() {
		try {
			reader.read();
		}

		catch (Exception e) {
			logger.error("run failed: " + e.getMessage());
		}
	}

	@Autowired
	private Reader<TelemetryRaw> reader;
}
