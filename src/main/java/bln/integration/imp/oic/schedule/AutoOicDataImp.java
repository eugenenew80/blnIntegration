package bln.integration.imp.oic.schedule;

import bln.integration.entity.TelemetryRaw;
import bln.integration.entity.WorkListHeader;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AutoOicDataImp implements ImportRunner {
	private final Reader<TelemetryRaw> reader;
	private final WorkListHeaderRepository headerRepository;

	private static final Logger logger = LoggerFactory.getLogger(AutoOicDataImp.class);

	@Value("${bln.integration.imp.oic.schedule.autoOicImp}")
	private boolean enable;

	@SuppressWarnings("Duplicates")
	@Scheduled(cron = "0 15 */1 * * *")
	public void run() {
		if (!enable) return;

		List<Callable<Void>> callables = headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.OIC, DirectionEnum.IMPORT, WorkListTypeEnum.SYS
		)
			.stream()
			.filter(WorkListHeader::getActive)
			.filter(h -> h.getConfig() != null)
			.<Callable<Void>>map(header -> () -> {
				reader.read(header);
				return null;
			}).collect(Collectors.toList());

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
