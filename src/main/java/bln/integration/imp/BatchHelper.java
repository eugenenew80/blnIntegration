package bln.integration.imp;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.repo.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import java.time.*;
import java.util.List;
import java.util.function.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.IntStream.range;

@Service
@RequiredArgsConstructor
@Transactional
public class BatchHelper {
    private static final Logger logger = LoggerFactory.getLogger(BatchHelper.class);
    private final WorkListHeaderRepo headerRepo;
    private final BatchRepo batchRepo;
    private final LastLoadInfoRepo lastLoadInfoRepo;
    private final LastRequestedDateRepo lastRequestedDateRepo;
    private final AtTimeValueRawRepo atValueRepo;
    private final PeriodTimeValueRawRepo ptValueRepo;
    private final ParameterConfRepo parameterConfRepo;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch createBatch(Batch batch) {
        batch = batchRepo.save(batch);
        updateHeader(batch);
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch successBatch(Batch batch, Long recCount) {
        batch.setEndDate(LocalDateTime.now());
        batch.setStatus(BatchStatusEnum.C);
        batch.setRecCount(recCount);
        batch = batchRepo.save(batch);
        updateHeader(batch);
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch errorBatch(Batch batch, Exception e) {
        batch.setEndDate(LocalDateTime.now());
        batch.setStatus(BatchStatusEnum.E);
        batch.setErrMsg(e.getMessage());
        batch = batchRepo.save(batch);
        updateHeader(batch);
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public WorkListHeader updateHeader(Batch batch) {
        WorkListHeader header = batch.getWorkListHeader();
        if (header==null) return null;

        header = headerRepo.findOne(header.getId());
        header.setBatch(batch);
        header.setStatus(batch.getStatus());

        headerRepo.save(header);
        return header;
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void updateLastDate(Batch batch) {
        logger.debug("updateLastDate started");

        if (batch.getParamType()==ParamTypeEnum.AT)
            lastLoadInfoRepo.updateAtLastDate(batch.getId());

        if (batch.getParamType()==ParamTypeEnum.PT)
            lastLoadInfoRepo.updatePtLastDate(batch.getId());

        logger.debug("updateLastDate completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void load(Batch batch) {
        logger.debug("load started");

        if (batch.getParamType()==ParamTypeEnum.AT)
            atValueRepo.load(batch.getId());

        if (batch.getParamType()==ParamTypeEnum.PT)
            ptValueRepo.load(batch.getId());

        logger.debug("load completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void updateLastRequestedDate(LastRequestedDate lastRequestedDate) {
        lastRequestedDateRepo.save(lastRequestedDate);
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void save(Batch batch, List<AtTimeValueRaw> atList, List<PeriodTimeValueRaw> ptList) {
        logger.debug("saving records started");

        LocalDateTime now = LocalDateTime.now();
        if (batch.getParamType()==ParamTypeEnum.AT) {
            for (AtTimeValueRaw t : atList) {
                t.setBatch(batch);
                t.setCreateDate(now);
            }
            atValueRepo.save(atList);
        }
        if (batch.getParamType()==ParamTypeEnum.PT) {
            for (PeriodTimeValueRaw t : ptList) {
                t.setBatch(batch);
                t.setCreateDate(now);
            }
            ptValueRepo.save(ptList);
        }

        logger.debug("saving records completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW, readOnly = true)
    public List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, Function<LastLoadInfo, LocalDateTime> buildStartTime, Supplier<LocalDateTime> buildEndTime) {
        entityManager.clear();

        List<ParameterConf> parameters = parameterConfRepo.findAllBySourceSystemCodeAndParamTypeAndInterval(
            header.getSourceSystemCode(),
            header.getParamType(),
            header.getInterval()
        );

        List<LastLoadInfo> lastLoadInfoList = lastLoadInfoRepo.findAllBySourceSystemCodeAndParamTypeAndInterval(
            header.getSourceSystemCode(),
            header.getParamType(),
            header.getInterval()
        );

        List<MeteringPointCfg> points = header.getLines().stream()
            .flatMap(line ->
                parameters.stream()
                    .filter(c -> c.getMeteringPoint().equals(line.getMeteringPoint()))
                    .map(p -> {
                        LastLoadInfo lastLoadInfo = null;
                        if (header.getWorkListType()==WorkListTypeEnum.SYS) {
                            lastLoadInfo = lastLoadInfoList.stream()
                                .filter(l -> l.getMeteringPoint().equals(p.getMeteringPoint()))
                                .filter(l -> l.getParam().equals(p.getParam()))
                                .findFirst()
                                .orElse(null);
                        }

                        MeteringPointCfg point = MeteringPointCfg.fromParameterConf(p);
                        point.setStartTime(buildStartTime.apply(lastLoadInfo));
                        point.setEndTime(buildEndTime.get());
                        return point;
                    })
                    .filter(point ->
                           (header.getParamType()==ParamTypeEnum.AT && !point.getEndTime().isBefore(point.getStartTime()))
                        || (header.getParamType()==ParamTypeEnum.PT &&  point.getStartTime().isBefore(point.getEndTime()))
                    )
                    .collect(toList())
                    .stream()
            )
            .collect(toList());

        return points;
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW, readOnly = true)
    public LastRequestedDate getLastRequestedDate(WorkListHeader header) {
        return  lastRequestedDateRepo.findAllByWorkListHeaderId(header.getId())
            .stream()
            .findFirst()
            .orElseGet(() -> {
                LocalDateTime now = LocalDate.now(ZoneId.of(header.getTimeZone()))
                    .withDayOfMonth(1)
                    .atStartOfDay();

                LastRequestedDate d = new LastRequestedDate();
                d.setWorkListHeader(header);
                d.setLastRequestedDate(now);
                return d;
            });
    }

    public List<List<MeteringPointCfg>> splitPointsCfg(List<MeteringPointCfg> points, Integer groupCount) {
        return range(0, points.size())
            .boxed()
            .collect(groupingBy(index -> index / groupCount))
            .values()
            .stream()
            .map(indices -> indices
                .stream()
                .map(points::get)
                .collect(toList()))
            .collect(toList());
    }


    public int tzOffset(String timezone) {
        if (timezone.startsWith("UTC+1"))
            return 0;

        if (timezone.startsWith("UTC+2"))
            return -1;

        if (timezone.startsWith("UTC+3"))
            return -2;

        if (timezone.startsWith("UTC+4"))
            return -3;

        if (timezone.startsWith("UTC+5"))
            return -4;

        if (timezone.startsWith("UTC+6"))
            return -5;

        if (timezone.startsWith("UTC+7"))
            return -6;

        return 0;
    }
}
