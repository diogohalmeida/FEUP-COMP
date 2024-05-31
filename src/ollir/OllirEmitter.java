package ollir;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import table.Method;
import table.Table;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class OllirEmitter extends AJmmVisitor {
    private SymbolTable table;
    private JmmNode root;
    private List<Report> reports;
    private boolean oOptimization;
    private int rOptimization;

    public OllirEmitter(SymbolTable table, JmmNode root, int rOptimization, boolean oOptimization, List<Report> reports) {
        this.table = table;
        this.root = root;
        this.reports = reports;
        this.rOptimization = rOptimization;
        this.oOptimization = oOptimization;
    }

    public String getOllirCode() {
        Table castTable = (Table) table;
        String tab = "    ";
        String nl = "\n";
        StringBuilder Ollir = new StringBuilder("");
        Utils utils = new Utils();
        //Imports
        for (String imp: castTable.getImports()){
            Ollir.append("import ");
            for (List<String> packages: castTable.getFullImports()){
                if (packages.contains(imp)){
                    int count = 0;
                    for (String pack: packages){
                        count++;
                        if (count == packages.size()){
                            Ollir.append(pack);
                        }
                        else{
                            Ollir.append(pack + ".");
                        }
                    }
                }
            }
            Ollir.append(";\n");
        }
        Ollir.append("\n");

        //Class name
        if (castTable.getSuper() != null){
            Ollir.append(castTable.getClassName() + " extends " + castTable.getSuper() + "{" + "\n");
        }
        else{
            Ollir.append(castTable.getClassName() + "{" + "\n");
        }


        //Fields
        for (Symbol field: castTable.getFields()) {
            Ollir.append(tab + ".field private ");
            Ollir.append(utils.symbolToString(field) + ";" + "\n");
        }

        //Constructor
        Ollir.append(nl + tab + ".construct " + castTable.getClassName() + "().V {" + nl + tab + tab +  "invokespecial(this, \"<init>\").V;" + nl + tab + "}" + nl + nl);

        //Method
        for (Method method: castTable.getMethodObjects()) {
            if (method.getName().equals("main")){
                Ollir.append(tab + ".method public static " + method.getName() + utils.methodParametersToString(method) + "{" + nl);
            }
            else{
                Ollir.append(tab + ".method public " + Utils.sanitize(method.getName()) + utils.methodParametersToString(method) + "{" + nl);
            }

            JmmNode methodNode = castTable.getNodeByMethod(method);
            OllirExpressions expressionVisitor = new OllirExpressions(method, Ollir, castTable, methodNode, rOptimization, oOptimization);
            if (method.getName().equals("main")){
                for (JmmNode child: methodNode.getChildren().get(1).getChildren()){
                    expressionVisitor.visitExpressions(child, this.reports);
                }
                Ollir.append(tab + tab + "ret.V;" + nl);
            }
            else{
                for (JmmNode child: methodNode.getChildren().get(2).getChildren()){
                    expressionVisitor.visitExpressions(child, this.reports);
                }
            }

            Ollir.append(tab + "}" + nl + nl);
        }



        Ollir.append("}");
        String result = Ollir.toString();
        return result;
    }




}
