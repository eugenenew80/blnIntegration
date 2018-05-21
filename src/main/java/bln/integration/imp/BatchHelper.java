package bln.integration.imp;

import bln.integration.entity.*;
import bln.integration.entity.enums.BatchStatusEnum;
import bln.integration.repo.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

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

    @SuppressWarnings("Duplicates")
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void atSave(List<AtTimeValueRaw> list, Batch batch) {
        logger.info("saving records started");
        LocalDateTime now = LocalDateTime.now();
        list.forEach(t -> {
            t.setBatch(batch);
            t.setCreateDate(now);
        });
        atValueRepository.save(list);
        logger.info("saving records completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void atLoad(Batch batch) {
        logger.info("ptLoad started");
        atValueRepository.load(batch.getId());
        logger.info("ptLoad completed");
    }

    @SuppressWarnings("Duplicates")
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void ptSave(List<PeriodTimeValueRaw> list, Batch batch) {
        logger.info("saving records started");
        LocalDateTime now = LocalDateTime.now();
        list.forEach(t -> {
            t.setBatch(batch);
            t.setCreateDate(now);
        });
        ptValueRepository.save(list);
        logger.info("saving records completed");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void ptLoad(Batch batch) {
        logger.info("ptLoad started");
        ptValueRepository.load(batch.getId());
        logger.info("ptLoad completed");
    }
}
