package bln.integration.imp;

import bln.integration.entity.*;
import bln.integration.entity.enums.BatchStatusEnum;
import bln.integration.entity.enums.ParamTypeEnum;
import bln.integration.entity.enums.WorkListTypeEnum;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.repo.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

@Service
@RequiredArgsConstructor
@Transactional
public class BatchHelper {
    private static final Logger logger = LoggerFactory.getLogger(BatchHelper.class);
    private final WorkListHeaderRepository headerRepository;
    private final BatchRepository batchRepository;
    private final LastLoadInfoRepository lastLoadInfoRepository;
    private final LastRequestedDateRepository lastRequestedDateRepository;
    private final AtTimeValueRawRepository atValueRepository;
    private final PeriodTimeValueRawRepository ptValueRepository;
    private final ParameterConfRepository parameterConfRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch createBatch(Batch batch) {
        batch = batchRepository.save(batch);
        updateHeader(batch);
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch updateBatch(Batch batch, Long recCount) {
        batch.setEndDate(LocalDateTime.now());
        batch.setStatus(BatchStatusEnum.C);
        batch.setRecCount(recCount);
        batch = batchRepository.save(batch);

        updateHeader(batch);
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch errorBatch(Batch batch, Exception e) {
        batch.setEndDate(LocalDateTime.now());
        batch.setStatus(BatchStatusEnum.E);
        batch.setErrMsg(e.getMessage());
        batch = batchRepository.save(batch);

        updateHeader(batch);
        return batch;
    }

    @Lock(LockModeType.WRITE)
    private WorkListHeader updateHeader(Batch batch) {
        WorkListHeader header = batch.getWorkListHeader();
        if (header==null) return null;

        header = headerRepository.findOne(header.getId());
        header.setBatch(batch);
        header.setStatus(batch.getStatus());

        headerRepository.save(header);
        return header;
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void updateLastDate(Batch batch) {
        logger.info("updateLastDate started");

        if (batch.getParamType()==ParamTypeEnum.AT)
            lastLoadInfoRepository.updateAtLastDate(batch.getId());

        if (batch.getParamType()==ParamTypeEnum.PT)
            lastLoadInfoRepository.updatePtLastDate(batch.getId());

        logger.info("updateLastDate completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void load(Batch batch) {
        logger.info("load started");
        if (batch.getParamType()==ParamTypeEnum.AT)
            atValueRepository.load(batch.getId());

        if (batch.getParamType()==ParamTypeEnum.PT)
            ptValueRepository.load(batch.getId());

        logger.info("load completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void updateLastRequestedDate(LastRequestedDate lastRequestedDate) {
        logger.info("updateLastRequestedDate started");
        lastRequestedDateRepository.save(lastRequestedDate);
        logger.info("updateLastRequestedDate completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void save(Batch batch, List<AtTimeValueRaw> atList, List<PeriodTimeValueRaw> ptList) {
        logger.info("saving records started");

        LocalDateTime now = LocalDateTime.now();
        if (batch.getParamType()==ParamTypeEnum.AT) {
            for (AtTimeValueRaw t : atList) {
                t.setBatch(batch);
                t.setCreateDate(now);
            }
            atValueRepository.save(atList);
        }
        if (batch.getParamType()==ParamTypeEnum.PT) {
            for (PeriodTimeValueRaw t : ptList) {
                t.setBatch(batch);
                t.setCreateDate(now);
            }
            ptValueRepository.save(ptList);
        }
        logger.info("saving records completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW, readOnly = true)
    public List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, Function<LastLoadInfo, LocalDateTime> buildStartTime, Supplier<LocalDateTime> buildEndTime) {
        entityManager.clear();

        List<ParameterConf> parameters = parameterConfRepository.findAllBySourceSystemCodeAndParamTypeAndInterval(
            header.getSourceSystemCode(),
            header.getParamType(),
            header.getInterval()
        );

        List<LastLoadInfo> lastLoadInfoList = lastLoadInfoRepository.findAllBySourceSystemCodeAndParamTypeAndInterval(
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
        return  lastRequestedDateRepository.findAllByWorkListHeaderId(header.getId())
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
}
