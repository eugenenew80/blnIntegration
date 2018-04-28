package bln.integration.imp.emcos.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.emcos.PeriodTimeValueImpGateway;
import bln.integration.imp.BatchHelper;
import bln.integration.imp.Reader;
import bln.integration.repo.LastLoadInfoRepository;
import bln.integration.repo.ParameterConfRepository;
import bln.integration.repo.PeriodTimeValueRawRepository;
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
public class ManualPeriodTimeValueReader implements Reader<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(ManualPeriodTimeValueReader.class);
    private final PeriodTimeValueRawRepository valueRepository;
    private final ParameterConfRepository parameterConfRepository;
	private final WorkListHeaderRepository headerRepository;
	private final LastLoadInfoRepository lastLoadInfoRepository;
    private final PeriodTimeValueImpGateway valueGateway;
    private final BatchHelper batchHelper;

	@Transactional(propagation=Propagation.NOT_SUPPORTED)
	public void read(Long headerId) {
		WorkListHeader header = headerRepository.findOne(headerId);

		logger.debug("read started");
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
			List<PeriodTimeValueRaw> list = valueGateway
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

		logger.debug("read completed");
	}


	@SuppressWarnings("Duplicates")
	@Transactional(propagation= Propagation.REQUIRES_NEW)
	void save(List<PeriodTimeValueRaw> list, Batch batch) {
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
	void updateLastDate(Batch batch) {
		logger.info("updateLastDate started");
		lastLoadInfoRepository.updatePtLastDate(batch.getId());
		logger.info("updateLastDate completed");
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	void load(Batch batch) {
		logger.info("load started");
		valueRepository.load(batch.getId());
		logger.info("load completed");
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	List<MeteringPointCfg> buildPoints(List<WorkListLine> lines) {
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

				if (mpc!=null && !mpc.getEndTime().isBefore(mpc.getStartTime()))
					points.add(mpc);
			});

		return points;
	}
}
