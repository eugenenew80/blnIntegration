package bln.integration.imp.emcos.schedule;

import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.imp.ImportRunner;
import bln.integration.imp.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoPeriodTimeValueImp implements ImportRunner {
	private static final Logger logger = LoggerFactory.getLogger(AutoPeriodTimeValueImp.class);

	@Scheduled(cron = "30 */1 * * * *")
	public void run() {
		try {
			autoPeriodTimeValueReader.read();
		}
		
		catch (Exception e) {
			logger.error("run failed: " + e.getMessage());
		}
    }

	@Autowired
	private Reader<PeriodTimeValueRaw> autoPeriodTimeValueReader;
}