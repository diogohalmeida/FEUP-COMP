package table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;
import java.util.Objects;

public class Method {
    private final String name;
    private final Type returnType;
    private final List<Symbol> parameters;
    private List<Symbol> localVariables;

    public Method(String name, Type returnType, List<Symbol> parameters, List<Symbol> localVariables){
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.localVariables = localVariables;
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(List<Symbol> localVariables) {
        this.localVariables = localVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Method method = (Method) o;
        if (name.equals(method.name) && returnType.equals(method.returnType) && parameters.size() == method.getParameters().size()){
            int count = 0;
            for (int i = 0; i < parameters.size(); i++){
                if (method.getParameters().get(i).getType().equals(parameters.get(i).getType())){
                    count++;
                }
            }
            return count == parameters.size();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, parameters);
    }

    @Override
    public String toString() {
        return "Method{" +
                "name='" + name + '\'' + "\n" +
                ", returnType=" + returnType + "\n" +
                ", parameters=" + parameters + "\n" +
                ", localVariables=" + localVariables + "\n" +
                '}';
    }
}
