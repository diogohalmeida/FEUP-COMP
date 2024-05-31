package visitors.table;

import pt.up.fe.comp.jmm.JmmNode;
import table.Method;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class MethodVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table symbolTable;

    public MethodVisitor(Table symbolTable){
        this.symbolTable = symbolTable;
        addVisit("MethodDeclaration", this::visitMethods);
    }

    public Boolean visitMethods(JmmNode node, List<Report> reports){
        System.out.println("    Method " + node);

        Method newMethod = this.symbolTable.nodeToMethod(node);

        LocalVariablesVisitor localVariablesVisitor = new LocalVariablesVisitor(symbolTable, newMethod);
        localVariablesVisitor.visit(node, reports);

        newMethod.setLocalVariables(localVariablesVisitor.getLocalVariables());

        if (symbolTable.getMethodObjects().contains(newMethod)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Method already defined: " + newMethod.getName()));
            System.err.println("Method already defined: " + newMethod.getName());
            return false;
        }
        else{
            symbolTable.getMethodObjects().add(newMethod);
            symbolTable.getMethodNodes().add(node);
        }
        return true;
    }
}
