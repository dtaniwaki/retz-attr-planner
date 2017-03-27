# Retz Attribute Planner

## Overview

This planner is a proof-of-concept of a Retz planner that can
take Mesos attributes into account when matching jobs and
offers. That is, if a `Job` has an `attributes` string such as
`rack:abc` set, then only offers from Mesos nodes with a
`rack` attribute having the value `abc` will be considered.

## Supported Attributes

At the moment only scalar and text attributes and only equality
comparisons are supported.  The constraints are specified with
the same syntax as attributes when passed to `mesos-slave`,
for example `floor:17;rack:A`.

## Other Planner Properties

Besides support for attributes, this planner uses a couple of
heuristics in order to achieve that "harder to schedule" jobs
are scheduled first.  That is, jobs will first be ordered

1. by priority (descending)
2. by whether they have attributes or not
3. by required number of GPUs (descending)

and then distributed to matching offers.

# Usage/Installation

This planner requires Retz in version 2.6.0-SNAPSHOT (after
commit c9d6003) or later.

Build it using

```sh
make jar
```

and place `build/libs/retz-attr-planner-0.0.1-SNAPSHOT.jar`
to the classpath configured in `retz.properties`.  Also, the
planner must be activated by setting
`retz.planner.name = io.github.retz.planner.attr.AttrPlanner`.
