package org.jellyware.trinity.conv;

import java.io.StringReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonString;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class URISet implements AttributeConverter<Set<URI>, String> {
	@Override
	public String convertToDatabaseColumn(Set<URI> attribute) {
		var builder = Json.createArrayBuilder();
		if (attribute != null)
			for (var uri : (Iterable<URI>) attribute.stream().sorted()::iterator)
				builder.add(uri.toString());
		return builder.build().toString();
	}

	@Override
	public Set<URI> convertToEntityAttribute(String dbData) {
		if (dbData == null)
			return new HashSet<>();
		return Json.createReader(new StringReader(dbData)).readArray().stream()
				.map(s -> URI.create(((JsonString) s).getString())).collect(Collectors.toCollection(HashSet::new));
	}
}
