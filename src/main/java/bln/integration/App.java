package bln.integration;

import bln.integration.exp.emcos.schedule.AutoEmcosExp;
import bln.integration.imp.emcos.schedule.AutoEmcosImp;
import bln.integration.imp.emcos.schedule.ManualEmcosImp;
import bln.integration.imp.oic.schedule.AutoOicImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@EntityScan(
    basePackageClasses = { App.class, Jsr310JpaConverters.class }
)
@EnableScheduling
@SpringBootApplication
public class App implements ApplicationListener<ApplicationReadyEvent> {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.initialize();

        CronTrigger autoEmcosImpCronTrigger = new CronTrigger("0 */5 */1 * * *");
        taskScheduler.schedule(autoEmcosImp, autoEmcosImpCronTrigger);

        CronTrigger manualEmcosImpCronTrigger = new CronTrigger("0 */1 * * * *");
        taskScheduler.schedule(manualEmcosImp, manualEmcosImpCronTrigger);

        CronTrigger autoOicImpCronTrigger = new CronTrigger("0 42 */1 * * *");
        taskScheduler.schedule(autoOicImp, autoOicImpCronTrigger);

        CronTrigger autoEmcosExpCronTrigger = new CronTrigger("0 */1 * * * *");
        taskScheduler.schedule(autoEmcosExp, autoEmcosExpCronTrigger);
    }

    @Autowired
    private AutoEmcosImp autoEmcosImp;

    @Autowired
    private ManualEmcosImp manualEmcosImp;

    @Autowired
    private AutoOicImp autoOicImp;

    @Autowired
    private AutoEmcosExp autoEmcosExp;
}
