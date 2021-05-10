package dev.rea.rmil.client.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DebugDistributionQueue<T> extends DistributionQueue<T> {

    private static final Logger logger = LoggerFactory.getLogger(DebugDistributionQueue.class);

    @Override
    protected void onGet(UUID itemID) {
        logger.debug(String.format("Pulled an item [%s] from queue belonging to function [%s],",
                itemID, getFunctionID()));
    }

    @Override
    protected void onPut(UUID itemID) {
        logger.debug(String.format("Added an item [%s] to queue belonging to function [%s]",
                itemID, getFunctionID()));
    }

    @Override
    protected void onRemove(UUID itemID) {
        logger.debug(String.format("Removed an item [%s] from queue belonging to function [%s]",
                itemID, getFunctionID()));
    }
}
