package bln.integration.imp.emcos.schedule;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.imp.*;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class AutoEmcosImp implements Runnable  {
	private final Reader<AtTimeValueRaw> autoEmcosAtReader;
	private final Reader<PeriodTimeValueRaw> autoEmcosPtReader;
	private final WorkListHeaderRepository headerRepository;

	@Value("${bln.integration.imp.emcos.schedule.autoEmcosImp}")
	private boolean enable;

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
			.filter(h -> h.getParamType() == ParamTypeEnum.PT && h.getActive() && h.getConfig() != null && h.getStatus()!=BatchStatusEnum.P)
			.map(h -> (Runnable) () -> autoEmcosPtReader.read(h.getId()))
			.collect(toList());

		if (!atTasks.isEmpty()) submit(atTasks);
		if (!ptTasks.isEmpty()) submit(ptTasks);
    }

    private void submit(List<Runnable> tasks) {
		ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
		tasks.forEach(t -> executor.submit(t));
		executor.shutdown();
	}
}
