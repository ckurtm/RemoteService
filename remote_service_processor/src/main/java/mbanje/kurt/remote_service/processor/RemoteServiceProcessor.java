package mbanje.kurt.remote_service.processor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import mbanje.kurt.remote_service.RemoteMessageClient;
import mbanje.kurt.remote_service.RemoteMessageServer;
import mbanje.kurt.remote_service.RemoteService;
import mbanje.kurt.remote_service.RemoteServiceClient;
import mbanje.kurt.remote_service.RemoteServiceServer;
import mbanje.kurt.remote_service.processor.generators.GenerateClient;
import mbanje.kurt.remote_service.processor.generators.GenerateClientHandler;
import mbanje.kurt.remote_service.processor.generators.GenerateServerHandler;
import mbanje.kurt.remote_service.processor.internal.ClientMethod;
import mbanje.kurt.remote_service.processor.internal.ParameterClient;
import mbanje.kurt.remote_service.processor.internal.ParameterServer;
import mbanje.kurt.remote_service.processor.internal.ServerMethod;

public class RemoteServiceProcessor extends AbstractProcessor {

    private final Messenger messenger = new Messenger();
    private ProcessorHelper helper;
    private Filer filer;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messenger.init(processingEnv);
        helper = new ProcessorHelper();
        filer = processingEnv.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(RemoteService.class.getCanonicalName());
        annotations.add(RemoteServiceClient.class.getCanonicalName());
        annotations.add(RemoteServiceServer.class.getCanonicalName());
        annotations.add(RemoteMessageServer.class.getCanonicalName());
        annotations.add(RemoteMessageClient.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        List<Element> services = new ArrayList<>();
        Map<String,List<ParameterClient>> clients = new HashMap<>();
        Map<String,List<ParameterServer>> servers = new HashMap<>();

        parseServices(environment, services);
        parseClients(environment, clients);
        parseServers(environment, servers);

        for(Element service:services){
            final String servicename = service.getSimpleName().toString();

            GenerateClientHandler serviceClientGenerator = new GenerateClientHandler(service,servers.get(servicename));
            serviceClientGenerator.generate(messenger, filer);

            GenerateServerHandler serviceServerGenerator = new GenerateServerHandler(service,clients.get(servicename));
            serviceServerGenerator.generate(messenger, filer);

            GenerateClient clientGenerator = new GenerateClient(service,clients.get(servicename),servers.get(servicename));
            clientGenerator.generate(messenger, filer);
        }
        return true;
    }

    private void parseServices(RoundEnvironment environment,List<Element> services){
        for (Element element : environment.getElementsAnnotatedWith(RemoteService.class)) {
//            messenger.note(element,"parseServices %s",element);
            if(!helper.isValidService(element.asType())){
                messenger.error(element, "@RemoteService can only be used on subclasses of Service class");
            }else {
                services.add(element);
            }
        }
    }

    private void parseClients(RoundEnvironment environment, Map<String, List<ParameterClient>> clients){
        for (Element element : environment.getElementsAnnotatedWith(RemoteServiceClient.class)) {
//            messenger.note(element,"parseClients %s",element);
            final RemoteServiceClient clientAnnotation = element.getAnnotation(RemoteServiceClient.class);
            final TypeMirror client = helper.clientValue(clientAnnotation);
            final String service = ((DeclaredType) client).asElement().getSimpleName().toString();
//            String clazz = ((TypeElement) element).getQualifiedName().toString();
            List<ParameterClient> methods = new ArrayList<>();

            if(!helper.isInterface(element.asType())){
                messenger.error(element, "%s can only be applied on interfaces, class %s is not an interface", clientAnnotation,element.getSimpleName());
            }

            if(!helper.isValidServiceClientClass(element.asType())){
                messenger.error(element, "%s: should extend from  %s", element.getClass().getCanonicalName(), ProcessorHelper.SERVICE_CLIENT_INTERFACE);
            }

            for(Element methodElement:element.getEnclosedElements()) {
                final RemoteMessageClient messageAnnotation = methodElement.getAnnotation(RemoteMessageClient.class);
                final ExecutableElement executable = (ExecutableElement) methodElement;
                List<? extends VariableElement> parameters = executable.getParameters();
                TypeMirror returnType = executable.getReturnType();

                if(returnType.getKind() != TypeKind.VOID){
                    messenger.error(methodElement, "%s method should return void", methodElement.getSimpleName());
                }

                ClientMethod method = new ClientMethod(service,element,methodElement.getSimpleName().toString(),messageAnnotation.value());
                ParameterClient parameterClient = null;
                if(parameters.size() == 0){
                    parameterClient = new ParameterClient(method,null,true);
                }
                for (int i = 0; i < parameters.size(); i++) {
                    VariableElement param = parameters.get(i);
//                    messenger.note(param,"compare %s -> %s",param.asType().toString(),String.class.getCanonicalName());
                    if(!helper.isValidParameter(param.asType())) {
                        messenger.error(param, "%s cannot be used, parameters should be primitives or implement Parcelable", param.asType());
                    }else{
                        parameterClient = new ParameterClient(method,param,true);
                        method.params.add(parameterClient);
                    }
                }
//                messenger.note(methodElement, "%s -> %s",service, method.toString());
                if (parameterClient != null) {
                    methods.add(parameterClient);
                }
            }
            clients.put(service, methods);
        }
    }

    private void parseServers(RoundEnvironment environment, Map<String, List<ParameterServer>> servers){
        for (Element element : environment.getElementsAnnotatedWith(RemoteServiceServer.class)) {
//            messenger.note(element,"parseServers %s",element);
            final RemoteServiceServer serverAnnotation = element.getAnnotation(RemoteServiceServer.class);
            final TypeMirror server = helper.serverValue(serverAnnotation);
            final String service = ((DeclaredType) server).asElement().getSimpleName().toString();
//            String clazz = ((TypeElement) element).getQualifiedName().toString();
            List<ParameterServer> methods = new ArrayList<>();

            if(!helper.isInterface(element.asType())){
                messenger.error(element, "%s can only be applied on interfaces, class %s is not an interface", serverAnnotation,element.getSimpleName());
            }

            if(!helper.isValidServiceServerClass(element.asType())){
                messenger.error(element, "%s: should extend from  %s", element.getClass().getCanonicalName(), ProcessorHelper.SERVICE_SERVER_INTERFACE);
            }

            for(Element methodElement:element.getEnclosedElements()) {
                final RemoteMessageServer messageAnnotation = methodElement.getAnnotation(RemoteMessageServer.class);
                final ExecutableElement executable = (ExecutableElement) methodElement;
                List<? extends VariableElement> parameters = executable.getParameters();
                TypeMirror returnType = executable.getReturnType();

                if(returnType.getKind() != TypeKind.VOID){
                    messenger.error(methodElement, "%s method should return void", methodElement.getSimpleName());
                }

                ServerMethod method = new ServerMethod(service,element,methodElement.getSimpleName().toString(),messageAnnotation.value());
                ParameterServer paramserver = null;
                if(parameters.size() == 0){
                    paramserver = new ParameterServer(method,null,true);
                }
                for (int i = 0; i < parameters.size(); i++) {
                    VariableElement param = parameters.get(i);
                    if(!helper.isValidParameter(param.asType())) {
                        messenger.error(param, "%s cannot be used, parameters should be primitives or implement Parcelable", param.asType());
                    }else{
                        paramserver = new ParameterServer(method,param,(i == (parameters.size()-1))); //TODO this should change, find a bettr way of passing around variables
                        method.params.add(paramserver);
                    }
                }
//                messenger.note(methodElement, "%s -> %s",service, method.toString());
                if (paramserver != null) {
                    methods.add(paramserver);
                }
            }
            servers.put(service, methods);
        }
    }






}
