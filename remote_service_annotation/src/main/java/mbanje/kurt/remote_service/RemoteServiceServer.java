package mbanje.kurt.remote_service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by kurt on 26 07 2015 .
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface RemoteServiceServer {
    Class<?> value();
}
