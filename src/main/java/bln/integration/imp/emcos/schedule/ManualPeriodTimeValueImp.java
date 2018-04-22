package bln.integration.imp.emcos.schedule;

import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.imp.ImportRunner;
import bln.integration.imp.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class ManualPeriodTimeValueImp implements ImportRunner {
	private static final Logger logger = LoggerFactory.getLogger(ManualPeriodTimeValueImp.class);

	@Value("${bln.integration.imp.emcos.schedule.ManualPeriodTimeValueImp}")
	private boolean enable;

	@Scheduled(cron = "0 */1 * * * *")
	public void run() {
		if (!enable) return;

		try {
			manualPeriodTimeValueReader.read();
		}
		
		catch (Exception e) {
			logger.error("run failed: " + e.getMessage());
		}
    }

	@Autowired
	private Reader<PeriodTimeValueRaw> manualPeriodTimeValueReader;
}
