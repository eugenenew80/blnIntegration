package bln.integration.imp;

import bln.integration.entity.Batch;
import bln.integration.entity.WorkListHeader;
import bln.integration.entity.WorkListLine;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.repo.WorkListHeaderRepository;
import org.slf4j.Logger;
import java.util.List;

public abstract class AbstractManualReader<T> implements Reader<T> {
    @Override
    public void read(Long headerId) {
        WorkListHeader header = headerRepository().findOne(headerId);
        if (header.getConfig() == null) {
            logger().warn("Config is empty, request stopped");
            return;
        }

        List<WorkListLine> lines = header.getLines();
        if (lines.size()==0) {
            logger().debug("List of points is empty, import data stopped");
            return;
        }

        List<MeteringPointCfg> points = buildPointsCfg(header);
        if (points.size()==0) {
            logger().debug("Import data is not required, import data stopped");
            return;
        }

        logger().info("read started");
        logger().info("headerId: " + header.getId());
        logger().info("url: " + header.getConfig().getUrl());
        logger().info("user: " + header.getConfig().getUserName());

        Batch batch = batchHelper().createBatch(new Batch(header));
        try {
            List<T> list = request(batch, points);
            save(batch, list);
            batchHelper().updateBatch(batch, (long) list.size());
            batchHelper().updateLastDate(batch);
            batchHelper().load(batch);
        }
        catch (Exception e) {
            logger().error("read failed: " + e.getMessage());
            batchHelper().errorBatch(batch, e);
        }
        finally {
            System.gc();
        }

        logger().info("read completed");
    }

    protected abstract List<MeteringPointCfg> buildPointsCfg(WorkListHeader header);

    protected abstract List<T> request(Batch batch, List<MeteringPointCfg> points) throws Exception;

    protected abstract void save(Batch batch, List<T> list);

    protected abstract Logger logger();

    protected  abstract BatchHelper batchHelper();

    protected  abstract WorkListHeaderRepository headerRepository();
}
