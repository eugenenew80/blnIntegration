package bln.integration.imp.emcos.reader;

import bln.integration.entity.Batch;
import bln.integration.entity.ParameterConf;
import bln.integration.entity.WorkListLine;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.emcos.PeriodTimeValueImpGateway;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.ParameterConfRepository;
import bln.integration.repo.PeriodTimeValueRawRepository;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualPeriodTimeValueReader implements Reader<PeriodTimeValueRaw> {
	private final PeriodTimeValueRawRepository valueRepository;
	private final WorkListHeaderRepository headerRepository;
	private final ParameterConfRepository parameterConfRepository;
	private final PeriodTimeValueImpGateway valueGateway;
	private final BatchHelper batchHelper;

	private static final Logger logger = LoggerFactory.getLogger(ManualPeriodTimeValueReader.class);

	@Transactional
	public void read() {
		logger.debug("read started");

		headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.EMCOS,
			DirectionEnum.IMPORT,
			WorkListTypeEnum.USER
		).stream()
			.filter(h -> h.getActive() && h.getPtStatus()==BatchStatusEnum.W && h.getConfig()!=null)
			.forEach(header -> {
				logger.info("Import media started");
				logger.info("headerId: " + header.getId());
				logger.info("url: " + header.getConfig().getUrl());
				logger.info("user: " + header.getConfig().getUserName());

				List<WorkListLine> lines = header.getLines();
				if (lines.size()==0) {
					logger.info("List of points is empty, import media stopped");
					return;
				}

				List<MeteringPointCfg> points = buildPoints(lines);
				if (points.size()==0) {
					logger.info("Import media is not required, import media stopped");
					return;
				}

				Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.PT));

				Long recCount = 0l;
				try {
					List<PeriodTimeValueRaw> ptList = valueGateway
						.config(header.getConfig())
						.points(points)
						.request();

					ptList.forEach(t -> t.setBatch(batch));
					valueRepository.bulkSave(ptList);
					recCount = recCount + ptList.size();

					batchHelper.updateBatch(batch, recCount);
					valueRepository.updateLastDate(batch.getId());
					valueRepository.load(batch.getId());
				}
				catch (Exception e) {
					logger.error("read failed: " + e.getMessage());
					batchHelper.errorBatch(batch, e);
				}
			});

		logger.debug("read completed");
	}


	private List<MeteringPointCfg> buildPoints(List<WorkListLine> lines) {
		List<ParameterConf> confList = parameterConfRepository.findAllBySourceSystemCodeAndParamType(
			SourceSystemEnum.EMCOS,
			ParamTypeEnum.PT
		);

		List<MeteringPointCfg> points = new ArrayList<>();
		lines.stream()
			.filter(line -> line.getParam().getIsPt())
			.forEach(line -> {
				ParameterConf parameterConf = confList.stream()
					.filter(c -> c.getParam().equals(line.getParam()))
					.filter(c -> c.getInterval().equals(900))
					.findFirst()
					.orElse(null);

				MeteringPointCfg mpc = MeteringPointCfg.fromLine(
					line,
					parameterConf,
					line.getStartDate(),
					line.getEndDate()
				);

				if (!(mpc.getStartTime().isEqual(mpc.getEndTime()) || mpc.getStartTime().isAfter(mpc.getEndTime())))
					points.add(mpc);
			});

		return points;
	}
}
