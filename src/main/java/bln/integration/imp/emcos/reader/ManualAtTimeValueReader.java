package bln.integration.imp.emcos.reader;

import bln.integration.entity.Batch;
import bln.integration.entity.ParameterConf;
import bln.integration.entity.WorkListLine;
import bln.integration.entity.AtTimeValueRaw;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.AtTimeValueGateway;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.AtTimeValueRawRepository;
import bln.integration.repo.ParameterConfRepository;
import bln.integration.repo.WorkListHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualAtTimeValueReader implements Reader<AtTimeValueRaw> {
	private final AtTimeValueRawRepository valueRepository;
	private final WorkListHeaderRepository headerRepository;
	private final ParameterConfRepository parameterConfRepository;
	private final AtTimeValueGateway valueGateway;
	private final BatchHelper batchHelper;
	private static final Logger logger = LoggerFactory.getLogger(ManualAtTimeValueReader.class);

	@Transactional(propagation=Propagation.NOT_SUPPORTED)
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
					List<AtTimeValueRaw> list = valueGateway
						.config(header.getConfig())
						.points(points)
						.request();

					save(list, batch);
					recCount = recCount + list.size();

					batchHelper.updateBatch(batch, recCount);
					updateLastDate(batch);
					load(batch);
				}
				catch (Exception e) {
					logger.error("read failed: " + e.getMessage());
					batchHelper.errorBatch(batch, e);
				}
			});

		logger.debug("read completed");
	}

	@Transactional(propagation= Propagation.REQUIRES_NEW)
	private void save(List<AtTimeValueRaw> list, Batch batch) {
		logger.info("saving records started");
		LocalDateTime now = LocalDateTime.now();
		list.forEach(t -> {
			t.setBatch(batch);
			t.setCreateDate(now);
		});
		valueRepository.save(list);
		logger.info("saving records completed");
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	private void updateLastDate(Batch batch) {
		logger.info("updateLastDate started");
		valueRepository.updateLastDate(batch.getId());
		logger.info("updateLastDate completed");
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	private void load(Batch batch) {
		logger.info("load started");
		valueRepository.load(batch.getId());
		logger.info("load completed");
	}

	@Transactional(propagation=Propagation.REQUIRED, readOnly = true)
	private List<MeteringPointCfg> buildPoints(List<WorkListLine> lines) {
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
					.filter(c -> c.getInterval() == null)
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
