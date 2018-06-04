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
public class ManualEmcosAtReader extends AbstractManualReader<AtTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(ManualEmcosAtReader.class);
	private final WorkListHeaderRepo headerRepo;
	private final ValueGateway<AtTimeValueRaw> atEmcosImpGateway;
    private final BatchHelper batchHelper;

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
	protected List<MeteringPointCfg> buildPointsCfg(WorkListHeader header) {
		return batchHelper.buildPointsCfg(header, (l) -> header.getStartDate(), ()  -> header.getEndDate());
	}

	@Override
	protected Logger logger() { return logger; }

	@Override
	protected BatchHelper batchHelper() { return batchHelper; }

	@Override
	protected WorkListHeaderRepo headerRepo() { return headerRepo; }
}
