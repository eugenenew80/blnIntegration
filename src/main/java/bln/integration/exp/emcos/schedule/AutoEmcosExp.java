package bln.integration.exp.emcos.schedule;

import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.exp.Sender;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class AutoEmcosExp implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AutoEmcosExp.class);
    private final Sender<PeriodTimeValueRaw> ftpPeriodTimeValueSender;
    public  final AtomicBoolean isEnable = new AtomicBoolean(true);
    private boolean isRunning = false;

    public void run() {
        if (!isEnable.get() || isRunning) return;

        try {
            isRunning = true;
            ftpPeriodTimeValueSender.send();
        }
        catch (Exception e) {
            logger.error("run failed: " + e.getMessage());
        }
        finally {
            isRunning = false;
        }
    }
}
