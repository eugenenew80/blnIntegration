package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.AtTimeValueGateway;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.ParameterConfRepository;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualAtTimeValueReader implements Reader<AtTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(ManualAtTimeValueReader.class);
    private final ParameterConfRepository parameterConfRepository;
	private final WorkListHeaderRepository headerRepository;
    private final AtTimeValueGateway valueGateway;
    private final BatchHelper batchHelper;

	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		WorkListHeader header = headerRepository.findOne(headerId);

		logger.debug("read started");
		logger.info("Import data started");
		logger.info("headerId: " + header.getId());
		logger.info("url: " + header.getConfig().getUrl());
		logger.info("user: " + header.getConfig().getUserName());

		List<WorkListLine> lines = header.getLines();
		if (lines.size()==0) {
			logger.info("List of points is empty, import media stopped");
			return;
		}

		List<MeteringPointCfg> points = buildPointsCfg(lines);
		if (points.size()==0) {
			logger.info("Import media is not required, import media stopped");
			return;
		}

		Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.AT));
		try {
			List<AtTimeValueRaw> list = valueGateway.request(header.getConfig(), points);
			batchHelper.atSave(list, batch);
			batchHelper.updateBatch(batch, (long) list.size());
			batchHelper.updateAtLastDate(batch);
			batchHelper.atLoad(batch);
		}
		catch (Exception e) {
			logger.error("read failed: " + e.getMessage());
			batchHelper.errorBatch(batch, e);
		}
		finally {
			System.gc();
		}

		logger.debug("read completed");
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW, readOnly = true)
	List<MeteringPointCfg> buildPointsCfg(List<WorkListLine> lines) {
		List<ParameterConf> confList = parameterConfRepository.findAllBySourceSystemCodeAndParamType(
			SourceSystemEnum.EMCOS,
			ParamTypeEnum.AT
		);

		List<MeteringPointCfg> points = new ArrayList<>();
		lines.stream()
			.filter(line -> line.getParam().getIsAt())
			.forEach(line -> {
				ParameterConf parameterConf = confList.stream()
					.filter(c -> c.getParam().equals(line.getParam()))
					.filter(c -> c.getParamType() == ParamTypeEnum.AT)
					.filter(c -> c.getMeteringPoint().equals(line.getMeteringPoint()))
					.findFirst()
					.orElse(null);

				MeteringPointCfg mpc = MeteringPointCfg.fromLine(parameterConf);
				mpc.setStartTime(line.getStartDate());
				mpc.setEndTime(line.getEndDate());

				if (mpc!=null && !mpc.getEndTime().isBefore(mpc.getStartTime()))
					points.add(mpc);
			});

		return points;
	}
}
