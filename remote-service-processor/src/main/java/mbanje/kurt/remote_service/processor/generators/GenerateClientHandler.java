package mbanje.kurt.remote_service.processor.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import mbanje.kurt.remote_service.processor.Messenger;
import mbanje.kurt.remote_service.processor.ProcessorHelper;
import mbanje.kurt.remote_service.processor.internal.ParameterServer;

import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by kurt on 28 07 2015 .
 */
public class GenerateClientHandler {

    private final Element service;
    private final List<ParameterServer> serverMethods;
    private final String clazz;
    private Element server;
    private ClassName serverInterface;
    public static final String SUFFIX = "ClientHandler";


    public GenerateClientHandler(Element service, List<ParameterServer> serverMethods) {
        this.service = service;
        this.serverMethods = serverMethods;
        this.clazz = service.getSimpleName() + SUFFIX;
    }

    public void generate(Messenger messenger,Filer filer) {
//        messenger.note(null,"servermethods: %d",serverMethods.size());
        server = serverMethods.get(0).variable.clazz;
        serverInterface = ClassName.get((TypeElement)server);
        JavaFile javaFile = JavaFile.builder(ProcessorHelper.PACKAGE, generateClass()).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            messenger.error(service, "failed to generate class for %s: %s", clazz, e.getMessage());
        }
    }


    TypeSpec generateClass()  {
        TypeName weakReferenceType = ParameterizedTypeName.get(ProcessorHelper.WEAK_REFERENCE, serverInterface);
        FieldSpec reference = FieldSpec.builder(weakReferenceType, "reference")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        return TypeSpec.classBuilder(clazz)
                .superclass(ProcessorHelper.HANDLER)
                .addModifiers(PUBLIC)
                .addField(reference)
                .addMethod(getConstructor())
                .addMethod(getMessageHandler())
                .build();
    }

    private MethodSpec getConstructor() {
        return  MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(server.asType()), "reference")
                .addStatement("this.reference = new $T<>(reference)", ProcessorHelper.WEAK_REFERENCE)
                .build();
    }

    private MethodSpec getMessageHandler(){

        StringBuffer buffer = new StringBuffer();
        for(ParameterServer param:serverMethods){
            int[] cases = param.variable.messages;
            for (int i = 0,max=cases.length; i < max; i++) {
                buffer.append("         case ").append(cases[i]).append(":\n")
                        .append("            stub.").append(param.variable.name).append("(");
                for(int index=0;index<param.variable.params.size();index++){
                    ParameterServer parameter = param.variable.params.get(index);
                    if(parameter.parameter) {
                        buffer.append("(")
                        .append(parameter.element.asType())
                        .append(") msg.obj");
                    }else{
                        buffer.append(cases[i]);
                    }
                    if(index != param.variable.params.size()-1){
                        buffer.append(",");
                    }
                }
                buffer.append(");\n");
                buffer.append("            break;\n");
            }
        }

        return MethodSpec.methodBuilder("handleMessage")
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ProcessorHelper.MESSAGE, "msg")
                .addStatement("$T stub = reference.get()",serverInterface)
                .addCode("if (stub != null) {\n" +
                        "        switch (msg.what) {\n" +
                        "         case $T.CONNECT:\n" +
                        "            stub.onBoundServiceConnectionChanged(true);\n" +
                        "            break;\n" +
                        "         case $T.DISCONNECT:\n" +
                        "            stub.onBoundServiceConnectionChanged(false);\n" +
                        "            break;\n" +
                        "         case $T.SHUTDOWN:\n" +
                        "            stub.onBoundServiceConnectionChanged(false);\n" +
                        "            break;\n", ProcessorHelper.ISERVICE_CLIENT, ProcessorHelper.ISERVICE_CLIENT, ProcessorHelper.ISERVICE_CLIENT)
                .addCode(buffer.toString())
                .addCode("         default:\n" +
                        "            super.handleMessage(msg);\n" +
                        "        }\n" +
                        "        removeMessages(msg.what);\n" +
                        " }")
                .build();



    }

}
