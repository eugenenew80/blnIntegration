package bln.integration.imp;

import bln.integration.entity.Batch;
import bln.integration.entity.LastRequestedDate;
import bln.integration.entity.WorkListHeader;
import bln.integration.entity.WorkListLine;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.repo.WorkListHeaderRepository;
import org.slf4j.Logger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public abstract class AbstractReader<T> implements Reader<T> {

    @Override
    public void read(Long headerId) {
        logger().info("read started");
        logger().info("headerId: " + headerId);

        WorkListHeader header = headerRepository().findOne(headerId);
        if (header.getConfig() == null) {
            logger().warn("Config is empty, request stopped");
            return;
        }

        if (header == null) {
            logger().info("Work list not found");
            return;
        }

        List<WorkListLine> lines = header.getLines();
        if (lines.size() == 0) {
            logger().info("List of lines is empty, import data stopped");
            return;
        }

        LocalDateTime endDateTime = LocalDate.now(ZoneId.of(header.getTimeZone())).atStartOfDay();
        List<MeteringPointCfg> points = buildPointsCfg(header, endDateTime);
        if (points.size() == 0) {
            logger().info("List of points is empty, import data stopped");
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
            logger().debug(points.toString());
            List<List<MeteringPointCfg>> groupsPoints = batchHelper().splitPointsCfg(points, groupCount());

            Batch batch = batchHelper().createBatch(new Batch(header));
            try {
                Long recCount = request(groupsPoints, batch);
                batchHelper().updateBatch(batch, recCount);
                batchHelper().updateLastDate(batch);
                batchHelper().load(batch);
                if (recCount>0) {
                    lastRequestedDate.setLastRequestedDate(requestedDateTime);
                    batchHelper().updateLastRequestedDate(lastRequestedDate);
                }
            }
            catch (Exception e) {
                logger().error("read failed: " + e.getMessage());
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

    protected abstract List<MeteringPointCfg> buildPointsCfg(WorkListHeader header, LocalDateTime endDateTime);

    protected abstract Long request(List<List<MeteringPointCfg>> groupsPoints, Batch batch) throws Exception;

    protected abstract Logger logger();

    protected  abstract BatchHelper batchHelper();

    protected  abstract WorkListHeaderRepository headerRepository();

    protected  abstract Integer groupCount();
}
