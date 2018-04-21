package bln.integration.imp.emcos.reader;

import bln.integration.entity.Batch;
import bln.integration.entity.WorkListLine;
import bln.integration.entity.AtTimeValueRaw;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.AtTimeValueGateway;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.AtTimeValueRawRepository;
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
	private final AtTimeValueRawRepository atValueRawRepository;
	private final WorkListHeaderRepository headerRepository;
	private final BatchHelper batchHelper;
	private final AtTimeValueGateway atValueGateway;

	private static final Logger logger = LoggerFactory.getLogger(ManualAtTimeValueReader.class);

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void read() {
		logger.debug("read started");

		headerRepository.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.EMCOS,
			DirectionEnum.IMPORT,
			WorkListTypeEnum.USER
		).stream()
			.filter(h -> h.getActive() && h.getAtStatus()==BatchStatusEnum.W && h.getConfig()!=null)
			.forEach(header -> {
				logger.info("Import data started");
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

				Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.AT));

				Long recCount = 0l;
				try {
					List<AtTimeValueRaw> atList = atValueGateway
						.config(header.getConfig())
						.points(points)
						.request();

					batchHelper.saveAtData(batch, atList);
					recCount = recCount + atList.size();

					batchHelper.updateBatch(batch, null, recCount);
					atValueRawRepository.updateLastDate(batch.getId());
					atValueRawRepository.load(batch.getId());
				}
				catch (Exception e) {
					logger.error("read failed: " + e.getMessage());
					batchHelper.updateBatch(batch, e, null);
				}
			});

		logger.debug("read completed");
	}


	private List<MeteringPointCfg> buildPoints(List<WorkListLine> lines) {
		List<MeteringPointCfg> points = new ArrayList<>();
		lines.stream()
			.filter(line -> line.getParam().getIsAt())
			.forEach(line -> {
				MeteringPointCfg mpc = batchHelper.buildPointCfg(
					line,
					line.getStartDate(),
					line.getEndDate(),
					ParamTypeEnum.AT,
					null
				);

				if (!(mpc.getStartTime().isEqual(mpc.getEndTime()) || mpc.getStartTime().isAfter(mpc.getEndTime())))
					points.add(mpc);
			});

		return points;
	}
}
