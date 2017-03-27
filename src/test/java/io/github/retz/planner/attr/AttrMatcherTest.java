package io.github.retz.planner.attr;

/*
 * (C) Copyright 2017 Preferred Networks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Tobias Pfeiffer <tgp@preferred.jp>
 */

import io.github.retz.planner.spi.Attribute;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AttrMatcherTest {
    private List<Attribute> getExampleOfferAttrs() {
        List<Attribute> attrs = new LinkedList<>();
        attrs.add(new Attribute("rack", Attribute.Type.TEXT, "A"));
        attrs.add(new Attribute("temp", Attribute.Type.SCALAR, 17.0));
        return attrs;
    }

    @Test
    public void emptyRequirements() {
        assertTrue(AttrPlanner.matches(getExampleOfferAttrs(), ""));
    }

    @Test
    public void emptyOffer() {
        assertFalse(AttrPlanner.matches(new LinkedList<>(), "rack:abc"));
    }

    @Test
    public void matchingTextAttr() {
        assertTrue(AttrPlanner.matches(getExampleOfferAttrs(), "rack:A"));
    }

    @Test
    public void nonMatchingTextAttr() {
        assertFalse(AttrPlanner.matches(getExampleOfferAttrs(), "rack:B"));
    }

    @Test
    public void matchingScalarAttr() {
        assertTrue(AttrPlanner.matches(getExampleOfferAttrs(), "temp:17"));
    }

    @Test
    public void nonMatchingScalarAttr() {
        assertFalse(AttrPlanner.matches(getExampleOfferAttrs(), "temp:18"));
    }

    @Test
    public void matchingMultiAttr() {
        assertTrue(AttrPlanner.matches(getExampleOfferAttrs(), "temp:17;rack:A"));
    }

    @Test
    public void nonMatchingMultiAttr1() {
        assertFalse(AttrPlanner.matches(getExampleOfferAttrs(), "temp:18;rack:A"));
    }

    @Test
    public void nonMatchingMultiAttr2() {
        assertFalse(AttrPlanner.matches(getExampleOfferAttrs(), "temp:17;rack:B"));
    }

    @Test
    public void nonPresentAttr() {
        assertFalse(AttrPlanner.matches(getExampleOfferAttrs(), "size:6"));
    }
}
