package bln.integration.imp.oic.reader;

import bln.integration.entity.*;
import bln.integration.imp.AbstractReader;
import bln.integration.imp.gateway.ValueGateway;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.imp.BatchHelper;
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
@Transactional
public class AutoOicPtReader extends AbstractReader<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(AutoOicPtReader.class);
	private final WorkListHeaderRepository headerRepository;
	private final BatchHelper batchHelper;
	private final ValueGateway<PeriodTimeValueRaw> ptOicImpGateway;
	private final Integer groupCount = 4000;

	@Override
	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		super.read(headerId);
	}

	@SuppressWarnings("Duplicates")
	@Override
	protected Long request(List<List<MeteringPointCfg>> groupsPoints, Batch batch) throws Exception {
		Long recCount = 0l;
		for (int i = 0; i < groupsPoints.size(); i++) {
			logger.info("group of points: " + (i + 1));
			List<PeriodTimeValueRaw> list = ptOicImpGateway.request(batch.getWorkListHeader().getConfig(), groupsPoints.get(i));
			batchHelper.ptSave(list, batch);
			recCount = recCount + list.size();
		}
		return recCount;
	}

	@Override
	protected List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, LocalDateTime endDateTime) {
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

	@Override
	protected Logger logger() { return logger; }

	@Override
	protected BatchHelper batchHelper() { return batchHelper; }

	@Override
	protected WorkListHeaderRepository headerRepository() { return headerRepository; }

	@Override
	protected Integer groupCount() { return groupCount; }
}