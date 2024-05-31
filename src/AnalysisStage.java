
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import table.Method;
import table.Table;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import visitors.semantic.*;
import visitors.table.*;

public class AnalysisStage implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = parserResult.getRootNode();

        List<Report> reports = new ArrayList<>();
        Table symbolTable = new Table();
        ImportVisitor importVisitor = new ImportVisitor(symbolTable);
        ClassNameVisitor classNameVisitor = new ClassNameVisitor(symbolTable);
        ExtendedClassVisitor extendedClassVisitor = new ExtendedClassVisitor(symbolTable);
        FieldsVisitor fieldsVisitor = new FieldsVisitor(symbolTable);
        MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
        OperationsVisitor operationsVisitor = new OperationsVisitor(symbolTable);
        AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
        ConditionVisitor conditionVisitor = new ConditionVisitor(symbolTable);
        ReturnVisitor returnVisitor = new ReturnVisitor(symbolTable);
        NewInstanceVisitor newInstanceVisitor = new NewInstanceVisitor(symbolTable);

        importVisitor.visit(node, reports);
        classNameVisitor.visit(node, reports);
        extendedClassVisitor.visit(node, reports);
        fieldsVisitor.visit(node, reports);
        methodVisitor.visit(node, reports);

        newInstanceVisitor.visit(node, reports);
        returnVisitor.visit(node, reports);
        operationsVisitor.visit(node, reports);
        assignmentVisitor.visit(node, reports);
        conditionVisitor.visit(node, reports);


        for (JmmNode methodNode: symbolTable.getMethodNodes()){
            Method method = symbolTable.nodeToMethod(methodNode);
            MethodCallVisitor methodCallVisitor = new MethodCallVisitor(symbolTable, method);
            methodCallVisitor.visit(methodNode, reports);
        }


        /*
        System.out.println("Dump tree with Visitor where you control tree traversal");
        ExampleVisitor visitor = new ExampleVisitor("Identifier", "id");
        System.out.println(visitor.visit(node, ""));

        System.out.println("Dump tree with Visitor that automatically performs preorder tree traversal");
        var preOrderVisitor = new ExamplePreorderVisitor("Identifier", "id");
        System.out.println(preOrderVisitor.visit(node, ""));

        System.out.println(
                "Create histogram of node kinds with Visitor that automatically performs postorder tree traversal");
        var postOrderVisitor = new ExamplePostorderVisitor();
        var kindCount = new HashMap<String, Integer>();
        postOrderVisitor.visit(node, kindCount);
        System.out.println("Kinds count: " + kindCount + "\n");

        System.out.println(
                "Print variables name and line, and their corresponding parent with Visitor that automatically performs preorder tree traversal");
        var varPrinter = new ExamplePrintVariables("Variable", "name", "line");
        varPrinter.visit(node, null);
         */

        // No Symbol Table being calculated yet
        return new JmmSemanticsResult(parserResult, symbolTable, reports);

    }

}