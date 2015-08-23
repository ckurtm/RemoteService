package mbanje.kurt.remote_service.processor.internal;

import javax.lang.model.element.VariableElement;

/**
 * Created by kurt on 29 07 2015 .
 */
public class ParameterClient {
    public ClientMethod variable;
    public VariableElement element;
    public boolean parameter;

    public ParameterClient(ClientMethod variable, VariableElement element, boolean parameter) {
        this.variable = variable;
        this.parameter = parameter;
        this.element = element;
    }
}
