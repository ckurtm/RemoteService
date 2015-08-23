package mbanje.kurt.remote_service.internal;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;

/**
 * Created by kurt on 29 07 2015 .
 */
public class ClientMethod {

    public String service;
    public Element clazz;
    public String name;
    public int message;
    public List<ParameterClient> params = new ArrayList<ParameterClient>();

    public ClientMethod(String service,Element clazz,String name, int message) {
        this.service = service;
        this.name = name;
        this.message = message;
        this.clazz = clazz;
    }

    @Override
    public String toString() {
        return "ClientMethod{" +
                "service='" + service + '\'' +
                ", clazz='" + clazz + '\'' +
                ", name='" + name + '\'' +
                ", message=" + message +
                ", params=" + params +
                '}';
    }
}
