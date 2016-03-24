package com.neoteric.starter.jersey;

import com.neoteric.starter.StarterConstants;
import com.neoteric.starter.exception.mapper.*;
import com.neoteric.starter.jersey.time.ZonedDateTimeConverterProvider;
import com.neoteric.starter.jersey.validation.ValidationConfigurationProvider;
import com.neoteric.starter.swagger.SwaggerProperties;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jersey.JerseyProperties;
import org.springframework.util.ClassUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;

public abstract class AbstractJerseyConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJerseyConfig.class);

    @Autowired
    protected SwaggerProperties swaggerProperties;

    @Autowired
    protected NeoStarterJerseyProperties starterJerseyProperties;

    @Autowired
    protected JerseyProperties jerseyProperties;

    /**
     * Register application specific Jersey resources.
     */
    protected abstract void configure();

    @PostConstruct // In constructor we can't inject properties
    public void register() {
        logRegister(MultiPartFeature.class);
        logRegister(ObjectMapperProvider.class);
        logRegister(ZonedDateTimeConverterProvider.class);
        if (starterJerseyProperties.isLogEndpointsOnStartup()) {
            logRegister(new EndpointLoggingListener(jerseyProperties.getApplicationPath()));
        }
        registerExceptionMappers();

        logRegister(ValidationConfigurationProvider.class);
        String[] packagesToScan = starterJerseyProperties.getPackagesToScan();
        if (packagesToScan != null && packagesToScan.length > 0) {
            logPackages(starterJerseyProperties.getPackagesToScan());
        }

        if (swaggerProperties.isEnabled()) {
            logPackages(StarterConstants.SWAGGER_PACKAGE);
        }
        configure();
    }

    private void registerExceptionMappers() {
        logRegister(ResourceNotFoundExceptionMapper.class);
        logRegister(ConstraintViolationExceptionMapper.class);
        logRegister(GlobalExceptionMapper.class);
        logRegister(QueryParamExceptionMapper.class);

        if (isClassPresent("org.springframework.security.access.AccessDeniedException")) {
            logRegister(AccessDeniedExceptionMapper.class);
        }

        if (isClassPresent("org.springframework.security.core.AuthenticationException")) {
            logRegister(AuthenticationExceptionMapper.class);
        }
    }

    private boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, ClassUtils.getDefaultClassLoader());
    }

    private void logRegister(final Class<?> componentClass) {
        LOG.debug("{} Jersey registers {}", StarterConstants.LOG_PREFIX, componentClass.getName());
        register(componentClass);
    }

    private void logRegister(final Object component) {
        LOG.debug("{} Jersey registers {}", StarterConstants.LOG_PREFIX, component.getClass().getName());
        register(component);
    }

    private void logPackages(String... packages) {
        LOG.debug("{} Jersey registers packages {}", StarterConstants.LOG_PREFIX, Arrays.toString(packages));
        packages(packages);
    }
}