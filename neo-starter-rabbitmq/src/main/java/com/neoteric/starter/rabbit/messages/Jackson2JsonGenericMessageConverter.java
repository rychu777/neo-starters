package com.neoteric.starter.rabbit.messages;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Havily based on Jackson2JsonMessageConverter
 * added JavaType created with constructParametrizedType method
 */
public class Jackson2JsonGenericMessageConverter extends Jackson2JsonMessageConverter {

    private final String OBJECT_MAPPER_FIELD_NAME = "jsonObjectMapper";
    private ObjectMapper jsonObjectMapper;

    public Jackson2JsonGenericMessageConverter() {
        super();
        Field objectMapperField = ReflectionUtils.findField(Jackson2JsonMessageConverter.class, OBJECT_MAPPER_FIELD_NAME);
        ReflectionUtils.makeAccessible(objectMapperField);
        jsonObjectMapper = (ObjectMapper) ReflectionUtils.getField(objectMapperField, this);
    }

    @Override
    protected Message createMessage(Object objectToConvert,
                                    MessageProperties messageProperties)
            throws MessageConversionException {

        byte[] bytes = null;
        try {
            String jsonString = jsonObjectMapper
                    .writeValueAsString(objectToConvert);
            bytes = jsonString.getBytes(getDefaultCharset());
        } catch (IOException e) {
            throw new MessageConversionException(
                    "Failed to convert Message content", e);
        }
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(getDefaultCharset());
        if (bytes != null) {
            messageProperties.setContentLength(bytes.length);
        }

        if (getClassMapper() == null) {

            JavaType javaType;
            if (objectToConvert instanceof Parametrized) {
                Class parameterClass = ((Parametrized) objectToConvert).getParameterType();
                javaType = jsonObjectMapper.getTypeFactory().constructParametrizedType(
                        objectToConvert.getClass(), objectToConvert.getClass(), parameterClass);
            } else {
                javaType = jsonObjectMapper.constructType(objectToConvert.getClass());
            }

            getJavaTypeMapper().fromJavaType(javaType, messageProperties);

        } else {
            getClassMapper().fromClass(objectToConvert.getClass(), messageProperties);

        }

        return new Message(bytes, messageProperties);
    }
}