package bln.integration.imp;

import bln.integration.entity.*;
import bln.integration.entity.enums.BatchStatusEnum;
import bln.integration.entity.enums.ParamTypeEnum;
import bln.integration.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.LockModeType;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BatchHelper {
    private final WorkListHeaderRepository headerRepository;
    private final BatchRepository batchRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch createBatch(Batch batch) {
        batch = batchRepository.save(batch);
        updateHeader(batch, batch.getWorkListHeader());
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch updateBatch(Batch batch, Long recCount) {
        batch.setEndDate(LocalDateTime.now());
        batch.setStatus(BatchStatusEnum.C);
        batch.setRecCount(recCount);
        batch = batchRepository.save(batch);

        updateHeader(batch, batch.getWorkListHeader());
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch errorBatch(Batch batch, Exception e) {
        batch.setEndDate(LocalDateTime.now());
        batch.setStatus(BatchStatusEnum.E);
        batch.setErrMsg(e.getMessage());
        batch = batchRepository.save(batch);

        updateHeader(batch, batch.getWorkListHeader());
        return batch;
    }

    @Lock(LockModeType.WRITE)
    private WorkListHeader updateHeader(Batch batch, WorkListHeader header) {
        if (header==null) return null;

        header = headerRepository.findOne(header.getId());
        header.setBatch(batch);
        header.setStatus(batch.getStatus());

        headerRepository.save(header);
        return header;
    }
}
