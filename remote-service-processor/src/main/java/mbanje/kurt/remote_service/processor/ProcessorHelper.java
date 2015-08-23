package mbanje.kurt.remote_service.processor;

import com.squareup.javapoet.ClassName;
import mbanje.kurt.remote_service.IServiceClient;
import mbanje.kurt.remote_service.IServiceServer;
import mbanje.kurt.remote_service.RemoteServiceClient;
import mbanje.kurt.remote_service.RemoteServiceServer;
import mbanje.kurt.remote_service.internal.UnnamedPackageException;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;



import static javax.lang.model.element.ElementKind.INTERFACE;


/**
 * Created by kurt on 05 07 2015 .
 */
public class ProcessorHelper {
    public final String PARCELABLE = "android.os.Parcelable";
    public final String SERVICE_CLASS = "android.app.Service";
    public static final String SERVICE_CLIENT_INTERFACE = IServiceClient.class.getCanonicalName();
    public static final String SERVICE_SERVER_INTERFACE = IServiceServer.class.getCanonicalName();

    public static final String PACKAGE = "mbanje.kurt.remote_service";

    public static final ClassName MESSAGE = ClassName.get("android.os", "Message");
    public static final ClassName WEAK_REFERENCE = ClassName.get("java.lang.ref", "WeakReference");
    public static final ClassName ISERVICE_CLIENT = ClassName.get(PACKAGE, "IServiceClient");
    public static final ClassName HANDLER = ClassName.get("android.os", "Handler");
    public static final ClassName ARRAY_LIST = ClassName.get("java.util", "ArrayList");
    public static final ClassName SERVICE = ClassName.get("android.app", "Service");
    public static final ClassName ISERVICE_SERVER = ClassName.get(PACKAGE, "IServiceServer");
    public static final ClassName MESSENGER = ClassName.get("android.os", "Messenger");
    public static final ClassName ACTIVITY = ClassName.get("android.app", "Activity");
    public static final ClassName COMPONENT = ClassName.get("android.content","ComponentName");
    public static final ClassName CONTEXT = ClassName.get("android.content","Context");
    public static final ClassName INTENT = ClassName.get("android.content","Intent");
    public static final ClassName SERVICECONNECTION = ClassName.get("android.content","ServiceConnection");
    public static final ClassName IBINDER = ClassName.get("android.os","IBinder");
    public static final ClassName LOG = ClassName.get("android.util","Log");

    boolean isPublic(TypeElement annotatedClass) {
        return annotatedClass.getModifiers().contains(Modifier.PUBLIC);
    }

    public boolean isInterface(TypeMirror typeMirror) {
        if (!(typeMirror instanceof DeclaredType)) {
            return false;
        }
        return ((DeclaredType) typeMirror).asElement().getKind() == INTERFACE;
    }

    public boolean isValidParameter(TypeMirror element){
        if(element.getKind() == TypeKind.ARRAY){
            ArrayType arrayType = (ArrayType) element;
            return isValidParameter(arrayType.getComponentType());
        }
        return element.getKind().isPrimitive() || isSubtypeOfType(element, PARCELABLE)
                || element.toString().equals(Number.class.getCanonicalName())
                || element.toString().equals(String.class.getCanonicalName())
                || element.toString().equals(Integer.class.getCanonicalName())
                || element.toString().equals(Long.class.getCanonicalName())
                || element.toString().equals(Short.class.getCanonicalName())
                || element.toString().equals(Float.class.getCanonicalName())
                || element.toString().equals(Double.class.getCanonicalName()
        );

    }

    public boolean isValidService(TypeMirror element){
        return isSubtypeOfType(element,SERVICE_CLASS);
    }

    public boolean isValidServiceClientClass(TypeMirror element){
        return isSubtypeOfType(element,SERVICE_CLIENT_INTERFACE);
    }

    public boolean isValidServiceServerClass(TypeMirror element){
        return isSubtypeOfType(element,SERVICE_SERVER_INTERFACE);
    }

    public boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (otherType.equals(typeMirror.toString())) {
            return true;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }


    public String getPackageName(Elements elementUtils, TypeElement type) throws UnnamedPackageException {
        PackageElement pkg = elementUtils.getPackageOf(type);
        if (pkg.isUnnamed()) {
            throw new UnnamedPackageException(type);
        }
        return pkg.getQualifiedName().toString();
    }


    public boolean isValidClass(TypeElement annotatedClass) {
        return isPublic(annotatedClass);
    }

    public TypeMirror clientValue(RemoteServiceClient annotation) {
        try {
            annotation.value();
        }catch( MirroredTypeException mte ) {
            return mte.getTypeMirror();
        }
        return null;
    }

    public TypeMirror serverValue(RemoteServiceServer annotation) {
        try {
            annotation.value();
        }catch( MirroredTypeException mte ) {
            return mte.getTypeMirror();
        }
        return null;
    }


}
