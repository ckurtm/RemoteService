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
import mbanje.kurt.remote_service.processor.internal.ParameterClient;

import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by kurt on 28 07 2015 .
 */
public class GenerateServerHandler {

    private final Element service;
    private final List<ParameterClient> clientMethods;
    private final String clazz;
    private Element client;
    private ClassName clientInterface;
    private Messenger messenger;
    public static final String SUFFIX = "ServerHandler";

    public GenerateServerHandler(Element service, List<ParameterClient> clientMethods) {
        this.service = service;
        this.clientMethods = clientMethods;
        this.clazz = service.getSimpleName() + SUFFIX;
    }

    public void generate(Messenger messenger,Filer filer) {
        client = clientMethods.get(0).variable.clazz;
        clientInterface = ClassName.get((TypeElement)client);
        this.messenger = messenger;
//        messenger.note(service, "client methods %d", clientMethods.size());
        JavaFile javaFile = JavaFile.builder(ProcessorHelper.PACKAGE, generateClass()).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            messenger.error(service, "failed to generate class for %s: %s", clazz, e.getMessage());
        }
    }


    TypeSpec generateClass()  {
        TypeName weakReferenceType = ParameterizedTypeName.get(ProcessorHelper.WEAK_REFERENCE, clientInterface);
        FieldSpec reference = FieldSpec.builder(weakReferenceType, "reference")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        TypeName clientsType = ParameterizedTypeName.get(ProcessorHelper.ARRAY_LIST, ProcessorHelper.MESSENGER);
        FieldSpec clients = FieldSpec.builder(clientsType, "clients")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", ProcessorHelper.ARRAY_LIST)
                .build();

        return TypeSpec.classBuilder(clazz)
                .superclass(ProcessorHelper.HANDLER)
                .addModifiers(PUBLIC)
                .addField(reference)
                .addField(clients)
                .addMethod(getConstructor())
                .addMethod(getMessageHandler())
                .addMethod(getSendMsg())
                .build();
    }

    private MethodSpec getConstructor() {
        return  MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(client.asType()), "reference")
                .addStatement("this.reference = new $T<>(reference)", ProcessorHelper.WEAK_REFERENCE)
                .build();
    }

    private MethodSpec getMessageHandler(){

        StringBuffer buffer = new StringBuffer();
        for(ParameterClient param:clientMethods){
//                  messenger.note(service, "%s", param.variable.name);

            buffer.append("         case ").append(param.variable.message).append(":\n")
                    .append("            service.").append(param.variable.name).append("(");
            for(int index=0;index<param.variable.params.size();index++){
                ParameterClient parameter = param.variable.params.get(index);
                if(parameter != null && parameter.parameter) {
                    buffer.append("(")
                            .append(parameter.element.asType())
                            .append(") msg.obj");
                }else{
                    buffer.append(param.variable.message);
                }
                if(index != param.variable.params.size()-1){
                    buffer.append(",");
                }
            }
            buffer.append(");\n");
            buffer.append("            break;\n");

        }

        return MethodSpec.methodBuilder("handleMessage")
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ProcessorHelper.MESSAGE, "msg")
                .addStatement("$T service = reference.get()", clientInterface)
                .addCode("if (service != null) {\n" +
                                "        switch (msg.what) {\n" +
                                "         case $T.CONNECT:\n" +
                                "            clients.add(msg.replyTo);\n" +
                                "            $L message  = $L.obtain(null, $T.DISCONNECT);\n" +
                                "            sendMsg(message);\n" +
                                "            break;\n" +
                                "         case $T.DISCONNECT:\n" +
                                "            clients.remove(msg.replyTo);\n" +
                                "            $L message1  = $L.obtain(null, $T.CONNECT);\n" +
                                "            sendMsg(message1);\n" +
                                "            break;\n" +
                                "         case $T.SHUTDOWN:\n" +
                                "            (($T)service).stopSelf();\n" +
                                "            break;\n", ProcessorHelper.ISERVICE_CLIENT, ProcessorHelper.MESSAGE, ProcessorHelper.MESSAGE,
                        ProcessorHelper.ISERVICE_CLIENT, ProcessorHelper.ISERVICE_CLIENT, ProcessorHelper.MESSAGE, ProcessorHelper.MESSAGE,
                        ProcessorHelper.ISERVICE_CLIENT, ProcessorHelper.ISERVICE_CLIENT, ProcessorHelper.SERVICE)
                .addCode(buffer.toString())
                .addCode("         default:\n" +
                        "            super.handleMessage(msg);\n" +
                        "        }\n" +
                        "        removeMessages(msg.what);\n" +
                        " }")
                .build();
    }


    private MethodSpec getSendMsg() {
        ClassName remoteExceptionClass = ClassName.get("java.lang","Exception");
        return MethodSpec.methodBuilder("sendMsg")
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ProcessorHelper.MESSAGE, "message")
                .beginControlFlow("for (int i = clients.size()-1; i >0; i--)")
                .addCode(" try {\n" +
                        "     clients.get(i).send(message);\n" +
                        " } catch ($T e) {\n" +
                        "      //clients.remove(i);\n" +
                        "}",remoteExceptionClass)
                .endControlFlow()
                .build();
    }
}
