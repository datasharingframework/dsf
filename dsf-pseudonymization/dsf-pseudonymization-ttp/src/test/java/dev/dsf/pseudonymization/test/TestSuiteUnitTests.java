package dev.dsf.pseudonymization.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import dev.dsf.pseudonymization.domain.MatchedPersonImplJsonTest;
import dev.dsf.pseudonymization.domain.PersonImplJsonTest;
import dev.dsf.pseudonymization.domain.PseudonymizedPersonImplJsonTest;
import dev.dsf.pseudonymization.psn.PseudonyWithPaddingJsonTest;
import dev.dsf.pseudonymization.psn.PseudonymGeneratorImplTest;
import dev.dsf.pseudonymization.recordlinkage.FederatedMatcherTest;
import dev.dsf.pseudonymization.recordlinkage.SingleOrganizationMatcherTest;

@RunWith(Suite.class)
@SuiteClasses({ MatchedPersonImplJsonTest.class, PersonImplJsonTest.class, PseudonymizedPersonImplJsonTest.class,
		PseudonymGeneratorImplTest.class, PseudonyWithPaddingJsonTest.class, FederatedMatcherTest.class,
		SingleOrganizationMatcherTest.class })
public class TestSuiteUnitTests
{
}
