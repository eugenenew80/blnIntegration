package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.*;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.LastLoadInfoRepository;
import bln.integration.repo.ParameterConfRepository;
import bln.integration.repo.PeriodTimeValueRawRepository;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

@Service
@RequiredArgsConstructor
public class AutoPeriodTimeValueReader implements Reader<PeriodTimeValueRaw> {
	private final PeriodTimeValueRawRepository valueRepository;
	private final WorkListHeaderRepository headerRepository;
	private final LastLoadInfoRepository lastLoadInfoRepository;
	private final ParameterConfRepository parameterConfRepository;
	private final PeriodTimeValueImpGateway valueGateway;
	private final BatchHelper batchHelper;

	private static final Logger logger = LoggerFactory.getLogger(AutoPeriodTimeValueReader.class);

	@Transactional
	public void read() {
		logger.info("read started");

		headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.EMCOS,
			DirectionEnum.IMPORT,
			WorkListTypeEnum.SYS
		).stream()
		.filter(h -> h.getActive() && h.getConfig()!=null)
		.forEach(header -> {
			logger.info("headerId: " + header.getId());
			logger.info("url: " + header.getConfig().getUrl());
			logger.info("user: " + header.getConfig().getUserName());

			List<WorkListLine> lines = header.getLines();
			if (lines.size()==0) {
				logger.info("List of lines is empty, import data stopped");
				return;
			}

			LocalDateTime endDateTime = buildEndDateTime();
			List<MeteringPointCfg> points = buildPoints(lines, endDateTime);
			if (points.size()==0) {
				logger.info("List of points is empty, import data stopped");
				return;
			}

			LocalDateTime lastLoadDateTime = points.stream()
				.map(p -> p.getStartTime())
				.max(LocalDateTime::compareTo)
				.orElse(endDateTime);

			LocalDateTime requestedDateTime = lastLoadDateTime.plusDays(1);
			if (requestedDateTime.isAfter(endDateTime))
				requestedDateTime=endDateTime;

			while (!endDateTime.isBefore(requestedDateTime)) {
				logger.info("requested date: " + requestedDateTime);

				points = buildPoints(lines, requestedDateTime);
				final List<List<MeteringPointCfg>> groupsPoints = splitPoints(points);

				Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.PT));
				Long recCount = 0l;
				try {
					for (int i = 0; i < groupsPoints.size(); i++) {
						logger.info("group of points: " + (i + 1));

						List<PeriodTimeValueRaw> ptList = valueGateway
							.config(header.getConfig())
							.points(groupsPoints.get(i))
							.request();

						ptList.forEach(t -> t.setBatch(batch));
						valueRepository.bulkSave(ptList);
						recCount = recCount + ptList.size();
					}

					batchHelper.updateBatch(batch, recCount);
					onBatchCompleted(batch);
				}
				catch (Exception e) {
					logger.error("read failed: " + e.getMessage());
					batchHelper.errorBatch(batch, e);
					break;
				}

				if (requestedDateTime.isEqual(endDateTime))
					break;

				requestedDateTime = requestedDateTime.plusDays(1);
				if (requestedDateTime.isAfter(endDateTime))
					requestedDateTime=endDateTime;
			}
		});

		logger.info("read completed");
	}

	private List<List<MeteringPointCfg>> splitPoints(List<MeteringPointCfg> points) {
		return range(0, points.size())
			.boxed()
			.collect(groupingBy(index -> index / 100))
			.values()
			.stream()
			.map(indices -> indices
				.stream()
				.map(points::get)
				.collect(toList()))
			.collect(toList());
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void onBatchCompleted(Batch batch) {
		logger.info("onBatchCompleted started");
		valueRepository.updateLastDate(batch.getId());
		valueRepository.load(batch.getId());
		logger.info("onBatchCompleted completed");
	}

	private List<MeteringPointCfg> buildPoints(List<WorkListLine> lines, LocalDateTime endDateTime) {
		List<ParameterConf> confList = parameterConfRepository.findAllBySourceSystemCodeAndParamType(
			SourceSystemEnum.EMCOS,
			ParamTypeEnum.PT
		);

		List<MeteringPointCfg> points = new ArrayList<>();
		lines.stream()
			.filter(line -> line.getParam().getIsPt())
			.forEach(line -> {
				ParameterConf parameterConf = confList.stream()
					.filter(c -> c.getParam().equals(line.getParam()))
					.filter(c -> c.getInterval().equals(900))
					.findFirst()
					.orElse(null);

				LastLoadInfo lastLoadInfo = lastLoadInfoRepository.findBySourceSystemCodeAndSourceMeteringPointCodeAndSourceParamCode(
					parameterConf.getSourceSystemCode(),
					line.getMeteringPoint().getExternalCode(),
					parameterConf.getSourceParamCode()
				);

				MeteringPointCfg mpc = MeteringPointCfg.fromLine(
					line,
					parameterConf,
					buildStartTime(lastLoadInfo),
					endDateTime
				);

				if (mpc!=null) points.add(mpc);
			});

		return points;
	}

	private LocalDateTime buildStartTime(LastLoadInfo lastLoadInfo) {
		if (lastLoadInfo!=null && lastLoadInfo.getLastLoadDate() !=null) {
			LocalDateTime lastLoadDate = lastLoadInfo.getLastLoadDate();
			return lastLoadDate.getMinute() < 45
				? lastLoadDate.truncatedTo(ChronoUnit.HOURS)
				: lastLoadDate.plusMinutes(15);
		}

		LocalDate now = LocalDate.now(ZoneId.of("UTC+1"));
		return now.minusDays(now.getDayOfMonth()-1).atStartOfDay();
	}

	private LocalDateTime buildEndDateTime() {
		return LocalDateTime.now(ZoneId.of("UTC+1")).minusMinutes(15).truncatedTo(ChronoUnit.HOURS);
	}
}