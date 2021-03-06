package mbanje.kurt.remote_service.processor.generators;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import mbanje.kurt.remote_service.processor.Messenger;
import mbanje.kurt.remote_service.processor.ClassHelper;
import mbanje.kurt.remote_service.processor.internal.ParameterClient;
import mbanje.kurt.remote_service.processor.internal.ParameterServer;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Created by kurt on 28 07 2015 .
 */
public class ClientGenerator {

    private final Element service;
    private final List<ParameterClient> clientMethods;
    private final List<ParameterServer> serverMethods;
    private final String clazz,packageName;
    private Element client,server;
    private ClassName clientInterface,serverInterface;
    private Messenger messenger;

    public ClientGenerator(String packageName,Element service, List<ParameterClient> clientMethods, List<ParameterServer> serverMethods) {
        this.service = service;
        this.packageName = packageName;
        this.clientMethods = clientMethods;
        this.serverMethods = serverMethods;
        this.clazz = service.getSimpleName() + "Client";
    }

    public void generate(Messenger messenger,Filer filer) {
        client = clientMethods.get(0).variable.clazz;
        server = serverMethods.get(0).variable.clazz;
        clientInterface = ClassName.get((TypeElement)client);
        serverInterface = ClassName.get((TypeElement)server);
        this.messenger = messenger;
        JavaFile javaFile = JavaFile.builder(packageName, generateClass()).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            messenger.error(service, "failed to generate class for %s: %s", clazz, e.getMessage());
        }
    }


    TypeSpec generateClass()  {

        FieldSpec activity = FieldSpec.builder(ClassHelper.ACTIVITY, "parent")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        FieldSpec messenger = FieldSpec.builder(ClassHelper.MESSENGER, "messenger")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        FieldSpec serviceMessenger = FieldSpec.builder(ClassHelper.MESSENGER, "serviceMessenger")
                .addModifiers(Modifier.PRIVATE, STATIC)
                .initializer("null")
                .build();

        FieldSpec tag = FieldSpec.builder(String.class, "TAG")
                .addModifiers(Modifier.PRIVATE)
                .initializer(clazz + ".class.getSimpleName();")
                .build();

        FieldSpec connection = FieldSpec.builder(ClassHelper.SERVICECONNECTION, "connection")
                .addModifiers(Modifier.PRIVATE)
                .initializer("new $T() {\n" +
                                " public void onServiceConnected($T className, $T service) {\n" +
                                "           serviceMessenger = new $T(service);\n" +
                                "           bound=true;\n" +
                                "           Log.d(TAG, \"service connected\");\n" +
                                "           send($T.obtain(null, $T.CONNECT));\n" +
                                "}\n" +
                                " \n" +
                                " public void onServiceDisconnected($T className) {\n" +
                                "          serviceMessenger = null;\n" +
                                "          $T.d(TAG, \"service disconnected\");\n" +
                                "}\n" +
                                "};", ClassHelper.SERVICECONNECTION, ClassHelper.COMPONENT, ClassHelper.IBINDER,
                        ClassHelper.MESSENGER, ClassHelper.MESSAGE, clientInterface, ClassHelper.COMPONENT, ClassHelper.LOG

                )
                .build();



        TypeSpec.Builder builder = TypeSpec.classBuilder(clazz)
                .addSuperinterface(clientInterface)
                .addModifiers(PUBLIC, FINAL)
                .addField(tag)
                .addField(activity)
                .addField(messenger)
                .addField(serviceMessenger)
                .addField(boolean.class, "bound", PRIVATE)
                .addField(connection)
                .addMethod(getConstructor())
                .addMethod(getStub())
                .addMethod(getDisconnect())
                .addMethod(getConnect())
                .addMethod(getSendMsg());

        for(MethodSpec spec:getMethods()){
            builder.addMethod(spec);
        }

        return builder.build();

    }

    private MethodSpec getConstructor() {
        ClassName clientHandlerClass = ClassName.get(packageName, service.getSimpleName() + ClientHandlerGenerator.SUFFIX);
        return  MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassHelper.ACTIVITY, "activity")
                .addParameter(serverInterface, "stub")
                .addStatement("this.parent = activity")
                .addStatement("messenger = new $T(new $T(stub))", ClassHelper.MESSENGER, clientHandlerClass)
                .build();
    }



    private MethodSpec getStub(){
        ClassName returnType = ClassName.get(packageName,clazz);
        return MethodSpec.methodBuilder("createStub")
                .returns(returnType)
                .addModifiers(Modifier.PUBLIC, STATIC)
                .addParameter(ClassHelper.ACTIVITY, "parent")
                .addParameter(serverInterface, "stub")
                .addStatement("return new $T(parent,stub)",returnType)
                .build();
    }


    private List<MethodSpec> getMethods(){
        List<MethodSpec> methods = new ArrayList<>();
        for(ParameterClient method:clientMethods){
            MethodSpec.Builder builder = MethodSpec.methodBuilder(method.variable.name);
            builder.addModifiers(PUBLIC);
            builder.returns(void.class);
            int paramCount = method.variable.params.size();
            String paramname = null;
            for(int index=0;index<paramCount;index++){
                ParameterClient parameter = method.variable.params.get(index);
                paramname = parameter.element.getSimpleName().toString();
                ParameterSpec arg = ParameterSpec.builder(ClassName.get(parameter.element.asType()), paramname)
                        .build();
                builder.addParameter(arg);
            }
            builder.addStatement("send($T.obtain(null,$L,$L))", ClassHelper.MESSAGE,method.variable.message,paramname);
            methods.add(builder.build());

        }
        return methods;
    }


    private MethodSpec getConnect(){
        ClassName serviceClass = ClassName.get((TypeElement)service);
        return MethodSpec.methodBuilder("connect")
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addCode("if (!bound) {\n" +
                                "   //if(!$T.serviceRunning) {\n" +
                                "     //  parent.startService(new $T(parent.getApplicationContext(),$T.class));\n" +
                                "   //}\n" +
                                "   parent.bindService(new $T(parent,$T.class),connection,$T.BIND_AUTO_CREATE);\n" +
                                "   $T.d(TAG,\"connecting..\");\n" +
                                "}else{\n" +
                                "   $T.d(TAG,\"not bound not connecting..\");\n" +
                                "}"
                        , serviceClass, ClassHelper.INTENT, serviceClass, ClassHelper.INTENT,
                        serviceClass, ClassHelper.CONTEXT, ClassHelper.LOG, ClassHelper.LOG
                )
                .build();
    }

    private MethodSpec getDisconnect(){
        return MethodSpec.methodBuilder("disconnect")
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addCode("if (bound) {\n" +
                        "   if(serviceMessenger != null) {\n" +
                        "send($T.obtain(null,$T.DISCONNECT));\n" +
                        "}\n" +
                        "parent.unbindService(connection);\n" +
                        "bound = false;\n" +
                        "$T.d(TAG,\"disconnecting..\");\n" +
                        "}", ClassHelper.MESSAGE,clientInterface, ClassHelper.LOG)
                .build();
    }

    private MethodSpec getSendMsg() {
        ClassName remoteExceptionClass = ClassName.get("android.os","RemoteException");
        return MethodSpec.methodBuilder("send")
                .returns(boolean.class)
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassHelper.MESSAGE, "message")
                .addCode(" if(bound) {\n" +
                        "     try {\n" +
                        "       message.replyTo = messenger;\n" +
                        "       serviceMessenger.send(message);\n" +
                        "       return true;\n" +
                        "     }catch ($T e) {\n" +
                        "       $T.e(TAG,\"error sending message: \",e);" +
                        "     }\n" +
                        "  }\n" +
                        " return false;\n"
                        , remoteExceptionClass, ClassHelper.LOG)
                .build();
    }
}
