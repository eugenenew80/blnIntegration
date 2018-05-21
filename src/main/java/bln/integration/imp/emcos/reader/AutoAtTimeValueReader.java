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
@Transactional
public class AutoAtTimeValueReader implements Reader<AtTimeValueRaw> {
	private static final Logger logger = LoggerFactory.getLogger(AutoAtTimeValueReader.class);
	private static final int groupCount = 2000;
	private final LastLoadInfoRepository lastLoadInfoRepository;
	private final ParameterConfRepository parameterConfRepository;
	private final WorkListHeaderRepository headerRepository;
	private final LastRequestedDateRepository lastRequestedDateRepository;
	private final AtTimeValueGateway valueGateway;
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
		if (lines.size()==0) {
			logger.info("List of lines is empty, import data stopped");
			return;
		}

		LocalDateTime endDateTime = buildEndDateTime();
		List<MeteringPointCfg> points = buildPointsCfg(lines, endDateTime);

		if (points.size()==0) {
			logger.info("List of points is empty, import data stopped");
			return;
		}

		LastRequestedDate lastRequestedDate = lastRequestedDateRepository.findAllByWorkListHeaderIdAndParamType(headerId, ParamTypeEnum.AT)
			.stream()
			.findFirst()
			.orElseGet(() -> {
				LastRequestedDate d = new LastRequestedDate();
				d.setWorkListHeader(header);
				d.setParamType(ParamTypeEnum.AT);
				d.setLastRequestedDate(endDateTime);
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

			Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.AT));
			Long recCount = 0l;
			try {
				for (int i = 0; i < groupsPoints.size(); i++) {
					logger.info("group of points: " + (i + 1));
					List<AtTimeValueRaw> list = valueGateway.request(header.getConfig(), groupsPoints.get(i));
					batchHelper.atSave(list, batch);
					recCount = recCount + list.size();
				}

				batchHelper.updateBatch(batch, recCount);
				batchHelper.updateAtLastDate(batch);
				batchHelper.atLoad(batch);
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
					buildStartDateTime(lastLoadInfo),
					endDateTime
				);

				if (mpc!=null && !mpc.getEndTime().isBefore(mpc.getStartTime()))
					points.add(mpc);
			});

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
		if (lastLoadInfo!=null && lastLoadInfo.getLastLoadDate()!=null)
			return lastLoadInfo.getLastLoadDate()
				.plusDays(1)
				.truncatedTo(ChronoUnit.DAYS);

		LocalDate now = LocalDate.now(ZoneId.of("UTC+1"));
		return now.minusDays(now.getDayOfMonth()).minusMonths(2).atStartOfDay();
	}

	private LocalDateTime buildEndDateTime() {
		LocalDateTime endDateTime = LocalDate.now(ZoneId.of("UTC+1")).atStartOfDay();
		return endDateTime;
	}
}
