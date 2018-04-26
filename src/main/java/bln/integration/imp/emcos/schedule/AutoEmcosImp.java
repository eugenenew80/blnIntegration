package bln.integration.imp.emcos.schedule;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.imp.*;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class AutoEmcosImp implements ImportRunner {
	private final Reader<AtTimeValueRaw> autoAtTimeValueReader;
	private final Reader<PeriodTimeValueRaw> autoPeriodTimeValueReader;
	private final WorkListHeaderRepository headerRepository;

	private static final Logger logger = LoggerFactory.getLogger(AutoEmcosImp.class);

	@Value("${bln.integration.imp.emcos.schedule.autoEmcosImp}")
	private boolean enable;

	@SuppressWarnings("Duplicates")
	@Scheduled(cron = "0 45 */1 * * *")
	public void run() {
		if (!enable) return;

		List<Callable<Void>> callables = new ArrayList<>();
		headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
		    SourceSystemEnum.EMCOS, DirectionEnum.IMPORT, WorkListTypeEnum.SYS
        )
			.stream()
			.filter(h -> h.getActive())
			.filter(h -> h.getConfig()!=null)
			.forEach(header -> {
				callables.add( () -> { autoAtTimeValueReader.read(header); return null; } );
				callables.add( () -> { autoPeriodTimeValueReader.read(header); return null; } );
			});

		try {
			ExecutorService executor = Executors.newFixedThreadPool(20);
			executor.invokeAll(callables);
			executor.shutdown();
		}

		catch (Exception e) {
			logger.error("run failed: " + e.getMessage());
		}
    }
}
