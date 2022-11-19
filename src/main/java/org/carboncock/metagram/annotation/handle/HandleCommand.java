package org.carboncock.metagram.annotation.handle;

import org.carboncock.metagram.annotation.types.StringSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Pavel Sharaev (mail@pechhenka.ru)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleCommand {
    String value();

    StringSelector selector() default StringSelector.EQUALS;
}
