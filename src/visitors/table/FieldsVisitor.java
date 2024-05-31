package visitors.table;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class FieldsVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table symbolTable;

    public FieldsVisitor(Table symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("VarDeclaration", this::visitFields);
    }

    public Boolean visitFields(JmmNode node, List<Report> reports){
        if (!node.getParent().getKind().equals("ClassDeclaration")) return false;
        boolean isArray = node.getChildren().get(0).getNumChildren() != 0;
        Type type = new Type(node.getChildren().get(0).getKind(), isArray);
        if (type.getName().equals("Type")) type = new Type(node.getChildren().get(0).get("name"), isArray);
        Symbol symbol = new Symbol(type, node.getChildren().get(1).get("name"));
        if (symbolTable.containsField(symbol)) {
            //field already exists
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Field already declared: " + symbol.getName());
            reports.add(report);
            return false;
        }
        System.out.println("    Fields " + node.getChildren().get(1));
        symbolTable.getFields().add(symbol);
        return true;
    }
}
