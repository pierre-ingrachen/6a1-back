package com.takima.backskeleton.controllers;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {})
@Min(value = SeasonYear.MINIMUM, message = SeasonYear.MESSAGE)
@Max(value = SeasonYear.MAXIMUM, message = SeasonYear.MESSAGE)
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface SeasonYear {
    int MINIMUM = 1900;
    int MAXIMUM = 2100;
    String MESSAGE = "season must be between 1900 and 2100";

    String message() default MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
