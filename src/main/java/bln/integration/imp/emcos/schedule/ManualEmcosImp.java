package bln.integration.imp.emcos.schedule;

import bln.integration.entity.AtTimeValueRaw;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.enums.BatchStatusEnum;
import bln.integration.entity.enums.DirectionEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.entity.enums.WorkListTypeEnum;
import bln.integration.imp.ImportRunner;
import bln.integration.imp.Reader;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class ManualEmcosImp implements ImportRunner {
	private final Reader<AtTimeValueRaw> manualAtTimeValueReader;
	private final Reader<PeriodTimeValueRaw> manualPeriodTimeValueReader;
	private final WorkListHeaderRepository headerRepository;

	private static final Logger logger = LoggerFactory.getLogger(ManualEmcosImp.class);

	@Value("${bln.integration.imp.emcos.schedule.manualEmcosImp}")
	private boolean enable;

	@SuppressWarnings("Duplicates")
	@Scheduled(cron = "0 */1 * * * *")
	public void run() {
		if (!enable) return;

		List<Callable<Void>> callables = new ArrayList<>();
		headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(SourceSystemEnum.EMCOS, DirectionEnum.IMPORT, WorkListTypeEnum.USER)
			.stream()
			.filter(h -> h.getAtStatus()==BatchStatusEnum.W )
			.filter(h -> h.getActive())
			.filter(h -> h.getConfig()!=null)
			.forEach(header -> {
				callables.add( () -> { manualAtTimeValueReader.read(header); return null; } );
				callables.add( () -> { manualPeriodTimeValueReader.read(header); return null; } );
			});

		try {
			ExecutorService executor = Executors.newFixedThreadPool(2);
			executor.invokeAll(callables);
			executor.shutdown();
		}

		catch (Exception e) {
			logger.error("run failed: " + e.getMessage());
		}
    }
}
