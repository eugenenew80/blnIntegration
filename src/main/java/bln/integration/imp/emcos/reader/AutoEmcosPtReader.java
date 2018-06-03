package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
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
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AutoEmcosPtReader implements Reader<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(AutoEmcosPtReader.class);
    private static final int groupCount = 300;
	private final WorkListHeaderRepository headerRepository;
	private final PeriodTimeValueImpGateway valueGateway;
    private final BatchHelper batchHelper;

	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		logger.info("read started");
		logger.info("headerId: " + headerId);

		WorkListHeader header = headerRepository.findOne(headerId);
		if (header.getConfig() == null) {
			logger.warn("Config is empty, request stopped");
			return;
		}

		if (header == null) {
			logger.info("Work list not found");
			return;
		}

		List<WorkListLine> lines = header.getLines();
		if (lines.size() == 0) {
			logger.info("List of lines is empty, import data stopped");
			return;
		}

		LocalDateTime endDateTime = LocalDateTime.now(ZoneId.of(header.getTimeZone()))
			.minusMinutes(15)
			.truncatedTo(ChronoUnit.HOURS);

		List<MeteringPointCfg> points = buildPointsCfg(header, endDateTime);
		if (points.size() == 0) {
			logger.info("List of points is empty, import data stopped");
			return;
		}

		LastRequestedDate lastRequestedDate = batchHelper.getLastRequestedDate(header);

		logger.info("url: " + header.getConfig().getUrl());
		logger.info("user: " + header.getConfig().getUserName());
		logger.info("lastRequestedDate: " + lastRequestedDate.getLastRequestedDate());

		LocalDateTime requestedDateTime = lastRequestedDate.getLastRequestedDate().plusDays(1);
		if (requestedDateTime.isAfter(endDateTime))
			requestedDateTime=endDateTime;

		while (!endDateTime.isBefore(requestedDateTime)) {
			logger.info("batch requested date: " + requestedDateTime);

			points = buildPointsCfg(header, requestedDateTime);
			List<List<MeteringPointCfg>> groupsPoints = batchHelper.splitPointsCfg(points, groupCount);

			Batch batch = batchHelper.createBatch(new Batch(header));
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

	private List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, LocalDateTime endDateTime) {
		return batchHelper.buildPointsCfg(
			header,
			lastLoadInfo -> {
				if (lastLoadInfo!=null && lastLoadInfo.getLastLoadDate() !=null) {
					return lastLoadInfo.getLastLoadDate().getMinute() == 45
						? lastLoadInfo.getLastLoadDate().plusMinutes(15)
						: lastLoadInfo.getLastLoadDate().truncatedTo(ChronoUnit.HOURS);
				}
				return LocalDate.now(ZoneId.of(header.getTimeZone()))
					.withDayOfMonth(1)
					.atStartOfDay();
			},
			() -> endDateTime
		);
	}
}