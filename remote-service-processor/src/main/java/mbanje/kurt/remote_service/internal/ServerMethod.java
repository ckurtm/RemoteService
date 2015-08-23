package mbanje.kurt.remote_service.internal;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;

/**
 * Created by kurt on 29 07 2015 .
 */
public class ServerMethod {

    public String service;
    public Element clazz;
    public String name;
    public List<ParameterServer> params = new ArrayList<ParameterServer>();
    public int[] messages;

    public ServerMethod(String service, Element clazz, String name, int[] messages) {
        this.service = service;
        this.name = name;
        this.clazz = clazz;
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "ClientMethod{" +
                "service='" + service + '\'' +
                ", clazz='" + clazz + '\'' +
                ", name='" + name + '\'' +
                ", message=" + messages +
                ", params=" + params +
                '}';
    }
}
