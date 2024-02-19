package dev.dsf.common.ui.theme;

public enum Theme
{
	DEV, TEST, PROD;

	public static Theme fromString(String s)
	{
		if (s == null || s.isBlank())
			return null;
		else
		{
			return switch (s.toLowerCase())
			{
				case "dev" -> DEV;
				case "test" -> TEST;
				case "prod" -> PROD;
				default -> null;
			};
		}
	}

	@Override
	public String toString()
	{
		return name().toLowerCase();
	}
}