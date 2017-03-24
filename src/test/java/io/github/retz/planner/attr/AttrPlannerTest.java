package io.github.retz.planner.attr;

import io.github.retz.planner.spi.*;
import io.github.retz.protocol.data.Job;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AttrPlannerTest {
    private Map<String, Offer> mkOffers() {
        Map<String, Offer> offers = new HashMap<>();
        Resource abc = new Resource(2.0, 128, 555);
        offers.put("abc", new Offer("abc", abc));
        Resource def = new Resource(1.0, 256, 123);
        offers.put("def", new Offer("def", def));
        return offers;
    }

    private Map<String, Offer> mkAttrOffers() {
        Map<String, Offer> offers = new HashMap<>();
        Resource abc = new Resource(2.0, 128, 555);
        offers.put("abc", new Offer("abc", abc));
        Resource def = new Resource(1.0, 256, 123);
        offers.put("def", new Offer("def", def));
        Resource ghi = new Resource(1.0, 256, 123);
        List<Attribute> attrs = new LinkedList<>();
        attrs.add(new Attribute("rack", Attribute.Type.TEXT, "A"));
        offers.put("ghi", new Offer("ghi", ghi, attrs));
        return offers;
    }

    private Planner getPlanner() {
        Planner p = new AttrPlanner();
        p.setMaxStock(2);
        return p;
    }

    @Test
    public void firstMatch() {
        Map<String, Offer> offers = mkOffers();

        // list of jobs, first job can be used with first offer
        List<Job> jobs = new LinkedList<>();
        jobs.add(new Job("test-app", "hostname", System.getProperties(),
                1, 64, 0, 0, 0));

        // test planner
        Plan x = getPlanner().plan(offers, jobs);

        // expected: offer abc is assigned to that job
        assertEquals(1, x.getJobSpecs().size());
        for (Map.Entry<String, List<Job>> entry : x.getJobSpecs().entrySet()) {
            assertEquals("abc", entry.getKey());
            assertEquals(1, entry.getValue().size());
        }
        // expected: offer def is added to stock
        assertEquals(1, x.getOfferIdsToStock().size());
        assertEquals("def", x.getOfferIdsToStock().get(0));
        // expected: no jobs are kept for later
        assertEquals(0, x.getToKeep().size());
    }

    @Test
    public void simpleMatch() {
        Map<String, Offer> offers = mkOffers();

        // list of jobs, first job can only be used with second offer
        List<Job> jobs = new LinkedList<>();
        jobs.add(new Job("test-app", "hostname", System.getProperties(),
                1, 129, 0, 0, 0));

        // test planner
        Plan x = getPlanner().plan(offers, jobs);

        // expected: offer def is assigned to that job
        assertEquals(1, x.getJobSpecs().size());
        for (Map.Entry<String, List<Job>> entry : x.getJobSpecs().entrySet()) {
            assertEquals("def", entry.getKey());
            assertEquals(1, entry.getValue().size());
        }
        // expected: offer abc is added to stock
        assertEquals(1, x.getOfferIdsToStock().size());
        assertEquals("abc", x.getOfferIdsToStock().get(0));
        // expected: no jobs are kept for later
        assertEquals(0, x.getToKeep().size());
    }

    @Test
    public void noMatch() {
        Map<String, Offer> offers = mkOffers();

        // list of jobs, no offer can satisfy requirements
        List<Job> jobs = new LinkedList<>();
        jobs.add(new Job("test-app", "hostname", System.getProperties(),
                1, 1024, 0, 0, 0));

        // test planner
        Plan x = getPlanner().plan(offers, jobs);

        // expected: no offer is assigned to that job
        assertEquals(0, x.getJobSpecs().size());
        // expected: both offers are added to stock
        assertEquals(2, x.getOfferIdsToStock().size());
        // expected: job is kept for later
        assertEquals(1, x.getToKeep().size());
        assertEquals(x.getToKeep().get(0), jobs.get(0));
    }
}
