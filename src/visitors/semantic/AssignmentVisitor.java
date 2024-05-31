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

public class AssignmentVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table symbolTable;
    private static ArrayList<String> arithmeticOperations = new ArrayList<>(List.of("Add", "Sub", "Mul", "Div"));
    private static ArrayList<String> booleanOperations = new ArrayList<>(List.of("And", "Not", "LessThan"));

    public AssignmentVisitor(Table symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("Assignment", this::visitAssignments);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("ArraySize", this::visitArraySize);

    }


    public boolean visitAssignments(JmmNode node, List<Report> reports) {
        String methodName = symbolTable.getMethodName(node);
        Symbol var = symbolTable.searchVariable(node.getChildren().get(0).get("name"), methodName);
        if (var == null){
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (variable " + node.getChildren().get(0).get("name") +  " does not exist)");
            reports.add(report);
            return false;
        }
        if (node.getChildren().get(1).getKind().equals("Name")) {
            //check variable assignment
            Symbol assignee = symbolTable.searchVariable(node.getChildren().get(1).get("name"), methodName);
            if (assignee == null){
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (variable " + node.getChildren().get(1).get("name") +  " does not exist)");
                reports.add(report);
                return false;
            }
            if (!assignee.getType().getName().equals(var.getType().getName())) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment: " + var.getName() + " and " + assignee.getName() + " have different types");
                reports.add(report);
            }
        }
        else if (node.getChildren().get(1).getKind().equals("Num") || node.getChildren().get(1).getKind().equals("True") || node.getChildren().get(1).getKind().equals("False")){
            if (!((node.getChildren().get(1).getKind().equals("Num") && var.getType().getName().equals("Int")) || (node.getChildren().get(1).getKind().equals("False") || node.getChildren().get(1).getKind().equals("True")) && var.getType().getName().equals("Boolean"))){
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment: " + var.getName() + " and " +node.getChildren().get(1).getKind() + " have different types");
                reports.add(report);
                return false;
            }
        }

        String operation = node.getChildren().get(1).getKind();
        if (arithmeticOperations.contains(operation) && !var.getType().getName().equals("Int")) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (variable " + var.getName() + " is not an integer)");
            reports.add(report);
            return false;
        }

        else if (booleanOperations.contains(operation) && !var.getType().getName().equals("Boolean")) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (variable " + var.getName() + " is not a boolean)");
            reports.add(report);
            return false;
        }

        else if (operation.equals("NewArray") && !var.getType().isArray()) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (variable " + var.getName() + " is not an array)");
            reports.add(report);
            return false;
        }
        
        else if (node.getChildren().get(1).getKind().equals("MethodCall")) {
            JmmNode child = node.getChildren().get(1).getChildren().get(0);

            if (!child.getKind().equals("This")) return true;
            if (!var.getType().getName().equals(symbolTable.getReturnType(node.getChildren().get(1).get("name")).getName())) {

                System.out.println(symbolTable.getReturnType(node.getChildren().get(1).get("name")).getName());
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (method " + node.getChildren().get(1).get("name") + " is not a " + var.getType().getName() + ")");
                reports.add(report);
                return false;
            }

        }

        else if (node.getChildren().get(1).getKind().equals("NewInstance")) {
            JmmNode assignee = node.getChildren().get(0);
            Symbol variable = symbolTable.searchVariable(assignee.get("name"), symbolTable.getMethodName(node));
            if (variable == null){
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (variable " + assignee.get("name") +  " does not exist)");
                reports.add(report);
                return false;
            }
            if (node.getChildren().get(1).get("name").equals(symbolTable.getClassName())) {

                if (!variable.getType().getName().equals(symbolTable.getClassName()) && !variable.getType().getName().equals(symbolTable.getSuper())) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (variable " + variable.getName() + " is not a " + symbolTable.getClassName() + ")");
                    reports.add(report);
                    return false;
                }
            }
            else if (!variable.getType().getName().equals(node.getChildren().get(1).get("name"))) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid assignment (variable " + variable.getName() + " is not a " + node.getChildren().get(1).get("name") + ")");
                reports.add(report);
                return false;
            }

        }


        return true;
    }


    public boolean visitArrayAccess(JmmNode node, List<Report> reports) {
        JmmNode childNode = node.getChildren().get(0);
        String methodName = symbolTable.getMethodName(node);
        if (!childNode.getKind().equals("Name") && !childNode.getKind().equals("Num") && !childNode.getKind().equals("ArrayAccess")) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid array access");
            reports.add(report);
            return false;
        }

        Symbol var = symbolTable.searchVariable(childNode.get("name"), methodName);
        if (var == null) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid array access (variable " + childNode.get("name") + " is not defined)");
            reports.add(report);
            return false;
        }

        if (!var.getType().isArray()) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid array access (variable " + var.getName() + " is not an array)");
            reports.add(report);
            return false;
        }

        return true;
    }

    public boolean visitArraySize(JmmNode node, List<Report> reports) {
        String methodName = symbolTable.getMethodName(node);
        JmmNode childNode = node.getChildren().get(0);
        switch (childNode.getKind()) {
            case "Num":
            case "ArraySize":
                break;

            case "Name":
                Symbol var = symbolTable.searchVariable(childNode.get("name"), methodName);
                if (var == null) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid array access (variable " + childNode.get("name") + " is not defined)");
                    reports.add(report);
                    return false;
                }

                if (!var.getType().getName().equals("Int")) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid array access (variable " + var.getName() + " is not an integer)");
                    reports.add(report);
                    return false;
                }
                break;


            default:
                if (!childNode.getKind().equals("Num") && !childNode.getKind().equals("ArraySize")) break;
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid array access");
                reports.add(report);
                break;
        }
        return true;
    }
}
