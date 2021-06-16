package com.github.jonhgithub.aws.codepipeline.java.example.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = EnumValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface IEnumValidator {

  Class<? extends Enum<?>> enumClazz();

  String message() default "Value is not valid";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
