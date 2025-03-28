package dev.dsf.bpe.v2.service.detector;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

public class NdJsonDetector implements Detector
{
	private static final MediaType MEDIA_TYPE_JSON = MediaType.application("json");
	private static final MediaType MEDIA_TYPE_NDJSON = MediaType.application("x-ndjson");

	public static final int DEFAULT_LINES_TO_CHECK = 3;

	private final Detector defaultDetector;
	private final int linesToCheck;

	public NdJsonDetector(Detector defaultDetector)
	{
		this(defaultDetector, DEFAULT_LINES_TO_CHECK);
	}

	public NdJsonDetector(Detector defaultDetector, int linesToCheck)
	{
		this.defaultDetector = defaultDetector;
		this.linesToCheck = linesToCheck;

		Objects.requireNonNull(this.defaultDetector, "defaultDetector");

		if (this.linesToCheck < 1)
			throw new IllegalArgumentException("lines to check must be greater zero (" + this.linesToCheck + ")");
	}

	@Override
	public MediaType detect(InputStream inputStream, Metadata metadata) throws IOException
	{
		if (inputStream == null)
			return MediaType.OCTET_STREAM;

		// Using own metadata as provided metadata should not be changed (see method definition in interface)
		Metadata internalMetadata = new Metadata();

		// Gives only a hint to the possible mimetype, this is needed because application/json
		// cannot be detected without any hint and would resolve to text/plain.
		// As we are checking line by line for JSON to detect if the content is application/x-ndjson, we have to reset
		// the hint to application/json.
		if (metadata != null && MEDIA_TYPE_NDJSON.toString().equals(metadata.get(Metadata.CONTENT_TYPE)))
		{
			internalMetadata.add(Metadata.CONTENT_TYPE, MEDIA_TYPE_JSON.toString());
		}

		List<MediaType> detectedMediaTypes = new ArrayList<>();

		String line;
		int lineCounter = 0;

		inputStream.mark(Integer.MAX_VALUE);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		while ((line = reader.readLine()) != null && lineCounter < linesToCheck)
		{
			InputStream internalInputStream = new ByteArrayInputStream(line.getBytes());
			MediaType mediaType = defaultDetector.detect(internalInputStream, internalMetadata);
			detectedMediaTypes.add(mediaType);

			lineCounter++;
		}
		inputStream.reset();

		boolean allMatch = detectedMediaTypes.stream().allMatch(this::isJson);

		if (allMatch)
			return MEDIA_TYPE_NDJSON;
		else
			return MediaType.OCTET_STREAM;
	}

	private boolean isJson(MediaType mediaType)
	{
		return MEDIA_TYPE_JSON.toString().equals(mediaType.toString());
	}
}
