package org.jellyware.trinity.conv;

import java.net.URI;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class StringURI implements AttributeConverter<URI, String> {
	@Override
	public String convertToDatabaseColumn(URI attribute) {
		return attribute.toString();
	}

	@Override
	public URI convertToEntityAttribute(String dbData) {
		return URI.create(dbData);
	}
}
