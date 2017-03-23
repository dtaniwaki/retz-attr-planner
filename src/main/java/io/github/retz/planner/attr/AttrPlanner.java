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
        List<Job> jobQueue = new LinkedList<>(jobs);
        for (Map.Entry<String, Offer> offerEntry : offers.entrySet()) {
            Offer offer = offerEntry.getValue();
            String offerKey = offerEntry.getKey();

            ResourceQuantity totalUsedResources = new ResourceQuantity();

            // loop over the not-yet-scheduled jobs and assign them to the
            // current offer, if that is still capable of handling the job
            while (!jobQueue.isEmpty() && offer.resource().cpu() - totalUsedResources.getCpu() > 0) {
                Job nextJob = jobQueue.get(0);
                // compute the resources that "would be" used if we also
                // scheduled the nextJob here
                ResourceQuantity wouldbeUsedResources = totalUsedResources.copy(totalUsedResources);
                wouldbeUsedResources.add(nextJob.resources());

                // check if the offer would still be able to satisfy those
                // "would be needed" resources
                if (offer.resource().toQuantity().fits(wouldbeUsedResources)) {
                    // if so, assign nextJob to this offer
                    plan.setJob(offerKey, nextJob);
                    jobQueue.remove(0);
                    totalUsedResources.add(nextJob.resources());
                } else {
                    // if not, we assume that this offer is "full" and continue
                    // with the next one
                    break;
                }
            }
            if (!plan.getJobSpecs().containsKey(offerKey)
                    && plan.getOfferIdsToStock().size() < maxStock) {
                // No jobs found for this offer, keep for later
                plan.addStock(offerKey);
            }
        }
        if (!jobQueue.isEmpty()) {
            // no offers found for some jobs, keep for later
            plan.addKeep(jobQueue);
        }
        LOG.debug("Plan => {}", plan);
        return plan;
    }
}
