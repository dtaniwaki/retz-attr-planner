package io.github.retz.planner.attr;

import io.github.retz.planner.spi.Offer;
import io.github.retz.planner.spi.Plan;
import io.github.retz.planner.spi.Planner;
import io.github.retz.planner.spi.Resource;
import io.github.retz.protocol.data.Job;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AttrPlannerTest {
    Map<String, Offer> mkOffers() {
        Map<String, Offer> offers = new HashMap<>();
        Resource abc = new Resource(2.0, 128, 555);
        offers.put("abc", new Offer("abc", abc));
        Resource def = new Resource(1.0, 256, 123);
        offers.put("def", new Offer("def", def));
        return offers;
    }

    @Test
    public void firstMatch() {
        Map<String, Offer> offers = mkOffers();

        // list of jobs, first job can be used with first offer
        List<Job> jobs = new LinkedList<>();
        jobs.add(new Job("test-app", "hostname", System.getProperties(),
                1, 64, 0, 0, 0));

        // test planner
        Planner p = new AttrPlanner();
        Plan x = p.plan(offers, jobs);

        // expected: offer abc is assigned to that job
        assertEquals(1, x.getJobSpecs().size());
        for (Map.Entry<String, List<Job>> entry : x.getJobSpecs().entrySet()) {
            assertEquals("abc", entry.getKey());
            assertEquals(1, entry.getValue().size());
        }
    }

    @Test
    public void simpleMatch() {
        Map<String, Offer> offers = mkOffers();

        // list of jobs, first job can only be used with second offer
        List<Job> jobs = new LinkedList<>();
        jobs.add(new Job("test-app", "hostname", System.getProperties(),
                1, 129, 0, 0, 0));

        // test planner
        Planner p = new AttrPlanner();
        Plan x = p.plan(offers, jobs);

        // expected: offer def is assigned to that job
        assertEquals(1, x.getJobSpecs().size());
        for (Map.Entry<String, List<Job>> entry : x.getJobSpecs().entrySet()) {
            assertEquals("def", entry.getKey());
            assertEquals(1, entry.getValue().size());
        }
    }

    @Test
    public void noMatch() {
        Map<String, Offer> offers = mkOffers();

        // list of jobs, no offer can satisfy requirements
        List<Job> jobs = new LinkedList<>();
        jobs.add(new Job("test-app", "hostname", System.getProperties(),
                1, 1024, 0, 0, 0));

        // test planner
        Planner p = new AttrPlanner();
        Plan x = p.plan(offers, jobs);

        // expected: no offer is assigned to that job
        assertEquals(0, x.getJobSpecs().size());
    }
}
