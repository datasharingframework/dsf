package dev.dsf.bpe.v2.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.springframework.beans.factory.InitializingBean;

public class MimetypeServiceImpl implements MimetypeService, InitializingBean
{
	private final Detector detector;

	public MimetypeServiceImpl(Detector detector)
	{
		this.detector = detector;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(detector, "detector");
	}

	@Override
	public ValidationResult validateWithResult(InputStream stream, String declared)
	{
		MediaType declaredMimeType = MediaType.parse(declared);

		try
		{
			TikaInputStream input = TikaInputStream.get(stream);

			// Gives only a hint to the possible mime-type, this is needed because text/csv and application/json
			// cannot be detected without any hint and would resolve to text/plain.
			Metadata metadata = new Metadata();
			metadata.add(Metadata.CONTENT_TYPE, declaredMimeType.toString());

			MediaType detectedMimeType = detector.detect(input, metadata);

			return new ValidationResult(declaredMimeType.getType(), declaredMimeType.getSubtype(),
					detectedMimeType.getType(), detectedMimeType.getSubtype());
		}
		catch (IOException exception)
		{
			throw new RuntimeException("Error while detecting mimetype", exception);
		}
	}
}
