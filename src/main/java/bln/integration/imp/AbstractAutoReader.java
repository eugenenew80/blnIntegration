package bln.integration.imp;

import bln.integration.entity.*;
import bln.integration.entity.enums.ParamTypeEnum;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.repo.WorkListHeaderRepo;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

public abstract class AbstractAutoReader<T> implements Reader<T> {
    @Override
    public void read(Long headerId) {
        logger().info("read started");
        logger().debug("headerId: " + headerId);

        WorkListHeader header = headerRepo().findOne(headerId);
        if (header == null) {
            logger().warn("Work list header not found");
            return;
        }
        if (header.getConfig() == null) {
            logger().warn("Config is empty, request stopped");
            return;
        }

        List<WorkListLine> lines = header.getLines();
        if (lines.size() == 0) {
            logger().warn("List of lines is empty, import data stopped");
            return;
        }

        LocalDateTime endDateTime = LocalDateTime.now(ZoneId.of("UTC+1"));
        if (header.getParamType()==ParamTypeEnum.PT)
            endDateTime = endDateTime.truncatedTo(ChronoUnit.HOURS);

        if (header.getParamType()== ParamTypeEnum.AT)
            endDateTime = endDateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1);

        List<MeteringPointCfg> points = buildPointsCfg(header, endDateTime);
        if (points.size() == 0) {
            logger().warn("List of points is empty, import data stopped");
            return;
        }

        LastRequestedDate lastRequestedDate = batchHelper().getLastRequestedDate(header);
        LocalDateTime startDateTime = lastRequestedDate.getLastRequestedDate().plusDays(1);
        if (startDateTime.isAfter(endDateTime))
            startDateTime=endDateTime;

        logger().info("url: " + header.getConfig().getUrl());
        logger().info("user: " + header.getConfig().getUserName());
        logger().info("startDateTime: " + startDateTime);
        logger().info("endDateTime: " + endDateTime);

        LocalDateTime requestedDateTime = startDateTime;
        while (!endDateTime.isBefore(requestedDateTime)) {
            logger().info("batch requested date: " + requestedDateTime);

            points = buildPointsCfg(header, requestedDateTime);
            logger().trace(points.toString());
            List<List<MeteringPointCfg>> groupsPoints = batchHelper().splitPointsCfg(points, groupCount());

            Batch batch = batchHelper().createBatch(new Batch(header));
            try {
                Long recCount = 0l;
                for (int i = 0; i < groupsPoints.size(); i++) {
                    logger().debug("group of points: " + (i + 1));
                    logger().trace(groupsPoints.get(i).toString());

                    List<T> list = request(batch, groupsPoints.get(i));
                    save(batch, list);
                    recCount = recCount + list.size();
                }

                batchHelper().successBatch(batch, recCount);
                batchHelper().updateLastDate(batch);
                batchHelper().load(batch);
                if (recCount>0) {
                    lastRequestedDate.setLastRequestedDate(requestedDateTime.truncatedTo(ChronoUnit.DAYS));
                    batchHelper().updateLastRequestedDate(lastRequestedDate);
                }
            }
            catch (Exception e) {
                logger().error("read failed: " + e);
                batchHelper().errorBatch(batch, e);
                break;
            }
            finally {
                System.gc();
            }

            if (requestedDateTime.isEqual(endDateTime))
                break;

            requestedDateTime = requestedDateTime.plusDays(1);
            if (requestedDateTime.isAfter(endDateTime))
                requestedDateTime=endDateTime;
        }

        logger().info("read completed");
    }

    protected abstract List<T> request(Batch batch, List<MeteringPointCfg> points) throws Exception;

    protected abstract void save(Batch batch, List<T> list);

    protected abstract List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, LocalDateTime endDateTime);

    protected abstract Logger logger();

    protected  abstract BatchHelper batchHelper();

    protected  abstract WorkListHeaderRepo headerRepo();

    protected  abstract Integer groupCount();
}
