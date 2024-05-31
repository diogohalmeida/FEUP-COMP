package visitors.semantic;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class ReturnVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {

    private Table symbolTable;
    private ArrayList<String> operations = new ArrayList<>(List.of("Add", "Sub", "Mul", "Div"));
    private ArrayList<String> booleanOperations = new ArrayList<>(List.of("LessThan", "And", "Not"));

    public ReturnVisitor(Table symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("Return", this::visitReturn);
    }

    public Boolean visitReturn(JmmNode node, List<Report> reports) {
        String methodName = symbolTable.getMethodName(node);
        Type returnType = symbolTable.getReturnType(methodName);
        JmmNode child = node.getChildren().get(0);

        switch (returnType.getName()){

            case "Int":
                if (child.getKind().equals("Num"))
                    return true;
                else if (child.getKind().equals("Name")) {
                    Symbol var = symbolTable.searchVariable(child.get("name"), methodName);
                    if (var == null){
                        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return variable " + child.get("name") +  " does not exist)");
                        reports.add(report);
                        return false;
                    }
                    if (!(var.getType().getName().equals(returnType.getName()) && var.getType().isArray() == returnType.isArray())) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement: " + methodName + "should return " + returnType.getName()));
                        return false;
                    }
                }
                else if (child.getKind().equals("MethodCall")) {
                    Type returnTypeMethod = symbolTable.getReturnType(child.get("name"));
                    if (!returnTypeMethod.getName().equals(returnType.getName()) || returnTypeMethod.isArray() != returnType.isArray()) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement"));
                        return false;
                    }
                }

                else if (operations.contains(child.getKind())) {
                    return true;
                }
                else if (this.operations.contains(child.getKind())){
                    break;
                }
                else if (child.getKind().equals("NewArray") &&  returnType.isArray()){
                    return true;
                }
                else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement"));
                    return false;
                }
                break;

            case "Boolean":

                if (child.getKind().equals("True") || child.getKind().equals("False")) return true;
                else if (child.getKind().equals("Name")) {
                    Symbol var = symbolTable.searchVariable(child.get("name"), methodName);
                    if (var == null){
                        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return variable " + child.get("name") +  " does not exist)");
                        reports.add(report);
                        return false;
                    }
                    if (!(var.getType().getName().equals(returnType.getName()))) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement: " + methodName + "should return " + returnType.getName()));
                        return false;
                    }
                }
                else if (child.getKind().equals("MethodCall")) {
                    Type returnTypeMethod = symbolTable.getReturnType(child.get("name"));
                    if (!returnTypeMethod.getName().equals(returnType.getName()) || returnTypeMethod.isArray() != returnType.isArray()) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement"));
                        return false;
                    }
                }

                else if (booleanOperations.contains(child.getKind())) {
                    return true;
                }

                else if (this.booleanOperations.contains(child.getKind())){
                    break;
                }
                else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement"));
                    return false;
                }
                break;

            default:
                if (child.getKind().equals("Name")) {
                    Symbol var = symbolTable.searchVariable(child.get("name"), methodName);
                    if (var == null){
                        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return variable " + child.get("name") +  " does not exist)");
                        reports.add(report);
                        return false;
                    }
                    if (!(var.getType().getName().equals(returnType.getName()))) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement: " + methodName + " should return " + returnType.getName()));
                        return false;
                    }
                }
                else if (child.getKind().equals("MethodCall")) {
                    Type returnTypeMethod = symbolTable.getReturnType(child.get("name"));
                    if (returnTypeMethod == null){
                        return true;
                    }
                    if (!returnTypeMethod.getName().equals(returnType.getName()) || returnTypeMethod.isArray() != returnType.isArray()) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement"));
                        return false;
                    }
                }
                else if (child.getKind().equals("NewInstance")){
                    if (!this.symbolTable.getImports().contains(child.get("name")) && !this.symbolTable.getClassName().equals(child.get("name"))){
                        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement - class not found: " + child.get("name"));
                        reports.add(report);
                        return false;
                    }
                }
                else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid return statement"));
                    return false;
                }
                break;
        }
        return true;
    }
}
