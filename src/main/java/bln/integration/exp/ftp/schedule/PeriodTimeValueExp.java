package bln.integration.exp.ftp.schedule;

import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.exp.ExportRunner;
import bln.integration.exp.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PeriodTimeValueExp implements ExportRunner {
    private static final Logger logger = LoggerFactory.getLogger(PeriodTimeValueExp.class);

    @Value("${bln.integration.exp.ftp.sender.PeriodTimeValueExp}")

    private boolean enable;
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        if (!enable) return;

        try {
            ftpPeriodTimeValueSender.send();
        }

        catch (Exception e) {
            logger.error("run failed: " + e.getMessage());
        }
    }

    @Autowired
    private Sender<PeriodTimeValueRaw> ftpPeriodTimeValueSender;
}
