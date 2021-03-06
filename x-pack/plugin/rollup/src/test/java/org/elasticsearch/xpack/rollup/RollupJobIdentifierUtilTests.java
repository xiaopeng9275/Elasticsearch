/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.rollup;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.min.MinAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValueType;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.core.rollup.ConfigTestHelpers;
import org.elasticsearch.xpack.core.rollup.RollupField;
import org.elasticsearch.xpack.core.rollup.action.RollupJobCaps;
import org.elasticsearch.xpack.core.rollup.job.DateHistoGroupConfig;
import org.elasticsearch.xpack.core.rollup.job.GroupConfig;
import org.elasticsearch.xpack.core.rollup.job.HistoGroupConfig;
import org.elasticsearch.xpack.core.rollup.job.MetricConfig;
import org.elasticsearch.xpack.core.rollup.job.RollupJobConfig;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class RollupJobIdentifierUtilTests extends ESTestCase {

    private static final List<String> UNITS = new ArrayList<>(DateHistogramAggregationBuilder.DATE_FIELD_UNITS.keySet());

    public void testOneMatch() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(job.getGroupConfig().getDateHisto().getInterval());

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);
        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testBiggerButCompatibleInterval() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1d"));

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);
        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testBiggerButCompatibleFixedInterval() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("100s")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
            .dateHistogramInterval(new DateHistogramInterval("1000s"));

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);
        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testBiggerButCompatibleFixedIntervalNotMultiple() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("300s")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
            .dateHistogramInterval(new DateHistogramInterval("1000s"));

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);
        assertWarnings("Starting in 6.5.0, query intervals must be a multiple of configured intervals.");
        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testBiggerButCompatibleFixedMillisInterval() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("100ms")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
            .interval(1000);

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);
        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testIncompatibleInterval() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1d")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1h"));

        RuntimeException e = expectThrows(RuntimeException.class, () -> RollupJobIdentifierUtils.findBestJobs(builder, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [date_histogram] agg on field " +
                "[foo] which also satisfies all requirements of query."));
    }

    public void testIncompatibleFixedCalendarInterval() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("5d")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);


        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
            .dateHistogramInterval(new DateHistogramInterval("day"));

        RuntimeException e = expectThrows(RuntimeException.class, () -> RollupJobIdentifierUtils.findBestJobs(builder, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [date_histogram] agg on field " +
            "[foo] which also satisfies all requirements of query."));
    }

    public void testRequestFixedConfigCalendarInterval() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1d")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);


        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
            .dateHistogramInterval(new DateHistogramInterval("5d"));

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);
        assertWarnings("Starting in 6.5.0, query and config interval " +
            "types must match (e.g. fixed-time config can only be queried with fixed-time aggregations, " +
            "and calendar-time config can only be queried with calendar-timeaggregations).");

        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testRequestCalendarConfigFixedInterval() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("60m")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);


        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
            .dateHistogramInterval(new DateHistogramInterval("1d"));

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);
        assertWarnings("Starting in 6.5.0, query and config interval " +
            "types must match (e.g. fixed-time config can only be queried with fixed-time aggregations, " +
            "and calendar-time config can only be queried with calendar-timeaggregations).");

        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testRequestMonthCalendarConfigFixedInterval() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("24h")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);


        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
            .dateHistogramInterval(new DateHistogramInterval("1M"));

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);
        assertWarnings("Starting in 6.5.0, query and config interval " +
            "types must match (e.g. fixed-time config can only be queried with fixed-time aggregations, " +
            "and calendar-time config can only be queried with calendar-timeaggregations).");

        assertThat(bestCaps.size(), equalTo(1));
    }


    public void testBadTimeZone() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1d"))
                .setTimeZone(DateTimeZone.forID("EST")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1h"))
                .timeZone(DateTimeZone.UTC);

        RuntimeException e = expectThrows(RuntimeException.class, () -> RollupJobIdentifierUtils.findBestJobs(builder, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [date_histogram] agg on field " +
                "[foo] which also satisfies all requirements of query."));
    }

    public void testMetricOnlyAgg() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build());
        job.setGroupConfig(group.build());
        job.setMetricsConfig(Collections.singletonList(new MetricConfig.Builder()
                .setField("bar")
                .setMetrics(Collections.singletonList("max"))
            .build()));
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        MaxAggregationBuilder max = new MaxAggregationBuilder("the_max").field("bar");

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(max, caps);
        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testOneOfTwoMatchingCaps() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1h"))
                .subAggregation(new MaxAggregationBuilder("the_max").field("bar"));

        RuntimeException e = expectThrows(RuntimeException.class, () -> RollupJobIdentifierUtils.findBestJobs(builder, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [max] agg with name [the_max] which also satisfies " +
                "all requirements of query."));
    }

    public void testTwoJobsSameRollupIndex() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build());
        group.setTerms(null);
        group.setHisto(null);
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = new HashSet<>(2);
        caps.add(cap);

        RollupJobConfig.Builder job2 = ConfigTestHelpers.getRollupJob("foo2");
        GroupConfig.Builder group2 = ConfigTestHelpers.getGroupConfig();
        group2.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build());
        group2.setTerms(null);
        group2.setHisto(null);
        job2.setGroupConfig(group.build());
        job2.setRollupIndex(job.getRollupIndex());
        RollupJobCaps cap2 = new RollupJobCaps(job2.build());
        caps.add(cap2);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1h"));

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);

        // Both jobs functionally identical, so only one is actually needed to be searched
        assertThat(bestCaps.size(), equalTo(1));
    }

    public void testTwoJobsButBothPartialMatches() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build());
        job.setGroupConfig(group.build());
        job.setMetricsConfig(Collections.singletonList(new MetricConfig.Builder()
                .setField("bar")
                .setMetrics(Collections.singletonList("max"))
                .build()));
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = new HashSet<>(2);
        caps.add(cap);

        RollupJobConfig.Builder job2 = ConfigTestHelpers.getRollupJob("foo2");
        GroupConfig.Builder group2 = ConfigTestHelpers.getGroupConfig();
        group2.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build());
        job2.setGroupConfig(group.build());
        job.setMetricsConfig(Collections.singletonList(new MetricConfig.Builder()
                .setField("bar")
                .setMetrics(Collections.singletonList("min"))
                .build()));
        RollupJobCaps cap2 = new RollupJobCaps(job2.build());
        caps.add(cap2);

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1h"))
                .subAggregation(new MaxAggregationBuilder("the_max").field("bar"))  // <-- comes from job1
                .subAggregation(new MinAggregationBuilder("the_min").field("bar")); // <-- comes from job2

        RuntimeException e = expectThrows(RuntimeException.class, () -> RollupJobIdentifierUtils.findBestJobs(builder, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [min] agg with name [the_min] which also " +
                "satisfies all requirements of query."));
    }

    public void testComparableDifferentDateIntervals() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build())
                .setHisto(null)
                .setTerms(null);
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());

        RollupJobConfig.Builder job2 = ConfigTestHelpers.getRollupJob("foo2").setRollupIndex(job.getRollupIndex());
        GroupConfig.Builder group2 = ConfigTestHelpers.getGroupConfig();
        group2.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1d")).build())
                .setHisto(null)
                .setTerms(null);
        job2.setGroupConfig(group2.build());
        RollupJobCaps cap2 = new RollupJobCaps(job2.build());

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1d"));

        Set<RollupJobCaps> caps = new HashSet<>(2);
        caps.add(cap);
        caps.add(cap2);
        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);

        assertThat(bestCaps.size(), equalTo(1));
        assertTrue(bestCaps.contains(cap2));
    }

    public void testComparableDifferentDateIntervalsOnlyOneWorks() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build())
                .setHisto(null)
                .setTerms(null);
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());

        RollupJobConfig.Builder job2 = ConfigTestHelpers.getRollupJob("foo2").setRollupIndex(job.getRollupIndex());
        GroupConfig.Builder group2 = ConfigTestHelpers.getGroupConfig();
        group2.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1d")).build())
                .setHisto(null)
                .setTerms(null);
        job2.setGroupConfig(group2.build());
        RollupJobCaps cap2 = new RollupJobCaps(job2.build());

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1h"));

        Set<RollupJobCaps> caps = new HashSet<>(2);
        caps.add(cap);
        caps.add(cap2);
        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);

        assertThat(bestCaps.size(), equalTo(1));
        assertTrue(bestCaps.contains(cap));
    }

    public void testComparableNoHistoVsHisto() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build())
                .setHisto(null)
                .setTerms(null);
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());

        RollupJobConfig.Builder job2 = ConfigTestHelpers.getRollupJob("foo2").setRollupIndex(job.getRollupIndex());
        GroupConfig.Builder group2 = ConfigTestHelpers.getGroupConfig();
        group2.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build())
                .setHisto(ConfigTestHelpers.getHisto().setInterval(100).setFields(Collections.singletonList("bar")).build())
                .setTerms(null);
        job2.setGroupConfig(group2.build());
        RollupJobCaps cap2 = new RollupJobCaps(job2.build());

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1h"))
                .subAggregation(new HistogramAggregationBuilder("histo").field("bar").interval(100));

        Set<RollupJobCaps> caps = new HashSet<>(2);
        caps.add(cap);
        caps.add(cap2);
        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);

        assertThat(bestCaps.size(), equalTo(1));
        assertTrue(bestCaps.contains(cap2));
    }

    public void testComparableNoTermsVsTerms() {
        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build())
                .setHisto(null)
                .setTerms(null);
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());

        RollupJobConfig.Builder job2 = ConfigTestHelpers.getRollupJob("foo2").setRollupIndex(job.getRollupIndex());
        GroupConfig.Builder group2 = ConfigTestHelpers.getGroupConfig();
        group2.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1h")).build())
                .setHisto(null)
                .setTerms(ConfigTestHelpers.getTerms().setFields(Collections.singletonList("bar")).build());
        job2.setGroupConfig(group2.build());
        RollupJobCaps cap2 = new RollupJobCaps(job2.build());

        DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("foo").field("foo")
                .dateHistogramInterval(new DateHistogramInterval("1h"))
                .subAggregation(new TermsAggregationBuilder("histo", ValueType.STRING).field("bar"));

        Set<RollupJobCaps> caps = new HashSet<>(2);
        caps.add(cap);
        caps.add(cap2);
        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(builder, caps);

        assertThat(bestCaps.size(), equalTo(1));
        assertTrue(bestCaps.contains(cap2));
    }

    public void testHistoSameNameWrongTypeInCaps() {
        HistogramAggregationBuilder histo = new HistogramAggregationBuilder("test_histo");
        histo.field("foo")
                .interval(1L)
                .subAggregation(new MaxAggregationBuilder("the_max").field("max_field"))
                .subAggregation(new AvgAggregationBuilder("the_avg").field("avg_field"));

        RollupJobConfig job = ConfigTestHelpers.getRollupJob("foo")
                .setGroupConfig(ConfigTestHelpers.getGroupConfig()
                        .setDateHisto(new DateHistoGroupConfig.Builder()
                                .setInterval(new DateHistogramInterval("1d"))
                                .setField("foo") // <-- NOTE same name but wrong type
                                .setTimeZone(DateTimeZone.UTC)
                                .build())
                        .setHisto(new HistoGroupConfig.Builder()
                                .setFields(Collections.singletonList("baz")) // <-- NOTE right type but wrong name
                                .setInterval(1L)
                                .build())
                        .build())
                .setMetricsConfig(Arrays.asList(new MetricConfig.Builder()
                                .setField("max_field")
                                .setMetrics(Collections.singletonList("max")).build(),
                        new MetricConfig.Builder()
                                .setField("avg_field")
                                .setMetrics(Collections.singletonList("avg")).build()))
                .build();
        Set<RollupJobCaps> caps = singletonSet(new RollupJobCaps(job));

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> RollupJobIdentifierUtils.findBestJobs(histo, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [histogram] " +
                "agg on field [foo] which also satisfies all requirements of query."));
    }

    public void testMissingDateHisto() {
        DateHistogramAggregationBuilder histo = new DateHistogramAggregationBuilder("test_histo");
        histo.dateHistogramInterval(new DateHistogramInterval("1d"))
                .field("other_field")
                .subAggregation(new MaxAggregationBuilder("the_max").field("max_field"))
                .subAggregation(new AvgAggregationBuilder("the_avg").field("avg_field"));

        RollupJobConfig job = ConfigTestHelpers.getRollupJob("foo")
                .setGroupConfig(ConfigTestHelpers.getGroupConfig()
                        .setDateHisto(new DateHistoGroupConfig.Builder()
                                .setInterval(new DateHistogramInterval("1d"))
                                .setField("foo")
                                .setTimeZone(DateTimeZone.UTC)
                                .build())
                        .build())
                .setMetricsConfig(Arrays.asList(new MetricConfig.Builder()
                                .setField("max_field")
                                .setMetrics(Collections.singletonList("max")).build(),
                        new MetricConfig.Builder()
                                .setField("avg_field")
                                .setMetrics(Collections.singletonList("avg")).build()))
                .build();
        Set<RollupJobCaps> caps = singletonSet(new RollupJobCaps(job));

        Exception e = expectThrows(IllegalArgumentException.class, () -> RollupJobIdentifierUtils.findBestJobs(histo,caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [date_histogram] agg on field " +
                "[other_field] which also satisfies all requirements of query."));
    }

    public void testNoMatchingInterval() {
        DateHistogramAggregationBuilder histo = new DateHistogramAggregationBuilder("test_histo");
        histo.interval(1)
                .field("foo")
                .subAggregation(new MaxAggregationBuilder("the_max").field("max_field"))
                .subAggregation(new AvgAggregationBuilder("the_avg").field("avg_field"));

        RollupJobConfig job = ConfigTestHelpers.getRollupJob("foo")
                .setGroupConfig(ConfigTestHelpers.getGroupConfig()
                        .setDateHisto(new DateHistoGroupConfig.Builder()
                                .setInterval(new DateHistogramInterval("100d")) // <- interval in job is much higher than agg interval above
                                .setField("foo")
                                .setTimeZone(DateTimeZone.UTC)
                                .build())
                        .build())
                .build();
        Set<RollupJobCaps> caps = singletonSet(new RollupJobCaps(job));

        Exception e = expectThrows(RuntimeException.class, () -> RollupJobIdentifierUtils.findBestJobs(histo, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [date_histogram] agg on field [foo] " +
                "which also satisfies all requirements of query."));
    }

    public void testDateHistoMissingFieldInCaps() {
        DateHistogramAggregationBuilder histo = new DateHistogramAggregationBuilder("test_histo");
        histo.dateHistogramInterval(new DateHistogramInterval("1d"))
                .field("foo")
                .subAggregation(new MaxAggregationBuilder("the_max").field("max_field"))
                .subAggregation(new AvgAggregationBuilder("the_avg").field("avg_field"));

        RollupJobConfig job = ConfigTestHelpers.getRollupJob("foo")
                .setGroupConfig(ConfigTestHelpers.getGroupConfig()
                        .setDateHisto(new DateHistoGroupConfig.Builder()
                                .setInterval(new DateHistogramInterval("1d"))
                                .setField("bar") // <-- NOTE different field from the one in the query
                                .setTimeZone(DateTimeZone.UTC)
                                .build())
                        .build())
                .setMetricsConfig(Arrays.asList(new MetricConfig.Builder()
                                .setField("max_field")
                                .setMetrics(Collections.singletonList("max")).build(),
                        new MetricConfig.Builder()
                                .setField("avg_field")
                                .setMetrics(Collections.singletonList("avg")).build()))
                .build();
        Set<RollupJobCaps> caps = singletonSet(new RollupJobCaps(job));

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> RollupJobIdentifierUtils.findBestJobs(histo, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [date_histogram] agg on field [foo] which also " +
                "satisfies all requirements of query."));
    }

    public void testHistoMissingFieldInCaps() {
        HistogramAggregationBuilder histo = new HistogramAggregationBuilder("test_histo");
        histo.interval(1)
                .field("foo")
                .subAggregation(new MaxAggregationBuilder("the_max").field("max_field"))
                .subAggregation(new AvgAggregationBuilder("the_avg").field("avg_field"));

        RollupJobConfig job = ConfigTestHelpers.getRollupJob("foo")
                .setGroupConfig(ConfigTestHelpers.getGroupConfig()
                        .setDateHisto(new DateHistoGroupConfig.Builder()
                                .setInterval(new DateHistogramInterval("1d"))
                                .setField("bar")
                                .setTimeZone(DateTimeZone.UTC)
                                .build())
                        .setHisto(new HistoGroupConfig.Builder()
                                .setFields(Collections.singletonList("baz")) // <-- NOTE note different field from one used in query
                                .setInterval(1L)
                                .build())
                        .build())
                .setMetricsConfig(Arrays.asList(new MetricConfig.Builder()
                                .setField("max_field")
                                .setMetrics(Collections.singletonList("max")).build(),
                        new MetricConfig.Builder()
                                .setField("avg_field")
                                .setMetrics(Collections.singletonList("avg")).build()))
                .build();
        Set<RollupJobCaps> caps = singletonSet(new RollupJobCaps(job));

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> RollupJobIdentifierUtils.findBestJobs(histo, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [histogram] agg on field [foo] which also " +
                "satisfies all requirements of query."));
    }

    public void testNoMatchingHistoInterval() {
        HistogramAggregationBuilder histo = new HistogramAggregationBuilder("test_histo");
        histo.interval(1)
                .field("bar")
                .subAggregation(new MaxAggregationBuilder("the_max").field("max_field"))
                .subAggregation(new AvgAggregationBuilder("the_avg").field("avg_field"));

        RollupJobConfig job = ConfigTestHelpers.getRollupJob("foo")
                .setGroupConfig(ConfigTestHelpers.getGroupConfig()
                        .setDateHisto(new DateHistoGroupConfig.Builder()
                                .setInterval(new DateHistogramInterval("1d"))
                                .setField("foo")
                                .setTimeZone(DateTimeZone.UTC)
                                .build())
                        .setHisto(new HistoGroupConfig.Builder()
                                .setFields(Collections.singletonList("bar"))
                                .setInterval(100L) // <--- interval in job is much higher than agg interval above
                                .build())
                        .build())
                .build();
        Set<RollupJobCaps> caps = singletonSet(new RollupJobCaps(job));

        Exception e = expectThrows(RuntimeException.class,
                () -> RollupJobIdentifierUtils.findBestJobs(histo, caps));
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [histogram] agg on field " +
                "[bar] which also satisfies all requirements of query."));
    }

    public void testHistoIntervalNotMultiple() {
        HistogramAggregationBuilder histo = new HistogramAggregationBuilder("test_histo");
        histo.interval(10)  // <--- interval is not a multiple of 3
            .field("bar");

        RollupJobConfig.Builder job = ConfigTestHelpers.getRollupJob("foo");
        GroupConfig.Builder group = ConfigTestHelpers.getGroupConfig();
        group.setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(new DateHistogramInterval("1d")).build());
        group.setHisto(new HistoGroupConfig.Builder().setFields(Collections.singletonList("bar")).setInterval(3L).build());
        job.setGroupConfig(group.build());
        RollupJobCaps cap = new RollupJobCaps(job.build());
        Set<RollupJobCaps> caps = singletonSet(cap);

        Set<RollupJobCaps> bestCaps = RollupJobIdentifierUtils.findBestJobs(histo, caps);

        assertThat(bestCaps.size(), equalTo(1));
        assertWarnings("Starting in 6.5.0, query intervals must be a multiple of configured intervals.");
    }

    public void testMissingMetric() {
        int i = ESTestCase.randomIntBetween(0, 3);

        Set<RollupJobCaps> caps = singletonSet(new RollupJobCaps(ConfigTestHelpers
                .getRollupJob("foo").setMetricsConfig(Collections.singletonList(new MetricConfig.Builder()
                        .setField("foo")
                        .setMetrics(Arrays.asList("avg", "max", "min", "sum")).build()))
                .build()));

        String aggType;
        Exception e;
        if (i == 0) {
            e = expectThrows(IllegalArgumentException.class,
                    () -> RollupJobIdentifierUtils.findBestJobs(new MaxAggregationBuilder("test_metric").field("other_field"), caps));
            aggType = "max";
        } else if (i == 1) {
            e = expectThrows(IllegalArgumentException.class,
                    () -> RollupJobIdentifierUtils.findBestJobs(new MinAggregationBuilder("test_metric").field("other_field"), caps));
            aggType = "min";
        } else if (i == 2) {
            e = expectThrows(IllegalArgumentException.class,
                    () -> RollupJobIdentifierUtils.findBestJobs(new SumAggregationBuilder("test_metric").field("other_field"), caps));
            aggType = "sum";
        } else {
            e = expectThrows(IllegalArgumentException.class,
                    () -> RollupJobIdentifierUtils.findBestJobs(new AvgAggregationBuilder("test_metric").field("other_field"),  caps));
            aggType = "avg";
        }
        assertThat(e.getMessage(), equalTo("There is not a rollup job that has a [" + aggType + "] agg with name " +
                "[test_metric] which also satisfies all requirements of query."));

    }

    public void testValidateFixedInterval() {
        boolean valid = RollupJobIdentifierUtils.validateFixedInterval(100, new DateHistogramInterval("100ms"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(200, new DateHistogramInterval("100ms"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(1000, new DateHistogramInterval("200ms"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(5*60*1000, new DateHistogramInterval("5m"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(10*5*60*1000, new DateHistogramInterval("5m"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(100, new DateHistogramInterval("500ms"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(100, new DateHistogramInterval("5m"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(100, new DateHistogramInterval("minute"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(100, new DateHistogramInterval("second"));
        assertFalse(valid);

        // -----------
        // Same tests, with both being DateHistoIntervals
        // -----------
        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("100ms"),
            new DateHistogramInterval("100ms"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("200ms"),
            new DateHistogramInterval("100ms"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("1000ms"),
            new DateHistogramInterval("200ms"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("5m"),
            new DateHistogramInterval("5m"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("20m"),
            new DateHistogramInterval("5m"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("100ms"),
            new DateHistogramInterval("500ms"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("100ms"),
            new DateHistogramInterval("5m"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("100ms"),
            new DateHistogramInterval("minute"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateFixedInterval(new DateHistogramInterval("100ms"),
            new DateHistogramInterval("second"));
        assertFalse(valid);
    }

    public void testValidateCalendarInterval() {
        boolean valid = RollupJobIdentifierUtils.validateCalendarInterval(new DateHistogramInterval("second"),
            new DateHistogramInterval("second"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateCalendarInterval(new DateHistogramInterval("minute"),
            new DateHistogramInterval("second"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateCalendarInterval(new DateHistogramInterval("month"),
            new DateHistogramInterval("day"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateCalendarInterval(new DateHistogramInterval("1d"),
            new DateHistogramInterval("1s"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateCalendarInterval(new DateHistogramInterval("second"),
            new DateHistogramInterval("minute"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateCalendarInterval(new DateHistogramInterval("second"),
            new DateHistogramInterval("1m"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateCalendarInterval(new DateHistogramInterval("1M"),
            new DateHistogramInterval("minute"));
        assertTrue(valid);

        // Fails because both are actually fixed
        valid = RollupJobIdentifierUtils.validateCalendarInterval(new DateHistogramInterval("100ms"),
            new DateHistogramInterval("100ms"));
        assertFalse(valid);
    }

    public void testMixedIntervals() {
        boolean valid = RollupJobIdentifierUtils.validateMixedInterval(60 * 1000, new DateHistogramInterval("1m"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(10 * 60 * 1000, new DateHistogramInterval("1m"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(100, new DateHistogramInterval("1d"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(1000 * 60 * 60 * 24, new DateHistogramInterval("1d"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(100, new DateHistogramInterval("minute"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(1000 * 60 * 60, new DateHistogramInterval("minute"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(100, new DateHistogramInterval("second"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(1000, new DateHistogramInterval("second"));
        assertTrue(valid);

        // -----------
        // Same tests, with both being DateHistoIntervals
        // -----------
        valid = RollupJobIdentifierUtils.validateMixedInterval(new DateHistogramInterval("60s"), new DateHistogramInterval("1m"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(new DateHistogramInterval("10m"), new DateHistogramInterval("1m"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(new DateHistogramInterval("100ms"), new DateHistogramInterval("1d"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(new DateHistogramInterval("24h"), new DateHistogramInterval("1d"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(new DateHistogramInterval("10s"), new DateHistogramInterval("minute"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(new DateHistogramInterval("2m"), new DateHistogramInterval("minute"));
        assertTrue(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(new DateHistogramInterval("100ms"), new DateHistogramInterval("second"));
        assertFalse(valid);

        valid = RollupJobIdentifierUtils.validateMixedInterval(new DateHistogramInterval("60s"), new DateHistogramInterval("second"));
        assertTrue(valid);

        assertWarnings("Starting in 6.5.0, query and config interval " +
            "types must match (e.g. fixed-time config can only be queried with fixed-time aggregations, " +
            "and calendar-time config can only be queried with calendar-timeaggregations).");
    }

    public void testComparatorMixed() {
        int numCaps = randomIntBetween(1, 10);
        List<RollupJobCaps> caps = new ArrayList<>(numCaps);

        for (int i = 0; i < numCaps; i++) {
            DateHistogramInterval interval = getRandomInterval();
            GroupConfig group = new GroupConfig.Builder()
                .setDateHisto(new DateHistoGroupConfig.Builder().setField("foo").setInterval(interval).build())
                .build();
            RollupJobConfig job = new RollupJobConfig.Builder()
                .setId("foo")
                .setIndexPattern("index")
                .setRollupIndex("rollup")
                .setPageSize(10)
                .setGroupConfig(group)
                .setCron("*/5 * * * * ?")
                .build();
            RollupJobCaps cap = new RollupJobCaps(job);
            caps.add(cap);
        }

        caps.sort(RollupJobIdentifierUtils.COMPARATOR);

        // This only tests for calendar/fixed ordering, ignoring the other criteria
        for (int i = 1; i < numCaps; i++) {
            RollupJobCaps a = caps.get(i - 1);
            RollupJobCaps b = caps.get(i);
            long aMillis = getMillis(a);
            long bMillis = getMillis(b);

            assertThat(aMillis, greaterThanOrEqualTo(bMillis));

        }
    }

    public void testComparatorFixed() {
        int numCaps = randomIntBetween(1, 10);
        List<RollupJobCaps> caps = new ArrayList<>(numCaps);

        for (int i = 0; i < numCaps; i++) {
            DateHistogramInterval interval = getRandomFixedInterval();
            GroupConfig group = new GroupConfig.Builder()
                .setDateHisto(
                    new DateHistoGroupConfig.Builder()
                        .setField("foo")
                        .setInterval(interval)
                        .build())
                .build();
            RollupJobConfig job = new RollupJobConfig.Builder()
                .setId("foo")
                .setIndexPattern("index")
                .setRollupIndex("rollup")
                .setCron("*/5 * * * * ?")
                .setPageSize(10)
                .setGroupConfig(group)
                .build();
            RollupJobCaps cap = new RollupJobCaps(job);
            caps.add(cap);
        }

        caps.sort(RollupJobIdentifierUtils.COMPARATOR);

        // This only tests for fixed ordering, ignoring the other criteria
        for (int i = 1; i < numCaps; i++) {
            RollupJobCaps a = caps.get(i - 1);
            RollupJobCaps b = caps.get(i);
            long aMillis = getMillis(a);
            long bMillis = getMillis(b);

            assertThat(aMillis, greaterThanOrEqualTo(bMillis));

        }
    }

    public void testComparatorCalendar() {
        int numCaps = randomIntBetween(1, 10);
        List<RollupJobCaps> caps = new ArrayList<>(numCaps);

        for (int i = 0; i < numCaps; i++) {
            DateHistogramInterval interval = getRandomCalendarInterval();
            GroupConfig group = new GroupConfig.Builder()
                .setDateHisto(
                    new DateHistoGroupConfig.Builder()
                        .setField("foo")
                        .setInterval(interval)
                        .build())
                .build();
            RollupJobConfig job = new RollupJobConfig.Builder()
                .setId("foo")
                .setIndexPattern("index")
                .setRollupIndex("rollup")
                .setCron("*/5 * * * * ?")
                .setPageSize(10)
                .setGroupConfig(group)
                .build();
            RollupJobCaps cap = new RollupJobCaps(job);
            caps.add(cap);
        }

        caps.sort(RollupJobIdentifierUtils.COMPARATOR);

        // This only tests for calendar ordering, ignoring the other criteria
        for (int i = 1; i < numCaps; i++) {
            RollupJobCaps a = caps.get(i - 1);
            RollupJobCaps b = caps.get(i);
            long aMillis = getMillis(a);
            long bMillis = getMillis(b);

            assertThat(aMillis, greaterThanOrEqualTo(bMillis));

        }
    }

    private static long getMillis(RollupJobCaps cap) {
        for (RollupJobCaps.RollupFieldCaps fieldCaps : cap.getFieldCaps().values()) {
            for (Map<String, Object> agg : fieldCaps.getAggs()) {
                if (agg.get(RollupField.AGG).equals(DateHistogramAggregationBuilder.NAME)) {
                    return RollupJobIdentifierUtils.getMillisFixedOrCalendar((String) agg.get(RollupField.INTERVAL));
                }
            }
        }
        return Long.MAX_VALUE;
    }

    private static DateHistogramInterval getRandomInterval() {
        if (randomBoolean()) {
            return getRandomFixedInterval();
        }
        return getRandomCalendarInterval();
    }

    private static DateHistogramInterval getRandomFixedInterval() {
        int value = randomIntBetween(1, 1000);
        String unit;
        int randomValue = randomInt(4);
        if (randomValue == 0) {
            unit = "ms";
        } else if (randomValue == 1) {
            unit = "s";
        } else if (randomValue == 2) {
            unit = "m";
        } else if (randomValue == 3) {
            unit = "h";
        } else {
            unit = "d";
        }
        return new DateHistogramInterval(Integer.toString(value) + unit);
    }

    private static DateHistogramInterval getRandomCalendarInterval() {
        return new DateHistogramInterval(UNITS.get(randomIntBetween(0, UNITS.size()-1)));
    }

    private Set<RollupJobCaps> singletonSet(RollupJobCaps cap) {
        Set<RollupJobCaps> caps = new HashSet<>();
        caps.add(cap);
        return caps;
    }
}
