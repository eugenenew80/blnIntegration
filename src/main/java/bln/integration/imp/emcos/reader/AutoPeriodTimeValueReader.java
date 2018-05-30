package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.*;
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
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

@Service
@RequiredArgsConstructor
@Transactional
public class AutoPeriodTimeValueReader implements Reader<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(AutoPeriodTimeValueReader.class);
    private static final int groupCount = 300;
    private final LastLoadInfoRepository lastLoadInfoRepository;
    private final ParameterConfRepository parameterConfRepository;
	private final WorkListHeaderRepository headerRepository;
    private final LastRequestedDateRepository lastRequestedDateRepository;
	private final PeriodTimeValueImpGateway valueGateway;
    private final BatchHelper batchHelper;
	private final EntityManager entityManager;

	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		logger.info("read started");
		logger.info("headerId: " + headerId);

		WorkListHeader header = headerRepository.findOne(headerId);
		if (header==null) {
			logger.info("Work list not found");
			return;
		}

		List<WorkListLine> lines = header.getLines();
		if (lines.size() == 0) {
			logger.info("List of lines is empty, import data stopped");
			return;
		}

		LocalDateTime endDateTime = buildEndDateTime();
		List<MeteringPointCfg> points = buildPointsCfg(lines, endDateTime);
		if (points.size() == 0) {
			logger.info("List of points is empty, import data stopped");
			return;
		}

		LastRequestedDate lastRequestedDate = lastRequestedDateRepository.findAllByWorkListHeaderIdAndParamType(headerId, ParamTypeEnum.PT)
			.stream()
			.findFirst()
			.orElseGet(() -> {
				LastRequestedDate d = new LastRequestedDate();
				d.setWorkListHeader(header);
				d.setParamType(ParamTypeEnum.PT);
				d.setLastRequestedDate(buildEndDateTimeDef());
				return d;
			});

		logger.info("url: " + header.getConfig().getUrl());
		logger.info("user: " + header.getConfig().getUserName());
		logger.info("lastRequestedDate: " + lastRequestedDate.getLastRequestedDate());

		LocalDateTime requestedDateTime = lastRequestedDate.getLastRequestedDate().plusDays(1);
		if (requestedDateTime.isAfter(endDateTime))
			requestedDateTime=endDateTime;

		while (!endDateTime.isBefore(requestedDateTime)) {
			logger.info("batch requested date: " + requestedDateTime);

			points = buildPointsCfg(lines, requestedDateTime);
			final List<List<MeteringPointCfg>> groupsPoints = splitPointsCfg(points);

			Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.PT));
			Long recCount = 0l;
			try {
				for (int i = 0; i < groupsPoints.size(); i++) {
					logger.info("group of points: " + (i + 1));
					List<PeriodTimeValueRaw> list = valueGateway.request(header.getConfig(), groupsPoints.get(i));
					batchHelper.ptSave(list, batch);
					recCount = recCount + list.size();
				}

				batchHelper.updateBatch(batch, recCount);
				batchHelper.updatePtLastDate(batch);
				batchHelper.ptLoad(batch);
				if (recCount>0) {
					lastRequestedDate.setLastRequestedDate(requestedDateTime);
					batchHelper.updateLastRequestedDate(lastRequestedDate);
				}
			}
			catch (Exception e) {
				logger.error("read failed: " + e.getMessage());
				batchHelper.errorBatch(batch, e);
				break;
			}
			finally {
				System.gc();
			}

			if (requestedDateTime.isEqual(endDateTime))
				break;

			requestedDateTime = requestedDateTime.plusDays(1);
			if (requestedDateTime.isAfter(endDateTime))
				requestedDateTime=endDateTime;
		}

		logger.info("read completed");
	}

	@Transactional(propagation=Propagation.REQUIRED, readOnly = true)
	List<MeteringPointCfg> buildPointsCfg(List<WorkListLine> lines, LocalDateTime endDateTime) {
		List<ParameterConf> parameters = parameterConfRepository.findAllBySourceSystemCodeAndParamType(
			SourceSystemEnum.EMCOS,
			ParamTypeEnum.PT
		);

		entityManager.clear();
		List<LastLoadInfo> lastLoadInfos = lastLoadInfoRepository
			.findAllBySourceSystemCode(SourceSystemEnum.EMCOS);

		List<MeteringPointCfg> points = lines.stream()
			.flatMap(line ->
				parameters.stream()
					.filter(c -> c.getMeteringPoint().equals(line.getMeteringPoint()))
					.filter(c -> c.getInterval().equals(900))
					.map(parameterConf -> {
						LastLoadInfo lastLoadInfo = lastLoadInfos.stream()
							.filter(l -> l.getMeteringPoint().equals(parameterConf.getMeteringPoint()))
							.filter(l -> l.getSourceMeteringPointCode().equals(parameterConf.getSourceMeteringPointCode()))
							.filter(l -> l.getSourceParamCode().equals(parameterConf.getSourceParamCode()))
							.findFirst()
							.orElse(null);

						MeteringPointCfg mpc = MeteringPointCfg.fromLine(parameterConf);
						mpc.setStartTime(buildStartDateTime(lastLoadInfo));
						mpc.setEndTime(endDateTime);
						return mpc.getEndTime().isAfter(mpc.getStartTime()) ? mpc : null;
					})
					.filter(mpc -> mpc != null)
					.collect(toList())
					.stream()
			)
			.collect(toList());

		return points;
	}

	private List<List<MeteringPointCfg>> splitPointsCfg(List<MeteringPointCfg> points) {
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

	private LocalDateTime buildStartDateTime(LastLoadInfo lastLoadInfo) {
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

	private LocalDateTime buildEndDateTimeDef() {
		LocalDate now = LocalDate.now(ZoneId.of("UTC+1"));
		return now.minusDays(now.getDayOfMonth()-1).atStartOfDay();
	}
}