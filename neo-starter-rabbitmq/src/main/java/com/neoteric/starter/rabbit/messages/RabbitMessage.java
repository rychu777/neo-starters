package com.neoteric.starter.rabbit.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.MoreObjects;

import java.util.Objects;

@ParametrizedRabbitEntity("rabbitMessage")
public class RabbitMessage<T> implements Parametrized {

    public static final String TYPE = "type";
    public static final String APP_CODE = "appCode";
    public static final String SOURCE_OBJECT = "sourceObject";

    @JsonProperty(TYPE)
    String type;

    @JsonProperty(APP_CODE)
    String appCode;

    @JsonProperty(SOURCE_OBJECT)
    T sourceObject;

    int cachedHashCode;

    @JsonCreator
    public RabbitMessage(@JsonProperty(TYPE) String type, @JsonProperty(APP_CODE) String appCode, @JsonProperty(SOURCE_OBJECT) T sourceObject) {
        this.type = type;
        this.appCode = appCode;
        this.sourceObject = sourceObject;
        this.cachedHashCode = Objects.hash(type, appCode, sourceObject);
    }

    public String getType() {
        return type;
    }

    public String getAppCode() {
        return appCode;
    }

    public Object getSourceObject() {
        return sourceObject;
    }

    public static <T> RabbitMessageBuilder<T> builder() {
        return new RabbitMessageBuilder<T>();
    }

    @Override
    @JsonIgnore
    public Class getParameterType() {
        return sourceObject == null ? null : sourceObject.getClass();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class RabbitMessageBuilder<T> {

        @JsonProperty(TYPE)
        String type;

        @JsonProperty(APP_CODE)
        String appCode;

        @JsonProperty(SOURCE_OBJECT)
        T sourceObject;

        public RabbitMessageBuilder<T> type(String type) {
            this.type = type;
            return this;
        }

        public RabbitMessageBuilder<T> appCode(String appCode) {
            this.appCode = appCode;
            return this;
        }

        public RabbitMessageBuilder<T> sourceObject(T sourceObject) {
            this.sourceObject = sourceObject;
            return this;
        }

        public RabbitMessage<T> build() {
            return new RabbitMessage<T>(type, appCode, sourceObject);
        }
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof RabbitMessage) {
            RabbitMessage<?> other = (RabbitMessage<?>) obj;
            return Objects.equals(this.appCode, other.appCode) &&
                    Objects.equals(this.type, other.type) &&
                    Objects.equals(this.sourceObject, other.sourceObject);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add(APP_CODE, appCode)
                .add(TYPE, type)
                .add(SOURCE_OBJECT, sourceObject)
                .toString();
    }

}