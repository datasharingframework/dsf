package dev.dsf.fhir.webservice;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RangeRequestImpl implements RangeRequest
{
	private static final String RANGE_PATTERN_STRING = "bytes=(?:(?<start>\\d+)-(?<end>\\d+)|(?<prefix>\\d+)-|(?<suffix>-\\d+))";
	private static final Pattern RANGE_PATTERN = Pattern.compile(RANGE_PATTERN_STRING);

	public static RangeRequest fromHeaders(BiFunction<String, String, Optional<String>> getHeaderValue)
	{
		Optional<String> rangeHeader = getHeaderValue.apply(RANGE_HEADER, RANGE_HEADER_LC);
		return rangeHeader.map(RangeRequestImpl::fromHeaderValue).orElseGet(() -> new RangeRequestImpl(null, null));
	}

	public static RangeRequest fromHeaderValue(String rangeHeaderValue)
	{
		Matcher matcher = RANGE_PATTERN.matcher(rangeHeaderValue);
		if (matcher.matches())
		{
			String start = matcher.group("start");
			String end = matcher.group("end");
			if (start != null && end != null)
				return new RangeRequestImpl(Long.parseLong(start), Long.parseLong(end));

			String prefix = matcher.group("prefix");
			if (prefix != null)
				return new RangeRequestImpl(Long.parseLong(prefix), null);

			String suffix = matcher.group("suffix");
			if (suffix != null)
				return new RangeRequestImpl(null, Long.parseLong(suffix));
		}

		return new RangeRequestImpl(null, null);
	}

	private final Long start;
	private final Long endInclusive;

	public RangeRequestImpl(Long start, Long endInclusive)
	{
		this.start = start;
		this.endInclusive = endInclusive;
	}

	public boolean isRangeNotDefined()
	{
		return start == null && endInclusive == null; // invalid cases guarded by regex
	}

	public boolean isRangeSatisfiable(long dataSize)
	{
		return isRangeNotDefined() || isFromGivenStartToEndOfFile(dataSize) || isFromGivenStartToGivenEnd(dataSize)
				|| isFromEndOfFileMinusGivenEndToEndOfFile(dataSize);
	}

	public String createRangeHeaderValue(long dataSize)
	{
		if (isFromGivenStartToEndOfFile(dataSize))
			return String.format("bytes %d-%d/%d", start, dataSize - 1, dataSize);
		else if (isFromGivenStartToGivenEnd(dataSize))
			return String.format("bytes %d-%d/%d", start, endInclusive, dataSize);
		else if (isFromEndOfFileMinusGivenEndToEndOfFile(dataSize))
			return String.format("bytes %d-%d/%d", dataSize + endInclusive, dataSize - 1, dataSize);
		else
			return null;
	}

	public String createContentRangeHeaderValue(long dataSize)
	{
		return "bytes */" + dataSize;
	}

	public boolean isFromGivenStartToEndOfFile(long dataSize)
	{
		return start != null && start >= 0 && start < dataSize && endInclusive == null;
	}

	public boolean isFromGivenStartToGivenEnd(long dataSize)
	{
		return start != null && start >= 0 && start < dataSize && endInclusive != null && endInclusive > start
				&& endInclusive < dataSize;
	}

	public boolean isFromEndOfFileMinusGivenEndToEndOfFile(long dataSize)
	{
		return start == null && endInclusive != null && endInclusive < 0 && Math.abs(endInclusive) < dataSize;
	}

	@Override
	public long getRequestedLength(long dataSize)
	{
		if (isRangeNotDefined())
			return dataSize;
		else if (isFromGivenStartToEndOfFile(dataSize))
			return dataSize - start;
		else if (isFromGivenStartToGivenEnd(dataSize))
			return endInclusive + 1 - start;
		else if (isFromEndOfFileMinusGivenEndToEndOfFile(dataSize))
			return Math.abs(endInclusive);
		else // we should only be called with valid requests
			throw new RuntimeException("Range Not Satisfiable");
	}

	@Override
	public long getStart(long dataSize)
	{
		if (isRangeNotDefined())
			return 0;
		else if (isFromGivenStartToEndOfFile(dataSize) || isFromGivenStartToGivenEnd(dataSize))
			return start;
		else if (isFromEndOfFileMinusGivenEndToEndOfFile(dataSize))
			return endInclusive;
		else
			return 0;
	}
}
