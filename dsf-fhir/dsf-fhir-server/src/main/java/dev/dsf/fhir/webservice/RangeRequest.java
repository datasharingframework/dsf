package dev.dsf.fhir.webservice;

public interface RangeRequest
{
	String USER_DATA_VALUE_RANGE_REQUEST = "range-request";
	String USER_DATA_VALUE_DATA_SIZE = "data-size";

	String RANGE_HEADER = "Range";
	String RANGE_HEADER_LC = RANGE_HEADER.toLowerCase();
	String IF_RANGE_HEADER = "If-Range";
	String IF_RANGE_HEADER_LC = "If-Range".toLowerCase();

	String CONTENT_RANGE_HEADER = "Content-Range";
	String ACCEPT_RANGES_HEADER = "Accept-Ranges";
	String ACCEPT_RANGES_HEADER_VALUE = "bytes";

	long getStart(long dataSize);

	long getRequestedLength(long dataSize);

	boolean isRangeSatisfiable(long dataSize);

	boolean isRangeNotDefined();

	String createContentRangeHeaderValue(long dataSize);

	String createRangeHeaderValue(long dataSize);
}
