package io.github.retz.planner.attr;

import io.github.retz.planner.builtin.FIFOPlanner;
import io.github.retz.planner.spi.Attribute;
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
                // sort decreasing by priority
                int prioCmp = p2.priority() - p1.priority();
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
                // check if this offer can satisfy the job constraints
                if (offer.resource().toQuantity().fits(j) &&
                        matches(offer.attributes(), j.attributes().orElse(""))) {
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

    protected static boolean matches(List<Attribute> offerAttrs, String required) {
        String[] components = required.split(";");

        // no requirements => always matches
        if (components.length == 0 || (components.length == 1 && components[0].equals(""))) {
            return true;
        }

        // requirements given and no attributes in offer => always fails
        if (components.length > 0 && offerAttrs.size() == 0) {
            return false;
        }

        // now check each of the given rules
        for (String cmp : components) {
            String[] rule = cmp.split(":");
            if (rule.length != 2) {
                throw new RuntimeException("attribute requirement '" + cmp +
                        "' does not have the required format 'key:value'");
            }
            String key = rule[0];
            String requiredValue = rule[1];

            // find a matching value in offerAttrs
            boolean found = false;
            for (Attribute attr : offerAttrs) {
                if (attr.name().equals(key)) {
                    if (attr.isRanges()) {
                        throw new RuntimeException("range checks (for attribute '" +
                                attr.name() + "' are not implemented yet");
                    } else if (attr.isSet()) {
                        throw new RuntimeException("set checks (for attribute '" +
                                attr.name() + "' are not implemented yet");
                    } else if (attr.isText()) {
                        String actual = attr.asTest();
                        if (actual.equals(requiredValue)) {
                            // requirement matches offered value => this requirement matches
                            found = true;
                            break;
                        } else {
                            // requirement does not match offered value => fail
                            return false;
                        }
                    } else if (attr.isScalar()) {
                        double actual = attr.asScalar();
                        double requiredDoubleValue = Double.parseDouble(requiredValue);
                        if (Math.abs(actual - requiredDoubleValue) < 0.000001) {
                            // requirement matches offered value => this requirement matches
                            found = true;
                            break;
                        } else {
                            // requirement does not match offered value => fail
                            return false;
                        }
                    } else {
                        throw new RuntimeException("unknown attribute type for " +
                                "object: " + attr.toString());
                    }
                }
            }
            // if none of the offered attributes matched the requirement, fail
            if (!found) {
                return false;
            }
        }

        // if we arrive here, then all of the rules in the requirement string
        // had a matching attribute in the offer, that means it matched
        return true;
    }
}
