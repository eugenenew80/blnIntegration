package bln.integration.imp.emcos.schedule;

import bln.integration.entity.AtTimeValueRaw;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.imp.ImportRunner;
import bln.integration.imp.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ManualEmcosImp implements ImportRunner {
	private static final Logger logger = LoggerFactory.getLogger(ManualEmcosImp.class);

	@Value("${bln.integration.imp.emcos.schedule.manualEmcosImp}")
	private boolean enable;

	@Scheduled(cron = "0 */1 * * * *")
	public void run() {
		if (!enable) return;

		try {
			ExecutorService executor = Executors.newFixedThreadPool(2);
			executor.invokeAll(Arrays.asList(
				() -> { manualAtTimeValueReader.read(); return null; },
				() -> { manualPeriodTimeValueReader.read(); return null; }
			));
			executor.shutdown();
		}

		catch (Exception e) {
			logger.error("run failed: " + e.getMessage());
		}
    }

	@Autowired
	private Reader<AtTimeValueRaw> manualAtTimeValueReader;

	@Autowired
	private Reader<PeriodTimeValueRaw> manualPeriodTimeValueReader;
}
