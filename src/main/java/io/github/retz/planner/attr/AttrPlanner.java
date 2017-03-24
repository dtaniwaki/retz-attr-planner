package io.github.retz.planner.attr;

import io.github.retz.planner.builtin.FIFOPlanner;
import io.github.retz.planner.spi.Offer;
import io.github.retz.planner.spi.Plan;
import io.github.retz.protocol.data.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AttrPlanner extends FIFOPlanner {
    // override private fields in FIFOPlanner
    private static final Logger LOG = LoggerFactory.getLogger(AttrPlanner.class);
    private int maxStock;

    public void setMaxStock(int maxStock) {
        this.maxStock = maxStock;
    }

    @Override
    public Plan plan(Map<String, Offer> offers, List<Job> jobs) {
        /* Plan Strategy
         * -------------
         *
         * This plan will first sort the given jobs
         *  1. by priority
         *  2. by whether they have attributes or not
         *  3. by required number of GPUs (descending).
         * This ensures that we can first prioritize a whole number of tasks that
         * are more important (e.g., paying customers, low-latency tasks, ...),
         * the others are heuristics to place jobs first that are "harder" to place
         * due to their requirements.
         *
         * Then we loop over the sorted job list and try to find
         * an offer that can be used for that job.
         */

        Plan plan = new Plan();

        List<Job> jobQueue = new LinkedList<>(jobs);
        Collections.sort(jobQueue, new Comparator<Job>() {
            @Override
            public int compare(Job p1, Job p2) {
                // sort increasing by priority
                int prioCmp = p1.priority() - p2.priority();
                if (prioCmp != 0) {
                    return prioCmp;
                }
                // sort jobs with attributes first
                int attrCmp = 0;
                if (p1.attributes().isPresent() && !p2.attributes().isPresent()) {
                    attrCmp = -1;
                } else if (!p1.attributes().isPresent() && p2.attributes().isPresent()) {
                    attrCmp = 1;
                }
                if (attrCmp != 0) {
                    return attrCmp;
                }
                // sort decreasing by GPU
                return p2.resources().getGpu() - p1.resources().getGpu();
            }
        });

        List<Job> unmatchedJobs = new LinkedList<>();
        List<Offer> availableOffers = new LinkedList<>(offers.values());
        for (Job j : jobQueue) {
            // try to find an offer that matches
            boolean found = false;
            for (Offer offer : availableOffers) {
                // TODO if the job has an attribute constraint, check if the offer satisfies that constraint

                // check if this offer can satisfy the job constraints
                if (offer.resource().toQuantity().fits(j)) {
                    plan.setJob(offer.id(), j);
                    availableOffers.remove(offer);
                    found = true;
                    break;
                }
            }
            if (!found) {
                unmatchedJobs.add(j);
            }
        }

        // remember unmatched jobs
        if (!unmatchedJobs.isEmpty()) {
            plan.addKeep(unmatchedJobs);
        }

        // remember unmatched offers
        for (int i = 0; i < maxStock && i < availableOffers.size(); i++) {
            plan.addStock(availableOffers.get(i).id());
        }

        LOG.debug("Plan => {}", plan);
        return plan;
    }
}
