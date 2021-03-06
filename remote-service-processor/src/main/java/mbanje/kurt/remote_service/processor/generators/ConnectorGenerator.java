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
import mbanje.kurt.remote_service.processor.ClassHelper;
import mbanje.kurt.remote_service.processor.internal.ParameterClient;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by kurt on 28 07 2015 .
 */
public class ConnectorGenerator {

    private final Element service;
    private final List<ParameterClient> clientMethods;
    private final String clazz,packageName;
    private Element client,server;
    private ClassName clientInterface,serverInterface;
    private Messenger messenger;

    public ConnectorGenerator(String packageName,Element service, List<ParameterClient> clientMethods) {
        this.service = service;
        this.packageName = packageName;
        serverInterface = ClassName.get(packageName, service.getSimpleName() + "ServerHandler");
        this.clientMethods = clientMethods;
        this.clazz = service.getSimpleName() + "Connector";
    }

    public void generate(Messenger messenger,Filer filer) {
        client = clientMethods.get(0).variable.clazz;
        clientInterface = ClassName.get((TypeElement)client);
        this.messenger = messenger;
        JavaFile javaFile = JavaFile.builder(packageName, generateClass()).build();
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

        FieldSpec messenger = FieldSpec.builder(ClassHelper.MESSENGER, "messenger")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();


        TypeSpec.Builder builder = TypeSpec.classBuilder(clazz)
                .addModifiers(PUBLIC, FINAL)
                .addField(handler)
                .addField(messenger)
                .addMethod(getConstructor())
                .addMethod(getBinder())
                .addMethod(getSend())
                .addMethod(getClients())
                ;


        return builder.build();

    }

    private MethodSpec getConstructor() {
        ClassName clientHandlerClass = ClassName.get(packageName, service.getSimpleName() + ClientHandlerGenerator.SUFFIX);

        return  MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(clientInterface, "client")
                .addStatement("handler = new $T(client)", serverInterface)
                .addStatement("messenger = new $T(handler)", ClassHelper.MESSENGER)
                .build();
    }



    private MethodSpec getBinder(){
        return MethodSpec.methodBuilder("getBinder")
                .returns(ClassHelper.IBINDER)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return messenger.getBinder()")
                .build();
    }

   private MethodSpec getSend() {
        return MethodSpec.methodBuilder("send")
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "id")
                .addParameter(Object.class, "object")
                .addStatement("$T message = $T.obtain(null,id)", ClassHelper.MESSAGE, ClassHelper.MESSAGE)
                .addStatement("message.obj = object")
                .addStatement("handler.send(message)")
                .build();
    }

    private MethodSpec getClients() {
        TypeName clientsType = ParameterizedTypeName.get(ClassHelper.ARRAY_LIST, ClassHelper.MESSENGER);
        return MethodSpec.methodBuilder("getClients")
                .returns(clientsType)
                .addStatement("return handler.getClients()")
                .build();
    }
}
