package bln.integration.exp.emcos.sender;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.exp.Sender;
import bln.integration.exp.gateway.FtpGateway;
import bln.integration.repo.BatchRepository;
import bln.integration.repo.ExportDataRepository;
import bln.integration.repo.WorkListHeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FtpPeriodTimeValueSender implements Sender<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(FtpPeriodTimeValueSender.class);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'_'HHmmss");

    public void send() {
        AtomicBoolean flag = new AtomicBoolean(false);

        headerService.findAllBySourceSystemCodeAndDirectionAndWorkListType(
            SourceSystemEnum.EMCOS,
            DirectionEnum.EXPORT,
            WorkListTypeEnum.USER
        ).stream()
            .filter(h -> h.getActive() && h.getStatus()==BatchStatusEnum.W && h.getConfig()!=null)
            .forEach(header -> {
                if (!flag.get())
                    logger.warn("send started");

                flag.set(true);

                List<WorkListLine> lines = header.getLines();
                if (lines.size()==0) {
                    logger.warn("List of points is empty, import media stopped");
                    return;
                }

                if (header.getStartDate()==null || header.getEndDate()==null) {
                    logger.warn("Period must be specified");
                    return;
                }

                Map<String, List<ExportData>> exportData = new HashMap<>();
                String path = "/home/bis-user/export";
                String fileName = "BIS_" + header.getId() + "_" +  LocalDateTime.now().format(timeFormatter);
                Batch batch = startBatch(header);

                try {
                    lines.stream()
                        .forEach(line -> {
                            logger.info("searching data for export: " + line.getMeteringPoint().getExternalCode());
                            List<ExportData> exportDataList = exportDataRepository.findAllBySourceMeteringPointCodeAndMeteringDateBetween(
                                line.getMeteringPoint().getExternalCode(),
                                header.getStartDate(),
                                header.getEndDate()
                            );
                            exportData.put(line.getMeteringPoint().getExternalCode(), exportDataList);
                        });

                    Long recCount = 0l;
                    for (String key : exportData.keySet())
                        recCount += exportData.get(key).size();

                    if (recCount==0) {
                        logger.info("No media found, export media stopped");
                        return;
                    }

                    ftpGateway
                        .config(header.getConfig())
                        .exportData(exportData)
                        .path(path)
                        .fileName(fileName)
                        .send();

                    endBatch(header, batch, recCount);
                }

                catch (Exception e) {
                    logger.error("send failed: " + e.getMessage());
                    errorBatch(header, batch, e);
                }
            });

        if (flag.get())
            logger.info("send completed");
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch startBatch(WorkListHeader header) {
        Batch batch = new Batch();
        batch.setWorkListHeader(header);
        batch.setSourceSystemCode(header.getSourceSystemCode());
        batch.setDirection(header.getDirection());
        batch.setParamType(ParamTypeEnum.PT);
        batch.setStatus(BatchStatusEnum.P);
        batch.setStartDate(LocalDateTime.now());
        batch = batchService.save(batch);

        header = headerService.findOne(header.getId());
        header.setBatch(batch);
        header.setStatus(BatchStatusEnum.P);
        headerService.save(header);
        return batch;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch endBatch(WorkListHeader header, Batch batch, Long retCount) {
        batch.setStatus(BatchStatusEnum.C);
        batch.setEndDate(LocalDateTime.now());
        batch.setRecCount(retCount);
        batchService.save(batch);

        header = headerService.findOne(header.getId());
        header.setStatus(BatchStatusEnum.C);
        headerService.save(header);
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch errorBatch(WorkListHeader header, Batch batch, Exception e) {
        batch.setStatus(BatchStatusEnum.C);
        batch.setEndDate(LocalDateTime.now());
        batch.setErrMsg(e.getMessage());
        batchService.save(batch);

        header = headerService.findOne(header.getId());
        header.setStatus(BatchStatusEnum.E);
        headerService.save(header);
        return batch;
    }


    @Autowired
    private FtpGateway ftpGateway;

    @Autowired
    private WorkListHeaderRepository headerService;

    @Autowired
    private BatchRepository batchService;

    @Autowired
    private ExportDataRepository exportDataRepository;
}
