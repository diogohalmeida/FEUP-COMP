package visitors.table;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class ImportVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table symbolTable;

    public ImportVisitor(Table symbolTable){
        this.symbolTable = symbolTable;
        addVisit("ImportDeclaration", this::visitImports);
    }

    public Boolean visitImports(JmmNode node, List<Report> reports){
        System.out.println("Import " + node);
        for (String str: symbolTable.getImports()){
            if (str.equals(node.get("name"))){
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Duplicated import declaration: " + node.get("name"));
                reports.add(report);
                return false;
            }
        }
        symbolTable.getImports().add(node.get("name"));
        List<String> packs = new ArrayList<>();
        for (JmmNode pack: node.getChildren()){
            packs.add(pack.get("name"));
        }
        symbolTable.getFullImports().add(packs);
        return true;
    }


}
