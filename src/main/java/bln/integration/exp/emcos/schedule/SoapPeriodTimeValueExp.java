package bln.integration.exp.emcos.schedule;

import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.exp.ExportRunner;
import bln.integration.exp.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SoapPeriodTimeValueExp implements ExportRunner {
    private static final Logger logger = LoggerFactory.getLogger(SoapPeriodTimeValueExp.class);

    //@Scheduled(cron = "*/1 * * * * *")
    public void run() {
        try {
            soapPeriodTimeValueSender.send();
        }

        catch (Exception e) {
            logger.error("run failed: " + e.getMessage());
        }
    }

    @Autowired
    private Sender<PeriodTimeValueRaw> soapPeriodTimeValueSender;
}
