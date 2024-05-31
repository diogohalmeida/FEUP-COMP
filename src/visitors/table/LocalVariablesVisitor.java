package visitors.table;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import table.Method;
import table.Table;

import java.util.ArrayList;
import java.util.List;

public class LocalVariablesVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private List<Symbol> localVariables = new ArrayList<>();
    private Table symbolTable;
    private Method parentMethod;

    public LocalVariablesVisitor(Table symbolTable, Method parentMethod) {
        this.parentMethod = parentMethod;
        this.symbolTable = symbolTable;
        addVisit("VarDeclaration", this::visitLocalVariables);
    }

    public Boolean visitLocalVariables(JmmNode node, List<Report> reports){
        boolean isArray = node.getChildren().get(0).getNumChildren() != 0;
        Type type = new Type(node.getChildren().get(0).getKind(), isArray);
        if (type.getName().equals("Type")) type = new Type(node.getChildren().get(0).get("name"), isArray);

        List<Symbol> searchList = new ArrayList<>();
        searchList.addAll(localVariables);
        searchList.addAll(this.parentMethod.getParameters());

        for (Symbol localVariable: searchList){
            if (localVariable.getName().equals(node.getChildren().get(1).get("name"))){
                //System.out.println("Variable already declared: " + node.getChildren().get(1).get("name"));
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Local variable already declared: " + node.getChildren().get(1).get("name"));
                reports.add(report);
                return false;
            }
        }
        if (!type.getName().equals("Int") && !type.getName().equals("Boolean") && !type.getName().equals(symbolTable.getClassName()) && !symbolTable.getImports().contains(type.getName())){
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Class not found: " + type.getName());
            reports.add(report);
            return false;
        }

        localVariables.add(new Symbol(type, node.getChildren().get(1).get("name")));
        System.out.println("        Local Variable " + node.getChildren().get(1));
        return true;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }
}
