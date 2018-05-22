package bln.integration.imp.emcos.schedule;

import bln.integration.entity.*;
import bln.integration.entity.enums.DirectionEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.entity.enums.WorkListTypeEnum;
import bln.integration.imp.*;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import static java.util.stream.Collectors.toList;

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
	@Scheduled(cron = "0 20 */1 * * *")
	public void run() {
		if (!enable) return;

		List<Callable<Void>> callables = new ArrayList<>();
		buildHeaderIds().stream()
			.forEach(headerId -> callables.add( () -> {
                autoAtTimeValueReader.read(headerId);
                autoPeriodTimeValueReader.read(headerId);
                return null;
            }));

		try {
			ExecutorService executor = Executors.newFixedThreadPool(callables.size());
			executor.invokeAll(callables);
			executor.shutdown();
		}

		catch (Exception e) {
			logger.error("run failed: " + e.getMessage());
		}
    }

	@Transactional(propagation= Propagation.NOT_SUPPORTED, readOnly = true)
    private List<Long> buildHeaderIds() {
		return headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.EMCOS,  DirectionEnum.IMPORT, WorkListTypeEnum.SYS
		)
		.stream()
		.filter(h-> h.getActive() && h.getConfig()!=null)
		.map(h -> h.getId())
		.collect(toList());
	}
}
