package mbanje.kurt.remote_service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by kurt on 2015/07/02.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface RemoteService {
    RemoteServiceType value() default RemoteServiceType.STARTED_BOUND;
}