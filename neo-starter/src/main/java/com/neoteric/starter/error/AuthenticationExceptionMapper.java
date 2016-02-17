package com.neoteric.starter.error;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

public class AuthenticationExceptionMapper extends AbstractExceptionMapper<AuthenticationException> {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationExceptionMapper.class);

    @Override
    protected HttpStatus httpStatus() {
        return HttpStatus.FORBIDDEN;
    }

    @Override
    protected Logger logger() {
        return LOG;
    }

    @Override
    protected Level logLevel() {
        return Level.ERROR;
    }

    @Override
    protected Object message(AuthenticationException exception) {
        return exception.getMessage();
    }
}
