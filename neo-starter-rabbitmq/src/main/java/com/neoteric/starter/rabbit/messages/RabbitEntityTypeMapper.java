package com.neoteric.starter.rabbit.messages;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Lists;
import com.neoteric.starter.rabbit.StarterRabbitProperties;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class RabbitEntityTypeMapper extends DefaultJackson2JavaTypeMapper implements Jackson2JavaTypeMapper, ClassMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitEntityTypeMapper.class);
    public static final String RABBIT_PACKAGE = "com.neoteric.starter.rabbit";

    private final StarterRabbitProperties rabbitProperties;
    private final RabbitEntitiesProvider rabbitEntitiesProvider = new RabbitEntitiesProvider();

    public RabbitEntityTypeMapper(StarterRabbitProperties rabbitProperties) {
        this.rabbitProperties = rabbitProperties;
    }

    @Override
    public JavaType toJavaType(MessageProperties properties) {
        JavaType classType = getClassType(properties, getEntityIdFieldName(), getEntityParameterIdFieldName(), getClassIdFieldName());
        if (!classType.isContainerType() || classType.isArrayType()) {
            return classType;
        }

        JavaType contentClassType = getClassType(properties, getContentEntityIdFieldName(),
                getContentEntityParameterIdFieldName(), getContentClassIdFieldName());
        if (classType.getKeyType() == null) {
            return CollectionType.construct(classType.getRawClass(), contentClassType);
        }

        JavaType keyClassType = getClassIdType(retrieveHeader(properties, getKeyClassIdFieldName()));
        return MapType.construct(classType.getRawClass(), keyClassType, contentClassType);
    }

    @Override
    public void fromJavaType(JavaType javaType, MessageProperties properties) {
        this.addHeader(properties, this.getClassIdFieldName(), javaType.getRawClass());
        this.putRabbitEntityInfoInHeaders(javaType, properties, this.getEntityIdFieldName(), this.getEntityParameterIdFieldName());

        if (javaType.isContainerType() && !javaType.isArrayType()) {
            addHeader(properties, getContentClassIdFieldName(), javaType.getContentType().getRawClass());
            this.putRabbitEntityInfoInHeaders(javaType, properties, this.getContentEntityIdFieldName(), this.getContentEntityParameterIdFieldName());
        }

        if (javaType.getKeyType() != null) {
            addHeader(properties, getKeyClassIdFieldName(), javaType.getKeyType().getRawClass());
        }
    }

    private void putRabbitEntityInfoInHeaders(JavaType javaType, MessageProperties properties, String entityIdHeader,
                                              String entityParameterIdHeader) {
        if (rabbitEntitiesProvider.getAnnotatedClassesJavaTypes().containsValue(javaType)) {
            if (javaType.hasGenericTypes()) {
                this.addHeader(properties, entityIdHeader, javaType.getRawClass().getAnnotation(ParametrizedRabbitEntity.class).value());
                this.addHeader(properties, entityParameterIdHeader,
                        javaType.containedTypeOrUnknown(0).getRawClass().getAnnotation(RabbitEntity.class).value());
            } else {
                this.addHeader(properties, entityIdHeader, javaType.getRawClass().getAnnotation(RabbitEntity.class).value());
            }
        }
    }

    private JavaType getClassType(MessageProperties properties, String entityIdHeader, String entityParameterIdHeader, String classIdHeader) {
        if (properties.getHeaders().containsKey(entityIdHeader)) {
            return getEntityIdType(retrieveHeader(properties, entityIdHeader), retrieveHeader(properties, entityParameterIdHeader));

        } else {
            return getClassIdType(retrieveHeader(properties, classIdHeader));
        }
    }

    private JavaType getEntityIdType(String entityId, String entityParameterId) {
        return rabbitEntitiesProvider.getJavaType(entityId, entityParameterId)
                .orElseThrow(() -> new IllegalArgumentException("Type: " + entityId + " (with parameter: " + entityParameterId +
                        ") is not a valid RabbitMQ entity"));
    }

    private JavaType getClassIdType(String classId) {
        if (getIdClassMapping().containsKey(classId)) {
            return TypeFactory.defaultInstance().constructType(getIdClassMapping().get(classId));
        }

        try {
            return TypeFactory.defaultInstance().constructType(ClassUtils.forName(classId, getClass().getClassLoader()));

        } catch (ClassNotFoundException e) {
            throw new MessageConversionException("failed to resolve class name. Class not found [" + classId + "]", e);
        } catch (LinkageError e) {
            throw new MessageConversionException("failed to resolve class name. Linkage error [" + classId + "]", e);
        }
    }

    private String getEntityIdFieldName() {
        return "__EntityId__";
    }

    private String getContentEntityIdFieldName() {
        return "__ContentEntityId__";
    }

    private String getEntityParameterIdFieldName() {
        return "__EntityParameterId__";
    }

    private String getContentEntityParameterIdFieldName() {
        return "__ContentEntityParameterId__";
    }

    protected void addHeader(MessageProperties properties, String headerName, String headerValue) {
        properties.getHeaders().put(headerName, headerValue);
    }

    private class RabbitEntitiesProvider {

        private Map<String, JavaType> annotatedClasses;

        private Map<String, JavaType> getAnnotatedClassesJavaTypes() {
            if (annotatedClasses == null) {
                Reflections reflections = new Reflections(rabbitProperties.getPackagesToScan(), RABBIT_PACKAGE);
                Set<Class<?>> typesAnnotatedWithRabbitEntity = reflections.getTypesAnnotatedWith(RabbitEntity.class);
                annotatedClasses = typesAnnotatedWithRabbitEntity.stream()
                        .collect(Collectors.toMap(
                                annotatedClass -> annotatedClass.getAnnotation(RabbitEntity.class).value(),
                                annotatedClass -> TypeFactory.defaultInstance().constructType(annotatedClass)));

                Set<Class<?>> typesAnnotatedWithParametrizedRabbitEntity = reflections.getTypesAnnotatedWith(ParametrizedRabbitEntity.class);
                List<JavaType> rabbitEntities = Lists.newArrayList(annotatedClasses.values());
                typesAnnotatedWithParametrizedRabbitEntity.stream()
                        .forEach(parametrizedRabbitEntity -> {
                            String parametrizedRabbitEntityValue = parametrizedRabbitEntity.getAnnotation(ParametrizedRabbitEntity.class).value();
                            rabbitEntities.stream()
                                    .forEach(rabbitEntity -> {
                                        String key = buildComposedKey(parametrizedRabbitEntityValue,
                                                rabbitEntity.getRawClass().getAnnotation(RabbitEntity.class).value());
                                        JavaType value = TypeFactory.defaultInstance().constructParametrizedType(parametrizedRabbitEntity,
                                                parametrizedRabbitEntity, rabbitEntity.getRawClass());
                                        annotatedClasses.put(key, value);
                                    });
                            annotatedClasses.put(parametrizedRabbitEntityValue, TypeFactory.defaultInstance().constructType(parametrizedRabbitEntity));
                        });


                LOG.info("Loaded RabbitMQ entites: {}", annotatedClasses);
            }
            return annotatedClasses;
        }

        private Optional<JavaType> getJavaType(String rabbitEntityId, String parameterRabbitEntityId) {
            if (parameterRabbitEntityId != null) {
                String composedKey = buildComposedKey(rabbitEntityId, parameterRabbitEntityId);
                JavaType javaType = getAnnotatedClassesJavaTypes().get(composedKey);
                if (javaType == null) {
                    LOG.info("ComposedKey: {} not registered as ParameterizedRabbitEntity<RabbitEntity>", composedKey);
                    // return is outside of the 'if' statement

                } else {
                    return Optional.of(javaType);
                }
            }
            return Optional.ofNullable(getAnnotatedClassesJavaTypes().get(rabbitEntityId));
        }

        private String buildComposedKey(String rabbitEntityValue, String parameterRabbitEntityValue) {
            return rabbitEntityValue + ":" + parameterRabbitEntityValue;

        }
    }

}
