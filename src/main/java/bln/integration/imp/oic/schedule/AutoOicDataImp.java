package bln.integration.imp.oic.schedule;

import bln.integration.entity.TelemetryRaw;
import bln.integration.entity.enums.DirectionEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.entity.enums.WorkListTypeEnum;
import bln.integration.imp.ImportRunner;
import bln.integration.imp.Reader;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class AutoOicDataImp implements ImportRunner {
	private final Reader<TelemetryRaw> reader;
	private final WorkListHeaderRepository headerRepository;

	private static final Logger logger = LoggerFactory.getLogger(AutoOicDataImp.class);

	@Value("${bln.integration.imp.oic.schedule.autoOicImp}")
	private boolean enable;

	@SuppressWarnings("Duplicates")
	@Scheduled(cron = "0 8 */1 * * *")
	public void run() {
		if (!enable) return;

		List<Callable<Void>> callables = new ArrayList<>();
		buildHeaderIds().stream()
			.forEach(headerId -> callables.add( () -> { reader.read(headerId); return null; } ));

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
	List<Long> buildHeaderIds() {
		return headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.OIC,  DirectionEnum.IMPORT, WorkListTypeEnum.SYS
		)
		.stream()
		.filter(h-> h.getActive() && h.getConfig()!=null)
		.map(h -> h.getId())
		.collect(toList());
	}
}
