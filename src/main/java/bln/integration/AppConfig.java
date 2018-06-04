package bln.integration;

import bln.integration.exp.emcos.schedule.AutoEmcosExp;
import bln.integration.imp.emcos.schedule.AutoEmcosImp;
import bln.integration.imp.emcos.schedule.ManualEmcosImp;
import bln.integration.imp.oic.schedule.AutoOicImp;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@Configuration
public class AppConfig implements ApplicationListener<ApplicationReadyEvent> {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ParameterNamesModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        taskScheduler().schedule(autoEmcosImp,   new CronTrigger(autoEmcosImpCronExpr));
        taskScheduler().schedule(manualEmcosImp, new CronTrigger(manualEmcosImpCronExpr));
        taskScheduler().schedule(autoOicImp,     new CronTrigger(autoOicImpCronExpr));
        taskScheduler().schedule(autoEmcosExp,   new CronTrigger(autoEmcosExpCronExpr));
    }

    private TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.initialize();
        return taskScheduler;
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
