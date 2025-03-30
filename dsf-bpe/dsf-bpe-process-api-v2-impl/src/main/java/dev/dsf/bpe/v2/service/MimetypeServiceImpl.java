package dev.dsf.bpe.v2.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class MimetypeServiceImpl implements MimetypeService, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(MimetypeServiceImpl.class);

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
	public void validate(InputStream stream, String declared)
	{
		MediaType declaredMimeType = MediaType.parse(declared);
		MediaType detectedMimeType;

		try
		{
			TikaInputStream input = TikaInputStream.get(stream);

			// Gives only a hint to the possible mime-type, this is needed because text/csv and application/json
			// cannot be detected without any hint and would resolve to text/plain.
			Metadata metadata = new Metadata();
			metadata.add(Metadata.CONTENT_TYPE, declaredMimeType.toString());

			detectedMimeType = detector.detect(input, metadata);
		}
		catch (IOException exception)
		{
			throw new RuntimeException("Error while detecting mimetype", exception);
		}

		if (!declaredMimeType.equals(detectedMimeType))
			logger.warn("Declared full mimetype {} does not match detected full mimetype {}",
					declaredMimeType.toString(), detectedMimeType.toString());

		if (!declaredMimeType.getType().equals(detectedMimeType.getType()))
		{
			throw new RuntimeException("Declared base mimetype of '" + declaredMimeType.toString()
					+ "' does not match detected base mimetype of '" + detectedMimeType.toString() + "'");
		}
	}
}
