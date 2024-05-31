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

public class ConditionVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table symbolTable;
    private static ArrayList<String> booleanOperations = new ArrayList<>(List.of("And", "Not", "LessThan", "True", "False"));

    public ConditionVisitor(Table symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("Condition", this::visitConditions);
    }

    public boolean visitConditions(JmmNode node, List<Report> reports) {
        String operation = node.getChildren().get(0).getKind();
        String methodName = symbolTable.getMethodName(node);
        if (operation.equals("Name")) {
            Symbol var = symbolTable.searchVariable(node.getChildren().get(0).get("name"), methodName);
            if (var == null){
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid condition - variable " + node.getChildren().get(0).get("name") +  " does not exist)");
                reports.add(report);
                return false;
            }
            if (!var.getType().getName().equals("Boolean")) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (Condition is invalid)");
                reports.add(report);
                return false;
            }
        }

        else if (operation.equals("MethodCall")) {
            if (!symbolTable.isMethodValid(node.getChildren().get(0), "Boolean")) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation " + node.getChildren().get(0).get("name") +" (Condition is invalid)");
                reports.add(report);
            }
        }


        else if (!booleanOperations.contains(operation)) {
            //invalid operation
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation (Condition is invalid)");
            reports.add(report);
        }
        return true;
    }
}
