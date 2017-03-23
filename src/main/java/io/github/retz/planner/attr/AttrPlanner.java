package io.github.retz.planner.attr;

import io.github.retz.planner.builtin.FIFOPlanner;
import io.github.retz.planner.spi.Offer;
import io.github.retz.planner.spi.Plan;
import io.github.retz.protocol.data.Job;
import io.github.retz.protocol.data.ResourceQuantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AttrPlanner extends FIFOPlanner {
    // override private fields in FIFOPlanner
    private static final Logger LOG = LoggerFactory.getLogger(AttrPlanner.class);
    private int maxStock;

    public void setMaxStock(int maxStock) {
        this.maxStock = maxStock;
    }

    @Override
    public Plan plan(Map<String, Offer> offers, List<Job> jobs) {

        Plan plan = new Plan();
        List<Job> queue = new LinkedList<>(jobs);
        for (Map.Entry<String, Offer> entry : offers.entrySet()) {
            ResourceQuantity total = new ResourceQuantity();
            while (!queue.isEmpty() && entry.getValue().resource().cpu() - total.getCpu() > 0) {
                Job job = queue.get(0);
                ResourceQuantity temp = total.copy(total);
                temp.add(job.resources());
                if (entry.getValue().resource().toQuantity().fits(temp)) {
                    plan.setJob(entry.getKey(), job);
                    queue.remove(0);
                    total.add(job.resources());
                } else {
                    break;
                }
            }
            if (!plan.getJobSpecs().containsKey(entry.getKey())
                    && plan.getOfferIdsToStock().size() < maxStock) {
                // No jobs found for this offer
                plan.addStock(entry.getKey());
            }
        }
        if (!queue.isEmpty()) {
            plan.addKeep(queue);
        }
        LOG.debug("Plan => {}", plan);
        return plan;
    }
}
