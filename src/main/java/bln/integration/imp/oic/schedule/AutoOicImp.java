package bln.integration.imp.oic.schedule;

import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.WorkListHeader;
import bln.integration.entity.enums.*;
import bln.integration.imp.Reader;
import bln.integration.repo.WorkListHeaderRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class AutoOicImp implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(AutoOicImp.class);
	private final Reader<PeriodTimeValueRaw> autoOicPtReader;
	private final WorkListHeaderRepo headerRepo;
	public  final AtomicBoolean isEnable = new AtomicBoolean(true);
	private boolean isRunning = false;

	public void run() {
		if (!isEnable.get() || isRunning) return;

		try {
			isRunning = true;

			List<WorkListHeader> headers = headerRepo.findAllBySourceSystemCodeAndDirectionAndWorkListType(
				SourceSystemEnum.OIC,
				DirectionEnum.IMPORT,
				WorkListTypeEnum.SYS
			);

			List<Runnable> tasks = headers.stream()
				.filter(h -> h.getParamType() == ParamTypeEnum.PT && h.getActive() && h.getConfig()!= null && h.getStatus()!=BatchStatusEnum.P)
				.map(h -> (Runnable) () -> autoOicPtReader.read(h.getId()))
				.collect(toList());

			submit(tasks);
		}
		catch (Exception e) {
			logger.error("run failed: " + e);
		}
		finally {
			isRunning = false;
		}
	}

	private void submit(List<Runnable> tasks) {
		if (tasks.isEmpty()) return;

		ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
		tasks.forEach(t -> executor.submit(t));
		executor.shutdown();
	}
}
