package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.AtTimeValueGateway;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.IntStream.range;

@Service
@RequiredArgsConstructor
public class AutoAtTimeValueReader implements Reader<AtTimeValueRaw> {
	private static final Logger logger = LoggerFactory.getLogger(AutoAtTimeValueReader.class);
	private static final int groupCount = 6000;
	private final AtTimeValueRawRepository valueRepository;
	private final LastLoadInfoRepository lastLoadInfoRepository;
	private final ParameterConfRepository parameterConfRepository;
	private final WorkListHeaderRepository headerRepository;
	private final AtTimeValueGateway valueGateway;
	private final BatchHelper batchHelper;
	private final EntityManager entityManager;

	@Transactional(propagation=Propagation.NOT_SUPPORTED)
	public void read(Long headerId) {
		WorkListHeader header = headerRepository.findOne(headerId);

		logger.info("read started");
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

			Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.AT));
			Long recCount = 0l;
			try {
				for (int i = 0; i < groupsPoints.size(); i++) {
					logger.info("group of points: " + (i + 1));

					List<AtTimeValueRaw> list = valueGateway
						.config(header.getConfig())
						.points(groupsPoints.get(i))
						.request();

					save(list, batch);
					recCount = recCount + list.size();
				}

				batchHelper.updateBatch(batch, recCount);
				updateLastDate(batch);
				load(batch);
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

		logger.info("read completed");
    }

	@SuppressWarnings("Duplicates")
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	void save(List<AtTimeValueRaw> list, Batch batch) {
		logger.info("saving records started");
		LocalDateTime now = LocalDateTime.now();
		list.forEach(t -> {
			t.setBatch(batch);
			t.setCreateDate(now);
		});
		valueRepository.save(list);
		logger.info("saving records completed");
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	void updateLastDate(Batch batch) {
		logger.info("updateLastDate started");
		lastLoadInfoRepository.updateAtLastDate(batch.getId());
		logger.info("updateLastDate completed");
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	void load(Batch batch) {
		logger.info("load started");
		valueRepository.load(batch.getId());
		logger.info("load completed");
	}

	@Transactional(propagation=Propagation.REQUIRED, readOnly = true)
	List<MeteringPointCfg> buildPoints(List<WorkListLine> lines, LocalDateTime endDateTime) {
		List<ParameterConf> confList = parameterConfRepository.findAllBySourceSystemCodeAndParamType(
			SourceSystemEnum.EMCOS,
			ParamTypeEnum.AT
		);

		entityManager.clear();
		List<LastLoadInfo> lastLoadInfoList = lastLoadInfoRepository
			.findAllBySourceSystemCode(SourceSystemEnum.EMCOS);

		List<MeteringPointCfg> points = new ArrayList<>();
		lines.stream()
			.filter(line -> line.getParam().getIsAt())
			.filter(line -> line.getMeteringPoint().getExternalCode().equals("130321022010010052"))
			.forEach(line -> {
				ParameterConf parameterConf = confList.stream()
					.filter(c -> c.getParam().equals(line.getParam()))
					.filter(c -> c.getInterval() == null)
					.findFirst()
					.orElse(null);

				LastLoadInfo lastLoadInfo = lastLoadInfoList.stream()
					.filter(l -> l.getSourceMeteringPointCode().equals(line.getMeteringPoint().getExternalCode()))
					.filter(l -> l.getSourceParamCode().equals(parameterConf.getSourceParamCode()))
					.findFirst()
					.orElse(null);

				MeteringPointCfg mpc = MeteringPointCfg.fromLine(
					line,
					parameterConf,
					buildStartTime(lastLoadInfo),
					endDateTime
				);

				if (mpc!=null && !mpc.getEndTime().isBefore(mpc.getStartTime()))
					points.add(mpc);
			});

		return points;
	}

	private List<List<MeteringPointCfg>> splitPoints(List<MeteringPointCfg> points) {
		return range(0, points.size())
			.boxed()
			.collect(groupingBy(index -> index / groupCount))
			.values()
			.stream()
			.map(indices -> indices
					.stream()
					.map(points::get)
					.collect(toList()))
			.collect(toList());
	}

	private LocalDateTime buildStartTime(LastLoadInfo lastLoadInfo) {
		if (lastLoadInfo!=null && lastLoadInfo.getLastLoadDate()!=null)
			return lastLoadInfo.getLastLoadDate()
				.plusDays(1)
				.truncatedTo(ChronoUnit.DAYS);

		LocalDate now = LocalDate.now(ZoneId.of("UTC+1"));
		return now.minusDays(now.getDayOfMonth()).minusMonths(1).atStartOfDay();
	}

	private LocalDateTime buildEndDateTime() {
		return LocalDate.now(ZoneId.of("UTC+1")).plusDays(1).atStartOfDay();
	}
}
