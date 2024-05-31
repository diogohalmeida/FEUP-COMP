package visitors.semantic;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class OperationsVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {

    private Table symbolTable;
    private ArrayList<String> operations = new ArrayList<>(List.of("Add", "Sub", "Mul", "Div"));
    private ArrayList<String> booleanOperations = new ArrayList<>(List.of("LessThan", "And"));
    public OperationsVisitor(Table symbolTable){
        this.symbolTable = symbolTable;
        addVisit("Add", this::visitOperations);
        addVisit("Sub", this::visitOperations);
        addVisit("Mul", this::visitOperations);
        addVisit("Div", this::visitOperations);
        addVisit("LessThan", this::visitOperations);
        addVisit("Not", this::visitNotOperation);
        addVisit("And", this::visitBooleanOperations);


    }


    public Boolean visitOperations(JmmNode node, List<Report> reports) {
        //TODO: reports are getting added twice sometimes

        String methodName = symbolTable.getMethodName(node);
        for (JmmNode jmmNode: node.getChildren()) {
            System.out.println("node: " + node + "; iteration: " + jmmNode);

            if (jmmNode.getKind().equals("Num")) continue;

            else if (operations.contains(jmmNode.getKind())) visitOperations(jmmNode, reports);

            else if (jmmNode.getKind().equals("Name")){
                //check symbol table to see the variable type
                Symbol var = symbolTable.searchVariable(jmmNode.get("name"), methodName);
                if (var == null) {
                    //variable not defined
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (variable " + jmmNode.get("name") + " is not defined)");
                    reports.add(report);
                    return false;
                }
                if ((!var.getType().getName().equals("Int") && !var.getType().getName().equals("Length")) || var.getType().isArray()) {
                    System.out.println(var);
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (variable " + jmmNode.get("name") + " is not an integer)");
                    reports.add(report);
                    return false;
                }
            }

            else if (jmmNode.getKind().equals("MethodCall")) {
                if (jmmNode.getChildren().get(0).getKind().equals("This") && !symbolTable.getReturnType(jmmNode.get("name")).getName().equals("Int")) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (" + jmmNode.get("name") + " does not return an integer)");
                    reports.add(report);
                    return false;
                }
            }

            else if (!jmmNode.getKind().equals("Length") && !jmmNode.getKind().equals("ArrayAccess")){
                //invalid type
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (" + jmmNode.getKind() + " is not an integer)");
                reports.add(report);
                return false;
            }
        }
        return true;
    }

    public Boolean visitNotOperation(JmmNode node, List<Report> reports) {
        String method = symbolTable.getMethodName(node);
        if (node.getChildren().get(0).getKind().equals("True") || node.getChildren().get(0).getKind().equals("False")) return true;
        JmmNode operand = node.getChildren().get(0);
        Symbol var = null;
        if (operand.getKind().equals("Name")){
            var = symbolTable.searchVariable(operand.get("name"), method);

            if (var == null) {
                //variable not defined
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (variable " + operand.get("name") + " is not defined)");
                reports.add(report);
                return false;
            }
            if (!var.getType().getName().equals("Boolean")) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (variable " + operand.get("name") + " is not a boolean)");
                reports.add(report);
                return false;
            }

            else if (operand.getKind().equals("MethodCall")) {
                if (operand.getChildren().get(0).getKind().equals("This") && !symbolTable.getReturnType(operand.get("name")).getName().equals("Int")) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (" + operand.get("name") + " does not return an integer)");
                    reports.add(report);
                    return false;
                }
            }

            else if (booleanOperations.contains(var.getType().getName())) visitBooleanOperations(operand, reports);

        }


        return true;
    }

    public boolean visitBooleanOperations(JmmNode node, List<Report> reports) {
        String methodName = symbolTable.getMethodName(node);
        for (JmmNode jmmNode: node.getChildren()) {
            if (jmmNode.getKind().equals("True") || jmmNode.getKind().equals("False")) continue;
            //else if (booleanOperations.contains(jmmNode.getKind())) visitBooleanOperations(jmmNode, reports);

            if (jmmNode.getKind().equals("Name")){
                //check symbol table to see the variable type
                Symbol var = symbolTable.searchVariable(jmmNode.get("name"), methodName);
                if (var == null) {
                    //variable not defined
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (variable " + jmmNode.get("name") + " is not defined)");
                    reports.add(report);
                    return false;
                }
                if (!var.getType().getName().equals("Boolean")) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (variable " + jmmNode.get("name") + " is not a boolean)");
                    reports.add(report);
                    return false;
                }
            }
        }
        return true;
    }

}
