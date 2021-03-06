package bln.integration.imp.oic.reader;

import bln.integration.entity.*;
import bln.integration.imp.AbstractAutoReader;
import bln.integration.imp.gateway.ValueGateway;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.imp.BatchHelper;
import bln.integration.repo.WorkListHeaderRepo;
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
@Transactional
public class AutoOicPtReader extends AbstractAutoReader<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(AutoOicPtReader.class);
	private final WorkListHeaderRepo headerRepo;
	private final BatchHelper batchHelper;
	private final ValueGateway<PeriodTimeValueRaw> ptOicImpGateway;
	private final Integer groupCount = 4000;

	@Override
	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		super.read(headerId);
	}

	@Override
	protected List<PeriodTimeValueRaw> request(Batch batch, List<MeteringPointCfg> points) throws Exception {
		return ptOicImpGateway.request(batch.getWorkListHeader().getConfig(), points);
	}

	@Override
	protected void save(Batch batch, List<PeriodTimeValueRaw> list) {
		batchHelper.save(batch, null, list);
	}

	@Override
	protected List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, LocalDateTime endDateTime) {
		return batchHelper.buildPointsCfg(
			header,
			lastLoadInfo -> {
				if (lastLoadInfo!=null && lastLoadInfo.getLastLoadDate()!=null)
					return lastLoadInfo.getLastLoadDate()
						.plusHours(1)
						.truncatedTo(ChronoUnit.HOURS);

				return LocalDate.now(ZoneId.of(header.getConfig().getTimeZone()))
					.withDayOfMonth(1)
					.atStartOfDay();
			},
			() -> endDateTime
		);
	}

	@Override
	protected Logger logger() { return logger; }

	@Override
	protected BatchHelper batchHelper() { return batchHelper; }

	@Override
	protected WorkListHeaderRepo headerRepo() { return headerRepo; }

	@Override
	protected Integer groupCount() { return groupCount; }
}