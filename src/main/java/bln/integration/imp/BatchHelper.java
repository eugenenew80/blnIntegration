package bln.integration.imp;

import bln.integration.entity.*;
import bln.integration.entity.enums.BatchStatusEnum;
import bln.integration.entity.enums.ParamTypeEnum;
import bln.integration.entity.enums.WorkListTypeEnum;
import bln.integration.gateway.emcos.MeteringPointCfg;
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
    public void updateAtLastDate(Batch batch) {
        logger.info("updateAtLastDate started");
        lastLoadInfoRepository.updateAtLastDate(batch.getId());
        logger.info("updateAtLastDate completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void updatePtLastDate(Batch batch) {
        logger.info("updateAtLastDate started");
        lastLoadInfoRepository.updatePtLastDate(batch.getId());
        logger.info("updateAtLastDate completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void updateLastRequestedDate(LastRequestedDate lastRequestedDate) {
        logger.info("updateLastRequestedDate started");
        lastRequestedDateRepository.save(lastRequestedDate);
        logger.info("updateLastRequestedDate completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void atSave(List<AtTimeValueRaw> atList, Batch batch) {
        logger.info("saving records started");
        LocalDateTime now = LocalDateTime.now();
        for (AtTimeValueRaw t : atList) {
            t.setBatch(batch);
            t.setCreateDate(now);
        }
        atValueRepository.save(atList);
        logger.info("saving records completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void ptSave(List<PeriodTimeValueRaw> ptList, Batch batch) {
        logger.info("saving records started");
        LocalDateTime now = LocalDateTime.now();
        for (PeriodTimeValueRaw t : ptList) {
            t.setBatch(batch);
            t.setCreateDate(now);
        }
        ptValueRepository.save(ptList);
        logger.info("saving records completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void atLoad(Batch batch) {
        logger.info("ptLoad started");
        atValueRepository.load(batch.getId());
        logger.info("ptLoad completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void ptLoad(Batch batch) {
        logger.info("ptLoad started");
        ptValueRepository.load(batch.getId());
        logger.info("ptLoad completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW, readOnly = true)
    public List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, Function<LastLoadInfo, LocalDateTime> buildStartTime, Supplier<LocalDateTime> buildEndTime) {
        entityManager.clear();

        List<ParameterConf> parameters = parameterConfRepository.findAllBySourceSystemCodeAndParamTypeAAndInterval(
            header.getSourceSystemCode(),
            header.getParamType(),
            header.getInterval()
        );
        List<LastLoadInfo> lastLoadInfoList = lastLoadInfoRepository.findAllBySourceSystemCodeAAndParamTypeAAndInterval(
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

                        MeteringPointCfg mpc = MeteringPointCfg.fromLine(p);
                        mpc.setStartTime(buildStartTime.apply(lastLoadInfo));
                        mpc.setEndTime(buildEndTime.get());
                        return mpc;
                    })
                    .filter(mpc ->
                           (header.getParamType()==ParamTypeEnum.AT && !mpc.getEndTime().isBefore(mpc.getStartTime()))
                        || (header.getParamType()==ParamTypeEnum.PT &&  mpc.getStartTime().isBefore(mpc.getEndTime()))
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
