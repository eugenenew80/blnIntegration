package bln.integration.imp.emcos.schedule;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.imp.*;
import bln.integration.repo.WorkListHeaderRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class AutoEmcosImp implements Runnable  {
	private static final Logger logger = LoggerFactory.getLogger(AutoEmcosImp.class);
	private final Reader<AtTimeValueRaw> autoEmcosAtReader;
	private final Reader<PeriodTimeValueRaw> autoEmcosPtReader;
	private final WorkListHeaderRepo headerRepo;
	public  final AtomicBoolean isEnable = new AtomicBoolean(true);
	private boolean isRunning = false;

	public void run() {
		if (!isEnable.get() || isRunning) return;

		try {
			isRunning = true;

			List<WorkListHeader> headers = headerRepo.findAllBySourceSystemCodeAndDirectionAndWorkListType(
				SourceSystemEnum.EMCOS,
				DirectionEnum.IMPORT,
				WorkListTypeEnum.SYS
			);

			List<Runnable> atTasks = headers.stream()
				.filter(h -> h.getParamType() == ParamTypeEnum.AT && h.getActive() && h.getConfig() != null && h.getStatus() != BatchStatusEnum.P)
				.map(h -> (Runnable) () -> autoEmcosAtReader.read(h.getId()))
				.collect(toList());

			List<Runnable> ptTasks = headers.stream()
				.filter(h -> h.getParamType() == ParamTypeEnum.PT && h.getActive() && h.getConfig() != null && h.getStatus() != BatchStatusEnum.P)
				.map(h -> (Runnable) () -> autoEmcosPtReader.read(h.getId()))
				.collect(toList());

			submit(atTasks);
			submit(ptTasks);
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
