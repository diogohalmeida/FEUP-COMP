package visitors.table;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class ClassNameVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table symbolTable;

    public ClassNameVisitor(Table symbolTable){
        this.symbolTable = symbolTable;
        addVisit("ClassName", this::visitClasses);
    }

    public Boolean visitClasses(JmmNode node, List<Report> reports){
        System.out.println("Class " + node);
        symbolTable.setClassName(node.get("name"));
        return true;
    }
}
