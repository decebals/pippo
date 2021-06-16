package ro.pippo.core;

import java.lang.reflect.Type;

public class SimpleTypeReference {

    private Class<?> rawType;

    private Type parameterizedType;

    public SimpleTypeReference(Class<?> rawType, Type parameterizedType) {
        super();
        this.rawType = rawType;
        this.parameterizedType = parameterizedType;
    }

    public String getTypeName() {
        return parameterizedType.getTypeName();
    }

    public Class<?> getRawType() {
        return rawType;
    }

    public Type getParameterizedType() {
        return parameterizedType;
    }

}
