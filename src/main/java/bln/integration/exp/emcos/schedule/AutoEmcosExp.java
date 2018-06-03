package bln.integration.exp.emcos.schedule;

import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.exp.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AutoEmcosExp implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AutoEmcosExp.class);

    @Value("${bln.integration.exp.emcos.schedule.AutoEmcosExp}")
    private boolean enable;

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
