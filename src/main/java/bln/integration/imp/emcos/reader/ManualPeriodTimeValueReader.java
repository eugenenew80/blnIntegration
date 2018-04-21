package bln.integration.imp.emcos.reader;

import bln.integration.entity.Batch;
import bln.integration.entity.WorkListLine;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.emcos.PeriodTimeValueImpGateway;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.PeriodTimeValueRawRepository;
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
public class ManualPeriodTimeValueReader implements Reader<PeriodTimeValueRaw> {
	private final PeriodTimeValueRawRepository ptValueRawRepository;
	private final WorkListHeaderRepository headerRepository;
	private final BatchHelper batchHelper;
	private final PeriodTimeValueImpGateway ptGateway;

	private static final Logger logger = LoggerFactory.getLogger(ManualPeriodTimeValueReader.class);

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
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
					List<PeriodTimeValueRaw> ptList = ptGateway
						.config(header.getConfig())
						.points(points)
						.request();

					batchHelper.savePtData(batch, ptList);
					recCount = recCount + ptList.size();

					batchHelper.updateBatch(batch, null, recCount);
					ptValueRawRepository.updateLastDate(batch.getId());
					ptValueRawRepository.load(batch.getId());
				}
				catch (Exception e) {
					logger.error("read failed: " + e.getMessage());
					batchHelper.updateBatch(batch, e, null);
				}
			});

		logger.debug("read completed");
	}


	private List<MeteringPointCfg> buildPoints() {
		return buildPoints();
	}

	private List<MeteringPointCfg> buildPoints(List<WorkListLine> lines) {
		List<MeteringPointCfg> points = new ArrayList<>();
		lines.stream()
			.filter(line -> line.getParam().getIsPt())
			.forEach(line -> {
				MeteringPointCfg mpc = batchHelper.buildPointCfg(
					line,
					line.getStartDate(),
					line.getEndDate(),
					ParamTypeEnum.PT,
					900
				);

				if (!(mpc.getStartTime().isEqual(mpc.getEndTime()) || mpc.getStartTime().isAfter(mpc.getEndTime())))
					points.add(mpc);
			});

		return points;
	}
}
