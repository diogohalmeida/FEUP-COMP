package visitors.semantic;

import pt.up.fe.comp.jmm.JmmNode;
import table.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class MethodCallVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table symbolTable;
    private Method parentMethod;
    private static ArrayList<String> arithmeticOperations = new ArrayList<>(List.of("Add", "Sub", "Mul", "Div"));
    private static ArrayList<String> booleanOperations = new ArrayList<>(List.of("And", "Not", "LessThan"));

    public MethodCallVisitor(Table symbolTable, Method parentMethod){
        this.symbolTable = symbolTable;
        this.parentMethod = parentMethod;
        addVisit("MethodCall", this::visitMethodCalls);
    }

    public Boolean visitMethodCalls(JmmNode node, List<Report> reports) {
        System.out.println("        Method Call " + node.get("name"));

        Method completeParent = null;
        for (Method method : this.symbolTable.getMethodObjects()) {
            if (method.equals(parentMethod)) {
                completeParent = method;
            }
        }


        if (node.getChildren().get(0).getKind().equals("This") || node.getChildren().get(0).get("name").equals(symbolTable.getClassName())) {
            List<Method> candidateMethods = new ArrayList<>();
            for (Method method : this.symbolTable.getMethodObjects()) {
                if (method.getName().equals(node.get("name")) && method.getParameters().size() == node.getChildren().get(1).getChildren().size()) {
                    candidateMethods.add(method);
                }
            }
            if (!candidateMethods.isEmpty()) {
                for (Method method : candidateMethods) {
                    int correct = 0;
                    int index = 0;
                    boolean passed = false;
                    List<Symbol> searchList = new ArrayList<>();
                    searchList.addAll(completeParent.getLocalVariables());
                    searchList.addAll(completeParent.getParameters());
                    searchList.addAll(this.symbolTable.getFields());
                    for (JmmNode argNode : node.getChildren().get(1).getChildren()) {
                        if (argNode.getKind().equals("Name")) {
                            Symbol result = this.symbolTable.searchVariable(argNode.get("name"), completeParent.getName());
                            if (result == null){
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Undeclared variables in Method Call: " + node.get("name")));
                                System.err.println("Undeclared variables in Method Call: " + node.get("name"));
                                return false;
                            }
                            if (result.getType().equals(method.getParameters().get(index).getType())){
                                index++;
                                correct++;
                            }
                        } else if ((argNode.getKind().equals("Num") && method.getParameters().get(index).getType().getName().equals("Int")) || ((argNode.getKind().equals("False") || argNode.getKind().equals("True")) && method.getParameters().get(index).getType().getName().equals("Boolean"))) {
                            index++;
                            correct++;
                        }
                        else if(argNode.getKind().equals("NewInstance") && method.getParameters().get(index).getType().getName().equals(argNode.get("name"))){
                            index++;
                            correct++;
                        }
                        else if (arithmeticOperations.contains(argNode.getKind()) && method.getParameters().get(index).getType().getName().equals("Int")) {
                            index++;
                            correct++;
                        }

                        else if (booleanOperations.contains(argNode.getKind()) && method.getParameters().get(index).getType().getName().equals("Boolean")) {
                            index++;
                            correct++;
                        }

                        else if (argNode.getKind().equals("MethodCall")) {
                            Type returnType = symbolTable.getReturnType(argNode.get("name"));
                            if (returnType == null){
                                index++;
                                correct++;
                            }
                            else{
                                if (returnType.getName().equals(method.getParameters().get(index).getType().getName()) && returnType.isArray() == method.getParameters().get(index).getType().isArray()) {
                                    index++;
                                    correct++;
                                }
                            }

                        }
                        else if (argNode.getKind().equals("ArrayAccess") && method.getParameters().get(index).getType().getName().equals("Int") ){
                            index++;
                            correct++;
                        }
                        else if (argNode.getKind().equals("Length") && method.getParameters().get(index).getType().getName().equals("Int") ){
                            index++;
                            correct++;
                        } else if (argNode.getKind().equals("NewArray") && method.getParameters().get(index).getType().getName().equals("Int") && method.getParameters().get(index).getType().isArray()) {
                            index++;
                            correct++;
                        }
                    }
                    if (correct == method.getParameters().size()) {
                        return true;
                    }
                }
                if (this.symbolTable.getSuper() == null) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Method not found or with wrong arguments: this." + node.get("name")));
                    System.err.println("Method not found or with wrong arguments: this." + node.get("name"));
                    return false;
                }
            } else {
                //Verify if a superclass exists if a method isn't declared inside the class
                for (Method method : this.symbolTable.getMethodObjects()) {
                    if (method.getName().equals(node.get("name"))) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Wrong arguments for method: this." + node.get("name")));
                        System.err.println("Wrong arguments for method: this." + node.get("name"));
                        return false;
                    }
                }
                if (this.symbolTable.getSuper() == null) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Method not found: this." + node.get("name")));
                    System.err.println("Method not found: this." + node.get("name"));
                    return false;
                }
            }
        } else {

            //verify is variable exists
            Symbol var = symbolTable.searchVariable(node.getChildren().get(0).get("name"), symbolTable.getMethodName(node));
            if (var != null) return true;
            
            //Verify if imports exist
            if (!symbolTable.getImports().contains(node.getChildren().get(0).get("name")) && !node.getChildren().get(0).get("name").equals(symbolTable.getClassName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Class not found: " + node.getChildren().get(0).get("name")));
                System.err.println("Class not found: " + node.getChildren().get(0).get("name"));
                return false;
            }
        }
        return true;
    }
}
