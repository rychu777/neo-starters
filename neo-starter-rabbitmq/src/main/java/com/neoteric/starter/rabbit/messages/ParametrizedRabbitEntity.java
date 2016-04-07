package com.neoteric.starter.rabbit.messages;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ParametrizedRabbitEntity {

    /**
     * Entity name
     */
    String value();
}
