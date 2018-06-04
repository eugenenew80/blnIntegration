package bln.integration.imp.emcos.schedule;

import bln.integration.entity.AtTimeValueRaw;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.WorkListHeader;
import bln.integration.entity.enums.*;
import bln.integration.imp.Reader;
import bln.integration.repo.WorkListHeaderRepository;
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
public class ManualEmcosImp implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ManualEmcosImp.class);
	private final Reader<AtTimeValueRaw> manualEmcosAtReader;
	private final Reader<PeriodTimeValueRaw> manualEmcosPtReader;
	private final WorkListHeaderRepository headerRepository;
	public  final AtomicBoolean isEnable = new AtomicBoolean(true);
	private boolean isRunning = false;

	public void run() {
		if (!isEnable.get() || isRunning) return;

		try {
			isRunning = true;

			List<WorkListHeader> headers = headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
				SourceSystemEnum.EMCOS,
				DirectionEnum.IMPORT,
				WorkListTypeEnum.USER
			);

			List<Runnable> atTasks = headers.stream()
				.filter(h -> h.getParamType() == ParamTypeEnum.AT && h.getActive() && h.getConfig() != null && h.getStatus() == BatchStatusEnum.W)
				.map(h -> (Runnable) () -> manualEmcosAtReader.read(h.getId()))
				.collect(toList());

			List<Runnable> ptTasks = headers.stream()
				.filter(h -> h.getParamType() == ParamTypeEnum.PT && h.getActive() && h.getConfig() != null && h.getStatus() == BatchStatusEnum.W)
				.map(h -> (Runnable) () -> manualEmcosPtReader.read(h.getId()))
				.collect(toList());

			submit(atTasks);
			submit(ptTasks);
		}

		catch (Exception e) {
			logger.error("run failed: " + e.getMessage());
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
