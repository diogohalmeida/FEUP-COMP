package table;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import table.Method;


import java.util.ArrayList;
import java.util.List;

public class Table implements SymbolTable {
    private List<String> imports = new ArrayList<>();
    private List<List<String>> fullImports = new ArrayList<>();
    private String className;
    private String superClass;
    private List<Symbol> fields = new ArrayList<>();
    private List<Method> methods = new ArrayList<>();
    private List<JmmNode> methodNodes = new ArrayList<>();

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        List<String> result = new ArrayList<>();
        for (Method method: methods){
            result.add(method.getName());
        }
        return result;
    }

    @Override
    public Type getReturnType(String methodName) {
        for (Method method: methods){
            if (method.getName().equals(methodName)){
                return method.getReturnType();
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        for (Method method: methods){
            if (method.getName().equals(methodName)){
                return method.getParameters();
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        for (Method method: methods){
            if (method.getName().equals(methodName)){
                return method.getLocalVariables();
            }
        }
        return null;
    }

    public List<JmmNode> getMethodNodes() {
        return methodNodes;
    }

    public List<Method> getMethodObjects(){
        return this.methods;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setFields(List<Symbol> fields) {
        this.fields = fields;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public List<List<String>> getFullImports() {
        return fullImports;
    }

    public Method nodeToMethod(JmmNode node){
        String name = node.get("name");

        boolean isArray;
        Type returnType;
        if (name.equals("main")){
            returnType = new Type("void", false);
        }
        else{
            isArray = node.getChildren().get(0).getNumChildren() != 0;
            if (node.getChildren().get(0).getKind().equals("Type")){
                returnType = new Type(node.getChildren().get(0).get("name"), isArray);
            }
            else{
                returnType = new Type(node.getChildren().get(0).getKind(), isArray);
            }

        }


        List<Symbol> parameters = new ArrayList<>();
        if (name.equals("main")){
            parameters.add(new Symbol(new Type("String", true), "args"));
        }
        else{
            for (JmmNode parameter: node.getChildren().get(1).getChildren()){
                isArray = parameter.getChildren().get(0).getNumChildren() != 0;
                if (parameter.getChildren().get(0).getKind().equals("Type")){
                    parameters.add(new Symbol(new Type(parameter.getChildren().get(0).get("name"), isArray), parameter.get("name")));
                }
                else{
                    parameters.add(new Symbol(new Type(parameter.getChildren().get(0).getKind(), isArray), parameter.get("name")));
                }
            }
        }
        return new Method(name, returnType, parameters, new ArrayList<>());
    }

    public boolean containsField(Symbol field) {
        for (Symbol symbol: fields) {
            if (symbol.getName().equals(field.getName())) return true;
        }
        return false;
    }

    public String getMethodName(JmmNode node) { //TODO: avoid repetition of this code segment
        //search for method declaration
        JmmNode current = node;
        boolean methodFound = false;
        while (!methodFound) {
            current = current.getParent();
            if (current.getKind().equals("MethodDeclaration")) methodFound = true;
        }
        return current.get("name");
    }

    public Symbol searchVariable(String varName, String methodName) {
        List<Symbol> searchList = new ArrayList<>();
        searchList.addAll(getLocalVariables(methodName));
        searchList.addAll(getParameters(methodName));
        searchList.addAll(fields);

        for (Symbol symbol: searchList) {
            if (symbol.getName().equals(varName)) {
                return symbol;
            }
        }
        return null;
    }

    public boolean isMethodValid(JmmNode methodNode, String expectedReturnType) {
        JmmNode child1 = methodNode.getChildren().get(0);

        if (child1.getKind().equals("This") && superClass == null){
            return getReturnType(methodNode.get("name")).getName().equals(expectedReturnType);
        }

        else if (child1.getKind().equals("This") && superClass != null) return true;


        String name = child1.get("name");
        if (child1.getKind().equals("Name") && (!imports.contains(name) && !getLocalVariables(methodNode.get("name")).contains(name) && fields.contains(name))) {
            return false;
        }
        return true;
    }

    public int isArgument(String varName, String methodName){
        int count = 1;
        for (Symbol arg: this.getParameters(methodName)){
            if (arg.getName().equals(varName)){
                return count;
            }
            count++;
        }
        return 0;
    }


    public Method getMethodByNode(JmmNode node){
        for (Method method: methods){
            if (method.getName().equals(node.get("name"))){
                return method;
            }
        }
        return null;
    }

    public JmmNode getNodeByMethod(Method method){
        for (JmmNode node: methodNodes){
            if (method.getName().equals(node.get("name"))){
                return node;
            }
        }
        return null;
    }

    public boolean isField(String varName){
        for (Symbol field: fields){
            if (field.getName().equals(varName)){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Table{" +
                "imports=" + imports + "\n" +
                ", fullImports=" + fullImports +
                ", className='" + className + '\'' + "\n" +
                ", superClass='" + superClass + '\'' + "\n" +
                ", fields=" + fields + "\n" +
                ", methods=" + methods + "\n" +
                ", methodNodes=" + methodNodes + "\n" +
                '}';
    }
}
