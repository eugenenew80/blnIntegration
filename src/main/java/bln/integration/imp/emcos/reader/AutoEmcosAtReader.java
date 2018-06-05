package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.imp.AbstractAutoReader;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.imp.gateway.ValueGateway;
import bln.integration.imp.BatchHelper;
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
public class AutoEmcosAtReader extends AbstractAutoReader<AtTimeValueRaw> {
	private static final Logger logger = LoggerFactory.getLogger(AutoEmcosAtReader.class);
	private final WorkListHeaderRepo headerRepo;
	private final BatchHelper batchHelper;
	private final ValueGateway<AtTimeValueRaw> atEmcosImpGateway;
	private final Integer groupCount = 500;

	@Override
	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		super.read(headerId);
	}

	@Override
	protected List<AtTimeValueRaw> request(Batch batch, List<MeteringPointCfg> points) throws Exception {
		return atEmcosImpGateway.request(batch.getWorkListHeader().getConfig(), points);
	}

	@Override
	protected void save(Batch batch, List<AtTimeValueRaw> list) {
		batchHelper.save(batch, list, null);
	}

	@Override
    protected List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, LocalDateTime endDateTime) {
		return batchHelper.buildPointsCfg(
			header,
			lastLoadInfo -> {
				if (lastLoadInfo!=null && lastLoadInfo.getLastLoadDate()!=null)
					return lastLoadInfo.getLastLoadDate()
						.plusDays(1)
						.truncatedTo(ChronoUnit.DAYS);

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
