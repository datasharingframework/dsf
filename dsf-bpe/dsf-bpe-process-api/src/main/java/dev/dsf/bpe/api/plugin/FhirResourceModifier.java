package dev.dsf.bpe.api.plugin;

public interface FhirResourceModifier
{
	FhirResourceModifier IDENTITY = new FhirResourceModifier()
	{
		@Override
		public Object modifyValueSet(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyTask(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyStructureDefinition(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyQuestionnaire(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyNamingSystem(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyMeasure(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyLibrary(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyCodeSystem(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyActivityDefinition(String filename, Object resource)
		{
			return resource;
		}
	};

	static FhirResourceModifier identity()
	{
		return IDENTITY;
	}

	Object modifyActivityDefinition(String filename, Object resource);

	Object modifyCodeSystem(String filename, Object resource);

	Object modifyLibrary(String filename, Object resource);

	Object modifyMeasure(String filename, Object resource);

	Object modifyNamingSystem(String filename, Object resource);

	Object modifyQuestionnaire(String filename, Object resource);

	Object modifyStructureDefinition(String filename, Object resource);

	Object modifyTask(String filename, Object resource);

	Object modifyValueSet(String filename, Object resource);
}
