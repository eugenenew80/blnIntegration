package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.imp.AbstractManualReader;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualEmcosPtReader extends AbstractManualReader<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(ManualEmcosPtReader.class);
	private final WorkListHeaderRepo headerRepo;
	private final ValueGateway<PeriodTimeValueRaw> ptEmcosImpGateway;
    private final BatchHelper batchHelper;

    @Override
	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		super.read(headerId);
    }

	@Override
	protected List<PeriodTimeValueRaw> request(Batch batch, List<MeteringPointCfg> points) throws Exception {
		return ptEmcosImpGateway.request(batch.getWorkListHeader().getConfig(), points);
	}

	@Override
	protected void save(Batch batch, List<PeriodTimeValueRaw> list) {
		batchHelper.save(batch, null, list);
	}

    @Override
	protected List<MeteringPointCfg> buildPointsCfg(WorkListHeader header) {
		return batchHelper.buildPointsCfg(header, (l) -> header.getStartDate(), () -> header.getEndDate());
	}

	@Override
	protected Logger logger() { return logger; }

	@Override
	protected BatchHelper batchHelper() { return batchHelper; }

	@Override
	protected WorkListHeaderRepo headerRepo() { return headerRepo; }
}
