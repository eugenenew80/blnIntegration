package bln.integration.imp.oic.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.oic.OicDataImpGateway;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoOicDataReader implements Reader<TelemetryRaw> {
    private static final Logger logger = LoggerFactory.getLogger(AutoOicDataReader.class);
	private final WorkListHeaderRepository headerRepository;
    private final OicDataImpGateway oicImpGateway;
    private final BatchHelper batchHelper;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void read(Long headerId) {
		logger.info("read started");
		logger.info("headerId: " + headerId);

		WorkListHeader header = headerRepository.findOne(headerId);
		if (header.getConfig() == null) {
			logger.warn("Config is empty, request stopped");
			return;
		}

		if (header==null) {
			logger.info("Work list not found");
			return;
		}

		List<WorkListLine> lines = header.getLines();
		if (lines.size()==0) {
			logger.info("List of lines is empty, import data stopped");
			return;
		}

		LocalDateTime endDateTime = LocalDateTime.now(ZoneId.of(header.getTimeZone()))
			.truncatedTo(ChronoUnit.HOURS);

		List<MeteringPointCfg> points = buildPointsCfg(header, endDateTime);
		if (points.size()==0) {
			logger.info("List of points is empty, import data stopped");
			return;
		}

		logger.info("url: " + header.getConfig().getUrl());
		logger.info("user: " + header.getConfig().getUserName());

		Batch batch = batchHelper.createBatch(new Batch(header));
		try {
			List<PeriodTimeValueRaw> list = oicImpGateway.request(header.getConfig(), points, header.getInterval());
			batchHelper.ptSave(list, batch);
			batchHelper.updateBatch(batch, (long)list.size());
			batchHelper.updatePtLastDate(batch);
			batchHelper.ptLoad(batch);
		}
		catch (Exception e) {
			logger.error("read failed: " + e.getMessage());
			batchHelper.errorBatch(batch, e);
		}
		finally {
			System.gc();
		}

		logger.info("read completed");
    }

	private List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, LocalDateTime endDateTime) {
		return batchHelper.buildPointsCfg(
			header,
			lastLoadInfo -> {
				if (lastLoadInfo!=null && lastLoadInfo.getLastLoadDate()!=null) {
					LocalDateTime lastLoadDate = lastLoadInfo.getLastLoadDate();
					return lastLoadDate.truncatedTo(ChronoUnit.HOURS).plusHours(1);
				}
				LocalDate now = LocalDate.now(ZoneId.of(header.getTimeZone()));
				return now.minusDays(now.getDayOfMonth()-1).atStartOfDay();
			},
			() -> endDateTime
		);
	}
}
