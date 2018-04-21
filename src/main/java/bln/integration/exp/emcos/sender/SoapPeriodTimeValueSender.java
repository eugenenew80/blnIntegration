package bln.integration.exp.emcos.sender;

import bln.integration.entity.Batch;
import bln.integration.entity.WorkListLine;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.enums.*;
import bln.integration.exp.Sender;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.emcos.PeriodTimeValueExpGateway;
import bln.integration.imp.BatchHelper;
import bln.integration.repo.WorkListHeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SoapPeriodTimeValueSender implements Sender<PeriodTimeValueRaw> {
	private static final Logger logger = LoggerFactory.getLogger(SoapPeriodTimeValueSender.class);

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void send() {
		AtomicBoolean flag = new AtomicBoolean(false);

		workListHeaderService.findAllBySourceSystemCodeAndDirectionAndWorkListType(
			SourceSystemEnum.EMCOS,
			DirectionEnum.EXPORT,
			WorkListTypeEnum.USER
		).stream()
			.filter(h -> h.getActive() && h.getPtStatus()==BatchStatusEnum.W && h.getConfig()!=null)
			.forEach(header -> {
				if (!flag.get())
					logger.info("read started");

				flag.set(true);

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
				try {
					ptGateway
						.config(header.getConfig())
						.points(points)
						.send();

					batchHelper.updateBatch(batch, null, (long)points.size());
				}
				catch (Exception e) {
					logger.error("SoapPeriodTimeValueSender.send failed: " + e.getMessage());
					batchHelper.updateBatch(batch, e, null);
				}
			});

		if (flag.get())
			logger.info("send completed");
	}


	private List<MeteringPointCfg> buildPoints(List<WorkListLine> lines) {
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH");

		List<MeteringPointCfg> points = new ArrayList<>();
		MeteringPointCfg mpc = new MeteringPointCfg();
		mpc.setSourceParamCode("709");
		mpc.setParamCode("A+");
		mpc.setInterval(3600);
		mpc.setVal(123d);
		mpc.setSourceMeteringPointCode("113440990999999999");

		LocalDateTime startTime = LocalDateTime.parse("12.03.2018 00", timeFormatter);
		LocalDateTime endTime   = LocalDateTime.parse("12.03.2018 01", timeFormatter);

		mpc.setStartTime(startTime);
		mpc.setEndTime(endTime);
		points.add(mpc);

		return points;
	}


	@Autowired
	private PeriodTimeValueExpGateway ptGateway;

	@Autowired
	private WorkListHeaderRepository workListHeaderService;

	@Autowired
	private BatchHelper batchHelper;
}