package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.emcos.PeriodTimeValueImpGateway;
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
import java.util.List;
import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class ManualPeriodTimeValueReader implements Reader<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(ManualPeriodTimeValueReader.class);
    private final ParameterConfRepository parameterConfRepository;
	private final WorkListHeaderRepository headerRepository;
    private final PeriodTimeValueImpGateway valueGateway;
    private final BatchHelper batchHelper;

	@Transactional(propagation=Propagation.NOT_SUPPORTED, readOnly = true)
	public void read(Long headerId) {
		WorkListHeader header = headerRepository.findOne(headerId);

		logger.debug("read started");
		logger.info("Import media started");
		logger.info("headerId: " + header.getId());

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

		logger.info("url: " + header.getConfig().getUrl());
		logger.info("user: " + header.getConfig().getUserName());

		Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.PT));
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

		logger.debug("read completed");
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW, readOnly = true)
	List<MeteringPointCfg> buildPointsCfg(List<WorkListLine> lines) {
		List<ParameterConf> parameters = parameterConfRepository.findAllBySourceSystemCodeAndParamType(
			SourceSystemEnum.EMCOS,
			ParamTypeEnum.PT
		);

		List<MeteringPointCfg> points = lines.stream()
			.flatMap(line ->
				parameters.stream()
					.filter(c -> c.getMeteringPoint().equals(line.getMeteringPoint()))
					.filter(c -> c.getInterval().equals(900))
					.map(parameterConf -> {
						MeteringPointCfg mpc = MeteringPointCfg.fromLine(parameterConf);
						mpc.setStartTime(line.getStartDate());
						mpc.setEndTime(line.getEndDate());
						return !mpc.getEndTime().isBefore(mpc.getStartTime()) ? mpc : null;
					})
					.filter(mpc -> mpc != null)
					.collect(toList())
					.stream()
			)
			.collect(toList());

		return points;
	}
}
