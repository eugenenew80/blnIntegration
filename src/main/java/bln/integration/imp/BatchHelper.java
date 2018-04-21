package bln.integration.imp;

import bln.integration.entity.*;
import bln.integration.entity.enums.BatchStatusEnum;
import bln.integration.entity.enums.ParamTypeEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.repo.AtTimeValueRawRepository;
import bln.integration.repo.BatchRepository;
import bln.integration.repo.PeriodTimeValueRawRepository;
import bln.integration.repo.WorkListHeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class BatchHelper {
    private static final Logger logger = LoggerFactory.getLogger(BatchHelper.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch createBatch(Batch batch) {
        batch = batchService.save(batch);
        updateHeader(batch, batch.getWorkListHeader());
        return batch;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Batch updateBatch(Batch batch, Exception e, Long recCount) {
        batch.setEndDate(LocalDateTime.now());
        if (e!=null) {
            batch.setStatus(BatchStatusEnum.E);
            batch.setErrMsg(e.getMessage());
        }
        else {
            batch.setStatus(BatchStatusEnum.C);
            batch.setRecCount(recCount);
        }

        batch = batchService.save(batch);
        updateHeader(batch, batch.getWorkListHeader());
        return batch;
    }


    @Lock(LockModeType.WRITE)
    private WorkListHeader updateHeader(Batch batch, WorkListHeader header) {
        if (header==null) return null;

        header = workListHeaderService.findOne(header.getId());
        if (ParamTypeEnum.AT==batch.getParamType()) {
            header.setAtBatch(batch);
            header.setAtStatus(batch.getStatus());
        }

        if (ParamTypeEnum.PT==batch.getParamType()) {
            header.setPtBatch(batch);
            header.setPtStatus(batch.getStatus());
        }

        workListHeaderService.save(header);
        return header;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAtData(Batch batch, List<AtTimeValueRaw> list) {
        list.forEach(t -> t.setBatch(batch));
        atService.save(list);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePtData(Batch batch, List<PeriodTimeValueRaw> list) {
        list.forEach(t -> t.setBatch(batch));
        ptService.save(list);
    }


    public MeteringPointCfg buildPointCfg(WorkListLine line, LocalDateTime startTime, LocalDateTime endTime, ParamTypeEnum paramType, Integer interval) {
        ParameterConf parameterConf = line.getParam().getConfs()
            .stream()
            .filter(c -> c.getSourceSystemCode()==SourceSystemEnum.EMCOS)
            .filter(c -> c.getParamType()==paramType)
            .filter(c -> (c.getInterval()==null && interval==null) || c.getInterval().equals(interval))
            .findFirst()
            .orElse(null);

        if (parameterConf==null) return null;

        MeteringPointCfg mpc = new MeteringPointCfg();
        mpc.setSourceParamCode(parameterConf.getSourceParamCode());
        mpc.setSourceUnitCode(parameterConf.getSourceUnitCode());
        mpc.setInterval(parameterConf.getInterval());
        mpc.setSourceMeteringPointCode(line.getMeteringPoint().getExternalCode());
        mpc.setParamCode(line.getParam().getCode());
        mpc.setStartTime(startTime);
        mpc.setEndTime(endTime);

        return mpc;
    }


    public LastLoadInfo getLastLoadIfo(List<LastLoadInfo> lastLoadInfoList, WorkListLine line, ParamTypeEnum paramType, Integer interval) {
        ParameterConf parameterConf = line.getParam().getConfs()
            .stream()
            .filter(c -> c.getSourceSystemCode()==SourceSystemEnum.EMCOS)
            .filter(c -> c.getParamType()==paramType)
            .filter(c ->  (c.getInterval()==null && interval==null) || c.getInterval().equals(interval))
            .findFirst()
            .orElse(null);

        if (parameterConf==null) {
            System.out.println(interval);
            System.out.println(paramType);
            return null;
        }

        LastLoadInfo lastLoadInfo = lastLoadInfoList.stream()
            .filter(t -> t.getSourceMeteringPointCode().equals(line.getMeteringPoint().getExternalCode()) && t.getSourceParamCode().equals(parameterConf.getSourceParamCode()))
            .findFirst()
            .orElse(null);

        return lastLoadInfo;
    }


    @Autowired
    private WorkListHeaderRepository workListHeaderService;

    @Autowired
    private BatchRepository batchService;

    @Autowired
    private AtTimeValueRawRepository atService;

    @Autowired
    private PeriodTimeValueRawRepository ptService;
}
