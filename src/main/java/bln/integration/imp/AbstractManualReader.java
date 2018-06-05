package bln.integration.imp;

import bln.integration.entity.Batch;
import bln.integration.entity.WorkListHeader;
import bln.integration.entity.WorkListLine;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.repo.WorkListHeaderRepo;
import org.slf4j.Logger;
import java.util.List;

public abstract class AbstractManualReader<T> implements Reader<T> {
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

        if (header.getStartDate() == null) {
            logger().warn("Start Date is empty, request stopped");
            return;
        }
        if (header.getEndDate() == null) {
            logger().warn("End Date is empty, request stopped");
            return;
        }

        List<WorkListLine> lines = header.getLines();
        if (lines.size()==0) {
            logger().warn("List of points is empty, import data stopped");
            return;
        }

        List<MeteringPointCfg> points = buildPointsCfg(header);
        if (points.size()==0) {
            logger().warn("List of points is empty, import data stopped");
            return;
        }

        logger().info("url: " + header.getConfig().getUrl());
        logger().info("user: " + header.getConfig().getUserName());
        logger().info("startDateTime: " + header.getStartDate());
        logger().info("endDateTime: " + header.getEndDate());

        Batch batch = batchHelper().createBatch(new Batch(header));
        try {
            logger().trace(points.toString());

            List<T> list = request(batch, points);
            save(batch, list);
            batchHelper().successBatch(batch, (long) list.size());
            batchHelper().updateLastDate(batch);
            batchHelper().load(batch);
        }
        catch (Exception e) {
            logger().error("read failed: " + e);
            batchHelper().errorBatch(batch, e);
        }
        finally {
            System.gc();
        }

        logger().info("read completed");
    }

    protected abstract List<T> request(Batch batch, List<MeteringPointCfg> points) throws Exception;

    protected abstract void save(Batch batch, List<T> list);

    protected abstract List<MeteringPointCfg> buildPointsCfg(WorkListHeader header);

    protected abstract Logger logger();

    protected  abstract BatchHelper batchHelper();

    protected  abstract WorkListHeaderRepo headerRepo();
}
