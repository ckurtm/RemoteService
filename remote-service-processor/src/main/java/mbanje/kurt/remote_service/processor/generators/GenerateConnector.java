package mbanje.kurt.remote_service.processor.generators;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
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

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by kurt on 28 07 2015 .
 */
public class GenerateConnector {

    private final Element service;
    private final List<ParameterClient> clientMethods;
    private final String clazz;
    private Element client,server;
    private ClassName clientInterface,serverInterface;
    private Messenger messenger;

    public GenerateConnector(Element service, List<ParameterClient> clientMethods) {
        this.service = service;
        serverInterface = ClassName.get(ProcessorHelper.PACKAGE, service.getSimpleName() + "ServerHandler");
        this.clientMethods = clientMethods;
        this.clazz = service.getSimpleName() + "Connector";
    }

    public void generate(Messenger messenger,Filer filer) {
        client = clientMethods.get(0).variable.clazz;
        clientInterface = ClassName.get((TypeElement)client);
        this.messenger = messenger;
        JavaFile javaFile = JavaFile.builder(ProcessorHelper.PACKAGE, generateClass()).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            messenger.error(service, "failed to generate class for %s: %s", clazz, e.getMessage());
        }
    }


    TypeSpec generateClass()  {

        FieldSpec handler = FieldSpec.builder(serverInterface, "handler")
                .addModifiers(Modifier.PRIVATE)
                .build();

        FieldSpec messenger = FieldSpec.builder(ProcessorHelper.MESSENGER, "messenger")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();


        TypeSpec.Builder builder = TypeSpec.classBuilder(clazz)
                .addModifiers(PUBLIC, FINAL)
                .addField(handler)
                .addField(messenger)
                .addMethod(getConstructor())
                .addMethod(getBinder())
                .addMethod(getPost());


        return builder.build();

    }

    private MethodSpec getConstructor() {
        ClassName clientHandlerClass = ClassName.get(ProcessorHelper.PACKAGE, service.getSimpleName() + GenerateClientHandler.SUFFIX);

        return  MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(clientInterface, "client")
                .addStatement("handler = new $T(client)", serverInterface)
                .addStatement("messenger = new $T(handler)", ProcessorHelper.MESSENGER)
                .build();
    }



    private MethodSpec getBinder(){
        return MethodSpec.methodBuilder("getBinder")
                .returns(ProcessorHelper.IBINDER)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return messenger.getBinder()")
                .build();
    }

   private MethodSpec getPost() {
        return MethodSpec.methodBuilder("post")
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "id")
                .addParameter(Object.class,"object")
                .addStatement("$T message = $T.obtain(handler,id)",ProcessorHelper.MESSAGE,ProcessorHelper.MESSAGE)
                .addStatement("message.obj = object")
                .addStatement("handler.sendMessage(message)")
                .build();
    }
}
