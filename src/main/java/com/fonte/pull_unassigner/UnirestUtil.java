package com.fonte.pull_unassigner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;

import java.io.IOException;

public class UnirestUtil
{
	public static void setUnirestObjectMapper()
	{
		Unirest.setObjectMapper(new ObjectMapper()
		{
			private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

			public <T> T readValue(String value, Class<T> valueType)
			{
				try
				{
					return jacksonObjectMapper.readValue(value, valueType);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}

			public String writeValue(Object value)
			{
				try
				{
					return jacksonObjectMapper.writeValueAsString(value);
				}
				catch (JsonProcessingException e)
				{
					throw new RuntimeException(e);
				}
			}
		});

	}

}