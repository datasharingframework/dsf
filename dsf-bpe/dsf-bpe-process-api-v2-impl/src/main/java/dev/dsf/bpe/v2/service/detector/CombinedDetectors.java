package dev.dsf.bpe.v2.service.detector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

public class CombinedDetectors implements Detector
{
	public static CombinedDetectors withDefaultAndNdJson(int ndJsonLinesToCheck)
	{
		Detector defaultDetector = TikaConfig.getDefaultConfig().getDetector();
		NdJsonDetector ndJsonDetector = new NdJsonDetector(defaultDetector, ndJsonLinesToCheck);

		return new CombinedDetectors(List.of(defaultDetector, ndJsonDetector));
	}

	private final List<Detector> detectors = new ArrayList<>();

	public CombinedDetectors(List<Detector> detectors)
	{
		if (detectors != null && !detectors.isEmpty())
			this.detectors.addAll(detectors);

		if (this.detectors.isEmpty())
			throw new RuntimeException("No detectors supplied");
	}

	@Override
	public MediaType detect(InputStream inputStream, Metadata metadata)
	{
		// Each detector is responsible to mark and reset the input stream them self
		// and to check if the input stream is null

		List<MediaType> detectedMediaTypesNotEmptyNotOctetStream = detectors.stream()
				.map(doDetect(inputStream, metadata)).filter(notEqualsMediaType(MediaType.EMPTY))
				.filter(notEqualsMediaType(MediaType.OCTET_STREAM)).toList();

		List<MediaType> detectedMediaTypesNotEmptyNotOctetStreamNotPlainText = detectedMediaTypesNotEmptyNotOctetStream
				.stream().filter(notEqualsMediaType(MediaType.TEXT_PLAIN)).toList();

		if (!detectedMediaTypesNotEmptyNotOctetStreamNotPlainText.isEmpty())
			return detectedMediaTypesNotEmptyNotOctetStreamNotPlainText.get(0);

		if (!detectedMediaTypesNotEmptyNotOctetStream.isEmpty())
			return detectedMediaTypesNotEmptyNotOctetStream.get(0);

		return MediaType.OCTET_STREAM;
	}

	private Function<Detector, MediaType> doDetect(InputStream input, Metadata metadata)
	{
		return (detector) ->
		{
			try
			{
				return detector.detect(input, metadata);
			}
			catch (IOException exception)
			{
				throw new RuntimeException("Error while detecting mimetype", exception);
			}
		};
	}

	private Predicate<MediaType> notEqualsMediaType(MediaType toCompare)
	{
		return (mediaType) -> toCompare != null && !toCompare.equals(mediaType);
	}
}
