package dev.dsf.bpe.v2.activity.task;

import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;

public interface BusinessKeyStrategy
{
	String get(Variables variables, Target target);
}
