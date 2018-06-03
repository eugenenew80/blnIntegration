package bln.integration.imp.oic.schedule;

import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.WorkListHeader;
import bln.integration.entity.enums.*;
import bln.integration.imp.ImportRunner;
import bln.integration.imp.Reader;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class AutoOicDataImp implements ImportRunner {
	private final Reader<PeriodTimeValueRaw> autoOicPtReader;
	private final WorkListHeaderRepository headerRepository;
	private static final Logger logger = LoggerFactory.getLogger(AutoOicDataImp.class);

	@Value("${bln.integration.imp.oic.schedule.autoOicImp}")
	private boolean enable;

	@SuppressWarnings("Duplicates")
	@Scheduled(cron = "0 42 */1 * * *")
	public void run() {
		if (!enable) return;

		List<WorkListHeader> headers = headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.OIC,
			DirectionEnum.IMPORT,
			WorkListTypeEnum.SYS
		);

		List<Runnable> tasks = headers.stream()
			.filter(h -> h.getParamType() == ParamTypeEnum.PT && h.getActive() && h.getConfig() != null && h.getStatus()!=BatchStatusEnum.P)
			.map(h -> (Runnable) () -> autoOicPtReader.read(h.getId()))
			.collect(toList());

		submit(tasks);
	}

	private void submit(List<Runnable> tasks) {
		ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
		tasks.forEach(t -> executor.submit(t));
		executor.shutdown();
	}
}
