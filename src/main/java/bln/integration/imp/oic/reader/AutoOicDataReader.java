package bln.integration.imp.oic.reader;

import bln.integration.entity.Batch;
import bln.integration.entity.WorkListLine;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.TelemetryRaw;
import bln.integration.entity.enums.*;
import bln.integration.gateway.oic.OicDataImpGateway;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.LastLoadInfoRepository;
import bln.integration.repo.PeriodTimeValueRawRepository;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoOicDataReader implements Reader<TelemetryRaw> {
	private final PeriodTimeValueRawRepository valueRawRepository;
	private final LastLoadInfoRepository loadInfoRepository;
	private final WorkListHeaderRepository headerRepository;
	private final OicDataImpGateway oicImpGateway;
	private final BatchHelper batchHelper;

	private static final Logger logger = LoggerFactory.getLogger(AutoOicDataReader.class);

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void read() {
		logger.info("read started");

		headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.OIC,
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
					logger.info("List of points is empty, import data stopped");
					return;
				}

				LocalDateTime startDateTime = buildStartTime();
				LocalDateTime endDateTime = buildEndDateTime();
				if (startDateTime.isAfter(endDateTime)){
					logger.info("Import media is not required, import data stopped");
					return;
				}

				Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.PT));
				try {
					List<PeriodTimeValueRaw> ptList = oicImpGateway
						.points(buildPoints(lines))
						.startDateTime(startDateTime)
						.endDateTime(endDateTime)
						.arcType("MIN-3")
						.request();

					ptList.forEach(t -> t.setBatch(batch));
					valueRawRepository.save(ptList);
					batchHelper.updateBatch(batch, (long) ptList.size() );
					valueRawRepository.updateLastDate(batch.getId());
					valueRawRepository.load(batch.getId());
				}
				catch (Exception e) {
					logger.error("read failed: " + e.getMessage());
					batchHelper.errorBatch(batch, e);
				}
			});

		logger.info("read completed");
    }

	private List<String> buildPoints(List<WorkListLine> lines) {
		return Arrays.asList("1", "2");
	}

	private LocalDateTime buildStartTime() {
		LocalDateTime startDateTime = loadInfoRepository.findAll().stream()
			.filter(l -> l.getSourceSystemCode().equals("OIC"))
			.map(l -> l.getLastLoadDate())
			.max(LocalDateTime::compareTo)
			.orElse(null);

		if (startDateTime==null)
			return buildEndDateTime().minusMonths(1);

		long step = 180l;
		startDateTime = startDateTime
			.minusSeconds(startDateTime.getMinute()*60 - Math.round(startDateTime.getMinute()*60 / step) * step)
			.plusSeconds(step);

		return startDateTime;
	}

	private LocalDateTime buildEndDateTime() {
		LocalDateTime endDateTime = LocalDateTime.now()
			.truncatedTo(ChronoUnit.MINUTES);

		long step = 180l;
		endDateTime = endDateTime
			.minusSeconds(endDateTime.getMinute()*60 - Math.round(endDateTime.getMinute()*60 / step) * step);

		return endDateTime;
	}
}
