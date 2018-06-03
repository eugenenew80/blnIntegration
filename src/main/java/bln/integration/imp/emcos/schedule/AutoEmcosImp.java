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
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class AutoEmcosImp implements ImportRunner {
	private final Reader<AtTimeValueRaw> autoEmcosAtReader;
	private final Reader<PeriodTimeValueRaw> autoEmcosPtReader;
	private final WorkListHeaderRepository headerRepository;
	private static final Logger logger = LoggerFactory.getLogger(AutoEmcosImp.class);

	@Value("${bln.integration.imp.emcos.schedule.autoEmcosImp}")
	private boolean enable;

	@SuppressWarnings("Duplicates")
	@Scheduled(cron = "0 45 */1 * * *")
	public void run() {
		if (!enable) return;

		List<WorkListHeader> headers = headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.EMCOS,
			DirectionEnum.IMPORT,
			WorkListTypeEnum.SYS
		);

		List<Runnable> atTasks = headers.stream()
			.filter(h -> h.getParamType() == ParamTypeEnum.AT && h.getActive() && h.getConfig() != null && h.getStatus()!=BatchStatusEnum.P)
			.map(h -> (Runnable) () -> autoEmcosAtReader.read(h.getId()))
			.collect(toList());

		List<Runnable> ptTasks = headers.stream()
			.filter(h -> h.getParamType() == ParamTypeEnum.AT && h.getActive() && h.getConfig() != null && h.getStatus()!=BatchStatusEnum.P)
			.map(h -> (Runnable) () -> autoEmcosPtReader.read(h.getId()))
			.collect(toList());

		submit(atTasks);
		submit(ptTasks);
    }

    private void submit(List<Runnable> tasks) {
		ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
		tasks.forEach(t -> executor.submit(t));
		executor.shutdown();
	}
}
