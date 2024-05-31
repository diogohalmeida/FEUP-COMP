package visitors.semantic;

import ollir.Utils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import table.Method;
import table.Table;

import java.util.List;
import java.util.Optional;

public class NewInstanceVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;

    public NewInstanceVisitor(Table table){
        this.table = table;
        addVisit("NewInstance", this::visitNewInstances);
    }

    public Boolean visitNewInstances(JmmNode node, List<Report> reports){
        if (!this.table.getImports().contains(node.get("name")) && !this.table.getClassName().equals(node.get("name"))){
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Class not found: " + node.get("name"));
            reports.add(report);
            return false;
        }
        return true;
    }
}
