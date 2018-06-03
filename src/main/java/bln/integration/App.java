package bln.integration;

import bln.integration.exp.emcos.schedule.AutoEmcosExp;
import bln.integration.imp.emcos.schedule.AutoEmcosImp;
import bln.integration.imp.emcos.schedule.ManualEmcosImp;
import bln.integration.imp.oic.schedule.AutoOicImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

        taskScheduler.schedule(autoEmcosImp, new CronTrigger(autoEmcosImpCronExpr));
        taskScheduler.schedule(manualEmcosImp, new CronTrigger(manualEmcosImpCronExpr));
        taskScheduler.schedule(autoOicImp, new CronTrigger(autoOicImpCronExpr));
        taskScheduler.schedule(autoEmcosExp, new CronTrigger(autoEmcosExpCronExpr));
    }


    @Value("${bln.integration.imp.emcos.schedule.autoEmcosImp.cron}")
    private String autoEmcosImpCronExpr;

    @Value("${bln.integration.imp.emcos.schedule.manualEmcosImp.cron}")
    private String manualEmcosImpCronExpr;

    @Value("${bln.integration.imp.oic.schedule.autoOicImp.cron}")
    private String autoOicImpCronExpr;

    @Value("${bln.integration.exp.emcos.schedule.autoEmcosExp.cron}")
    private String autoEmcosExpCronExpr;

    @Autowired
    private AutoEmcosImp autoEmcosImp;

    @Autowired
    private ManualEmcosImp manualEmcosImp;

    @Autowired
    private AutoOicImp autoOicImp;

    @Autowired
    private AutoEmcosExp autoEmcosExp;
}
