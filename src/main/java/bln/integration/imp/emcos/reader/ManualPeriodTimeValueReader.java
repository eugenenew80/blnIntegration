package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.emcos.PeriodTimeValueImpGateway;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualPeriodTimeValueReader implements Reader<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(ManualPeriodTimeValueReader.class);
	private final WorkListHeaderRepository headerRepository;
    private final PeriodTimeValueImpGateway valueGateway;
    private final BatchHelper batchHelper;

	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		WorkListHeader header = headerRepository.findOne(headerId);
		if (header.getConfig() == null) {
			logger.warn("Config is empty, request stopped");
			return;
		}

		List<WorkListLine> lines = header.getLines();
		if (lines.size()==0) {
			logger.debug("List of points is empty, import data stopped");
			return;
		}

		List<MeteringPointCfg> points = buildPointsCfg(header);
		if (points.size()==0) {
			logger.debug("Import data is not required, import data stopped");
			return;
		}

		logger.info("read started");
		logger.info("headerId: " + header.getId());
		logger.info("url: " + header.getConfig().getUrl());
		logger.info("user: " + header.getConfig().getUserName());

		Batch batch = batchHelper.createBatch(new Batch(header));
		try {
			List<PeriodTimeValueRaw> list = valueGateway.request(header.getConfig(), points);
			batchHelper.ptSave(list, batch);
			batchHelper.updateBatch(batch, (long) list.size());
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

	private List<MeteringPointCfg> buildPointsCfg(WorkListHeader header) {
		return batchHelper.buildPointsCfg(header, (l) -> header.getStartDate(), () -> header.getEndDate());
	}
}
