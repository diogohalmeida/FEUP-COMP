package visitors.table;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class ExtendedClassVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table symbolTable;

    public ExtendedClassVisitor(Table symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("ExtendedClassName", this::visitExtendedClasses);
    }

    public Boolean visitExtendedClasses(JmmNode node, List<Report> reports){
        System.out.println("    Extends Class " + node);
        if (!symbolTable.getImports().contains(node.get("name"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Class not found: " + node.get("name")));
        }
        symbolTable.setSuperClass(node.get("name"));
        return true;
    }
}
