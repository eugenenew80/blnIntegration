package bln.integration.imp.oic.reader;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.gateway.oic.LogPointCfg;
import bln.integration.gateway.oic.OicDataImpGateway;
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
import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class AutoOicDataReader implements Reader<TelemetryRaw> {
    private static final Logger logger = LoggerFactory.getLogger(AutoOicDataReader.class);
	private final LastLoadInfoRepository lastLoadInfoRepository;
	private final ParameterConfRepository parameterConfRepository;
	private final WorkListHeaderRepository headerRepository;
    private final OicDataImpGateway oicImpGateway;
    private final BatchHelper batchHelper;
	private final EntityManager entityManager;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void read(Long headerId) {
		logger.info("read started");
		logger.info("headerId: " + headerId);

		WorkListHeader header = headerRepository.findOne(headerId);
		if (header==null) {
			logger.info("Work list not found");
			return;
		}

		List<WorkListLine> lines = header.getLines();
		if (lines.size()==0) {
			logger.info("List of lines is empty, import data stopped");
			return;
		}

		LocalDateTime endDateTime = buildEndDateTime();
		List<LogPointCfg> points = buildPointsCfg(lines, endDateTime);
		if (points.size()==0) {
			logger.info("List of points is empty, import data stopped");
			return;
		}

		logger.info("url: " + header.getConfig().getUrl());
		logger.info("user: " + header.getConfig().getUserName());

		Batch batch = batchHelper.createBatch(new Batch(header, ParamTypeEnum.PT));
		try {
			List<PeriodTimeValueRaw> list = oicImpGateway.request(header.getConfig(), points, "MIN-60");
			batchHelper.ptSave(list, batch);
			batchHelper.updateBatch(batch, (long)list.size());
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

	@Transactional(propagation=Propagation.REQUIRED, readOnly = true)
	List<LogPointCfg> buildPointsCfg(List<WorkListLine> lines, LocalDateTime endDateTime) {
		List<ParameterConf> parameters = parameterConfRepository.findAllBySourceSystemCodeAndParamType(
			SourceSystemEnum.OIC,
			ParamTypeEnum.PT
		);

		entityManager.clear();
		List<LastLoadInfo> lastLoadInfos = lastLoadInfoRepository
			.findAllBySourceSystemCode(SourceSystemEnum.OIC);

		List<LogPointCfg> points = lines.stream()
			.flatMap(line ->
				parameters.stream()
					.filter(p -> p.getMeteringPoint().equals(line.getMeteringPoint()))
					.filter(p -> p.getInterval().equals(3600))
					.map(p -> {
						LastLoadInfo lastLoadInfo = lastLoadInfos.stream()
							.filter(l -> l.getMeteringPoint().equals(p.getMeteringPoint()))
							.filter(l -> l.getSourceMeteringPointCode().equals(p.getSourceMeteringPointCode()))
							.filter(l -> l.getSourceParamCode().equals(p.getSourceParamCode()))
							.findFirst()
							.orElse(null);

						LogPointCfg lpc = new LogPointCfg();
						lpc.setMeteringPointId(p.getMeteringPoint().getId());
						lpc.setParamId(p.getParam().getId());
						lpc.setLogPointId(Long.parseLong(p.getSourceMeteringPointCode()));
						lpc.setParamCode(p.getSourceParamCode());
						lpc.setUnitCode(p.getSourceUnitCode());
						lpc.setStart(buildStartDateTime(lastLoadInfo));
						lpc.setEnd(endDateTime);

						return !lpc.getEnd().isBefore(lpc.getStart()) ? lpc : null;
					})
					.filter(lpc -> lpc!=null)
					.collect(toList())
					.stream()
			)
			.collect(toList());

		return points;
	}

	private LocalDateTime buildStartDateTime(LastLoadInfo lastLoadInfo) {
		if (lastLoadInfo!=null && lastLoadInfo.getLastLoadDate() !=null) {
			LocalDateTime lastLoadDate = lastLoadInfo.getLastLoadDate();
			return lastLoadDate.truncatedTo(ChronoUnit.HOURS).plusHours(1);
		}

		LocalDate now = LocalDate.now();
		return now.minusDays(now.getDayOfMonth()-1).atStartOfDay();
	}

	private LocalDateTime buildEndDateTime() {
		return LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
	}
}
