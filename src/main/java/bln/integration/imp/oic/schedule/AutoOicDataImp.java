package bln.integration.imp.oic.schedule;

import bln.integration.entity.TelemetryRaw;
import bln.integration.imp.ImportRunner;
import bln.integration.imp.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AutoOicDataImp implements ImportRunner {
	private static final Logger logger = LoggerFactory.getLogger(AutoOicDataImp.class);

	@Value("${bln.integration.imp.oic.schedule.AutoOicDataImp}")
	private boolean enable;

	@Scheduled(cron = "0 15 */1 * * *")
	public void run() {
		if (!enable) return;

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
