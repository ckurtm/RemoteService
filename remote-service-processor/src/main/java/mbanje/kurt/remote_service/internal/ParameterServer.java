package mbanje.kurt.remote_service.internal;

import javax.lang.model.element.VariableElement;

/**
 * Created by kurt on 29 07 2015 .
 */
public class ParameterServer {
    public ServerMethod variable;
    public VariableElement element;
    public boolean parameter;

    public ParameterServer(ServerMethod variable, VariableElement element, boolean parameter) {
        this.variable = variable;
        this.parameter = parameter;
        this.element = element;
    }
}
