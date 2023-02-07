package dev.dsf.pseudonymization.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import dev.dsf.pseudonymization.recordlinkage.MatchingTimeTest;
import dev.dsf.pseudonymization.recordlinkage.WeightDistributionTest;

@RunWith(Suite.class)
@SuiteClasses({ MatchingTimeTest.class, WeightDistributionTest.class })
public class TestSuitePerformanceTests
{
}
