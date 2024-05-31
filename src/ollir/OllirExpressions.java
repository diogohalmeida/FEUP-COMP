package ollir;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.report.Report;
import table.Method;
import table.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OllirExpressions {
    private Method parentMethod;
    private JmmNode parentMethodNode;
    private StringBuilder Ollir;
    private ArrayList<String> operations = new ArrayList<>(List.of("Add", "Sub", "Mul", "Div"));
    private ArrayList<String> booleanOperations = new ArrayList<>(List.of("LessThan", "And", "Not"));
    private List<JmmNode> auxOperations = new ArrayList<>();
    private HashMap<JmmNode, String> auxVarOps = new HashMap<>();
    private final Table table;
    private Utils utils = new Utils();
    private String auxName;
    private int count;
    private int index;
    private Symbol assignmentVar;
    private String ident;
    private String tab = "    ";
    private String nl = "\n";
    private List<Report> reports;
    private int ifLabelCount;
    private HashMap<JmmNode, Integer> ifLabelMap = new HashMap<>();
    private int whileLabelCount;
    private HashMap<JmmNode, Integer> whileLabelMap = new HashMap<>();
    private boolean oOptimization;
    private int rOptimization;

    public OllirExpressions(Method parentMethod, StringBuilder Ollir, Table table, JmmNode parentMethodNode, int rOptimization, boolean oOptimization){
        this.parentMethod = parentMethod;
        this.Ollir = Ollir;
        this.table = table;
        this.parentMethodNode = parentMethodNode;
        this.ident = tab + tab;
        this.ifLabelCount = 1;
        this.whileLabelCount = 1;
        this.rOptimization = rOptimization;
        this.oOptimization = oOptimization;
    }

    public void visitExpressions(JmmNode node, List<Report> reports){
        this.reports = reports;
        this.reset();
        switch (node.getKind()) {
            case "Assignment":
                assignmentsHandler(node);
                break;
            case "MethodCall":
                methodCallsHandler(node);
                break;
            case "Return":
                returnsHandler(node);
                break;
            case "ArrayAssignment":
                arrayAssignmentsHandler(node);
                break;
            case "If":
                ifsHandler(node);
                break;
            case "Else":
                elsesHandler(node);
                break;
            case "While":
                if (oOptimization){
                    optimizedWhilesHandler(node);
                }
                else{
                    whilesHandler(node);
                }

                break;
        }
    }

    private void assignmentsHandler(JmmNode node) {
        if (node.getChildren().get(1).getKind().equals("True") || node.getChildren().get(1).getKind().equals("False") || node.getChildren().get(1).getKind().equals("Num")){
            if (table.isField(node.getChildren().get(0).get("name"))){
                Ollir.append(this.ident + "putfield(this, " + Utils.sanitize(node.getChildren().get(0).get("name")) + "." + nodeToTypeString(node.getChildren().get(0)) + ", " + nodeToName(node.getChildren().get(1)) + "." + nodeToTypeString(node.getChildren().get(1)) + ").V;\n");
            }
            else{
                this.Ollir.append(this.ident + Utils.sanitize(node.getChildren().get(0).get("name")) + "." + nodeToTypeString(node.getChildren().get(0)) + " :=." + nodeToTypeString(node.getChildren().get(0)) + " " + nodeToName(node.getChildren().get(1)) + "." + nodeToTypeString(node.getChildren().get(1)) + ";" + nl);
            }
            return;
        }

        if (node.getChildren().get(1).getKind().equals("Name")){
            if (table.isField(node.getChildren().get(1).get("name"))){
                if (table.isField(node.getChildren().get(0).get("name"))){
                    this.Ollir.append(this.ident + "aux1." + nodeToTypeString(node.getChildren().get(0)) + " :=." + nodeToTypeString(node.getChildren().get(0)) + " getfield(this, " + nodeToName(node.getChildren().get(1)) + "." + nodeToTypeString(node.getChildren().get(1)) + ")." + nodeToTypeString(node.getChildren().get(1))  + ";" + nl);
                    Ollir.append(this.ident + "putfield(this, " + Utils.sanitize(node.getChildren().get(0).get("name")) + "." + nodeToTypeString(node.getChildren().get(0)) + ", aux1." + nodeToTypeString(node.getChildren().get(1)) + ").V;\n");
                }
                else{
                    this.Ollir.append(this.ident + Utils.sanitize(node.getChildren().get(0).get("name")) + "." + nodeToTypeString(node.getChildren().get(0)) + " :=." + nodeToTypeString(node.getChildren().get(0)) + " getfield(this, " + nodeToName(node.getChildren().get(1)) + "." + nodeToTypeString(node.getChildren().get(1)) + ")." + nodeToTypeString(node.getChildren().get(1))  + ";" + nl);
                }
            }
            else{
                if (table.isField(node.getChildren().get(0).get("name"))){
                    Ollir.append(this.ident + "putfield(this, " + Utils.sanitize(node.getChildren().get(0).get("name")) + "." + nodeToTypeString(node.getChildren().get(0)) + ", " + nodeToName(node.getChildren().get(1)) + "." + nodeToTypeString(node.getChildren().get(1)) + ").V;\n");
                }
                else{
                    this.Ollir.append(this.ident + Utils.sanitize(node.getChildren().get(0).get("name")) + "." + nodeToTypeString(node.getChildren().get(0)) + " :=." + nodeToTypeString(node.getChildren().get(0)) + " " + nodeToName(node.getChildren().get(1)) + "." + nodeToTypeString(node.getChildren().get(1)) + ";" + nl);
                }
            }
            return;
        }

        assignmentVar = this.table.searchVariable(node.getChildren().get(0).get("name"), parentMethodNode.get("name"));
        this.searchOps(node);
        if (oOptimization){
            count = 1;
        }
        for (index = auxOperations.size() - 1; index >= 0; index--) {
            if (index == 0) {
                if (auxOperations.get(index).getKind().equals("MethodCall")) {
                    if (auxOperations.get(index).getChildren().get(0).getKind().equals("NewInstance")) {
                        auxName = "aux" + count;
                    } else {
                        auxName = getVarName(parentMethodNode.get("name"), assignmentVar.getName());
                    }
                }
                else if (auxOperations.get(index).getKind().equals("Name")){
                    if (table.isField(auxOperations.get(index).get("name"))){
                        Ollir.append(this.ident + "putfield(this, " + Utils.sanitize(auxOperations.get(index).get("name")) + "." + nodeToTypeString(auxOperations.get(index)) + ", " + this.auxVarOps.get(auxOperations.get(1)) + "." + nodeToTypeString(auxOperations.get(1)) + ").V;\n");
                        return;
                    }
                }
                else {
                    auxName = getVarName(parentMethodNode.get("name"), assignmentVar.getName());
                }
            } else {
                auxName = "aux" + count;
            }
            this.processExpressions();
        }
    }

    private void methodCallsHandler(JmmNode node) {
        this.auxOperations.add(node);
        this.searchOps(node.getChildren().get(1));
        if (oOptimization){
            count = 1;
        }
        for (index = auxOperations.size() - 1; index >= 0; index--) {
            if (index == 0) {
                if (auxOperations.get(index).getKind().equals("MethodCall")) {
                    if (auxOperations.get(index).getChildren().get(0).getKind().equals("NewInstance")) {
                        auxName = "aux" + count;
                    }
                }
            } else {
                auxName = "aux" + count;
            }

            if (auxOperations.get(index).getKind().equals("Not")){
                this.doNots();
            }
            else if (operations.contains(auxOperations.get(index).getKind()) || booleanOperations.contains(auxOperations.get(index).getKind())) {
                this.doOperations();
            } else if (auxOperations.get(index).getKind().equals("MethodCall")) {
                String methodString = this.getInvokeString(auxOperations.get(index));
                if (index == 0){
                    this.Ollir.append(this.ident + methodString + ")." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
                    auxVarOps.put(auxOperations.get(index), auxName);
                }
                else{
                    this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + methodString + ")." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
                    auxVarOps.put(auxOperations.get(index), auxName);
                }

            } else if (auxOperations.get(index).getKind().equals("NewInstance")) {
                doNewInstances();
            } else if (auxOperations.get(index).getKind().equals("ArrayAccess")) {
                doArrayAccesses();

            } else if (auxOperations.get(index).getKind().equals("NewArray")) {
                doNewArrays();
            }
            else if (auxOperations.get(index).getKind().equals("Length")){
                doLength();
            }
            else if (auxOperations.get(index).getKind().equals("Name")){
                doNames();
            }
            count++;
        }
    }


    private void returnsHandler(JmmNode node) {
        if (node.getChildren().get(0).getKind().equals("True") || node.getChildren().get(0).getKind().equals("False") || node.getChildren().get(0).getKind().equals("Num")){
            this.Ollir.append(this.ident + "ret." + nodeToTypeString(node.getChildren().get(0)) + " " + nodeToName(node.getChildren().get(0)) + "." + nodeToTypeString(node.getChildren().get(0)) + ";" + nl);
            return;
        }

        if (node.getChildren().get(0).getKind().equals("Name")){
            if (!table.isField(node.getChildren().get(0).get("name"))){
                this.Ollir.append(this.ident + "ret." + nodeToTypeString(node.getChildren().get(0)) + " " + nodeToName(node.getChildren().get(0)) + "." + nodeToTypeString(node.getChildren().get(0)) + ";" + nl);
                return;
            }
        }

        this.auxOperations.add(node);
        this.searchOps(node);
        if (oOptimization){
            count = 1;
        }
        for (index = auxOperations.size() - 1; index >= 0; index--) {
            if (index == 0) {
                if (auxOperations.get(index).getKind().equals("MethodCall")) {
                    if (auxOperations.get(index).getChildren().get(0).getKind().equals("NewInstance")) {
                        auxName = "aux" + count;
                    }
                }
            } else {
                auxName = "aux" + count;
            }

            if (auxOperations.get(index).getKind().equals("Not")){
                this.doNots();
            }
            else if (operations.contains(auxOperations.get(index).getKind()) || booleanOperations.contains(auxOperations.get(index).getKind())) {
                this.doOperations();
            } else if (auxOperations.get(index).getKind().equals("MethodCall")) {
                String methodString = this.getInvokeString(auxOperations.get(index));
                if (index == 0){
                    this.Ollir.append(this.ident + methodString + ")." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
                    auxVarOps.put(auxOperations.get(index), auxName);
                }
                else{
                    this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + methodString + ")." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
                    auxVarOps.put(auxOperations.get(index), auxName);
                }

            } else if (auxOperations.get(index).getKind().equals("NewInstance")) {
                doNewInstances();
            } else if (auxOperations.get(index).getKind().equals("ArrayAccess")) {
                doArrayAccesses();
                continue;

            } else if (auxOperations.get(index).getKind().equals("NewArray")) {
                doNewArrays();
            }
            else if (auxOperations.get(index).getKind().equals("Return")){
                this.Ollir.append(this.ident + "ret." + nodeToTypeString(node.getChildren().get(0)) + " " + this.auxVarOps.get(node.getChildren().get(0)) + "." + nodeToTypeString(node.getChildren().get(0)) + ";" + nl);
                return;
            }
            else if (auxOperations.get(index).getKind().equals("Length")){
                doLength();
            }
            else if (auxOperations.get(index).getKind().equals("Name")){
                doNames();
            }
            count++;
        }
    }


    private void arrayAssignmentsHandler(JmmNode node) {
        if (oOptimization){
            count = 1;
        }
        String leftSide;
        if ((node.getChildren().get(0).getKind().equals("Name"))){
            if (table.isField(node.getChildren().get(0).get("name"))){
                Ollir.append(this.ident + "aux" + count + ".array.i32" + " :=.array.i32 getfield(this, " + nodeToName(node.getChildren().get(0)) + ".array.i32).array.i32;" + nl);
                auxVarOps.put(node.getChildren().get(0), "aux" + count);
                count++;
            }
        }
        //Left side is a number or variable
        if ((node.getChildren().get(1).getKind().equals("Num") || node.getChildren().get(1).getKind().equals("Name"))){
            if (node.getChildren().get(1).getKind().equals("Num")){
                Ollir.append(this.ident + "aux" + count + ".i32 :=.i32 " + nodeToName(node.getChildren().get(1)) + ".i32;" + nl);
                leftSide = this.ident + nodeToName(node.getChildren().get(0)) + "[aux" + count + ".i32].i32 :=.i32 ";
                count++;
            }
            else{
                if (table.isField(node.getChildren().get(1).get("name"))){
                    Ollir.append(this.ident + "aux" + count + ".i32" + " :=.i32 getfield(this, " + nodeToName(node.getChildren().get(1)) + ".i32).i32;" + nl);
                    leftSide = this.ident + nodeToName(node.getChildren().get(0)) + "[aux" + count + ".i32].i32 :=.i32 ";
                    count++;
                }
                else{
                    leftSide = this.ident + nodeToName(node.getChildren().get(0)) + "[" + nodeToName(node.getChildren().get(1)) + ".i32].i32 :=.i32 ";
                }
            }
        }
        //Left side is an expression
        else{
            if (node.getChildren().get(1).getKind().equals("MethodCall")) {
                this.auxOperations.add(node.getChildren().get(1));
                this.searchOps(node.getChildren().get(1).getChildren().get(1));
            }
            else if (node.getChildren().get(1).getKind().equals("ArrayAccess")){
                this.auxOperations.add(node.getChildren().get(1));
                if (table.isField(node.getChildren().get(1).getChildren().get(0).get("name"))){
                    this.auxOperations.add(node.getChildren().get(1).getChildren().get(0));
                }
                this.searchOps(node.getChildren().get(1).getChildren().get(1));
            }
            else {
                this.auxOperations.add(node.getChildren().get(1));
                this.searchOps(node.getChildren().get(1));
            }
            for (index = auxOperations.size() - 1; index >= 0; index--) {
                auxName = "aux" + count;
                processExpressions();
            }
            leftSide = this.ident + nodeToName(node.getChildren().get(0)) + "[" + auxName + ".i32].i32 :=.i32 ";
        }

        //Right side is a number or variable
        if ((node.getChildren().get(2).getKind().equals("Num") || node.getChildren().get(2).getKind().equals("Name"))){

            if (node.getChildren().get(2).getKind().equals("Name")){
                if (table.isField(node.getChildren().get(2).get("name"))){
                    Ollir.append(this.ident + "aux" + count + ".i32" + " :=.i32 getfield(this, " + nodeToName(node.getChildren().get(2)) + ".i32).i32;" + nl);
                    Ollir.append(leftSide + "aux" + count + ".i32;" + nl);
                }
                else{
                    Ollir.append(leftSide + nodeToName(node.getChildren().get(2)) + ".i32;" + nl);
                }
            }
            else{
                Ollir.append(leftSide + nodeToName(node.getChildren().get(2)) + ".i32;" + nl);
            }
            return;
        }
        //Right side is an expression
        else{
            this.reset();
            if (node.getChildren().get(2).getKind().equals("MethodCall")) {
                this.auxOperations.add(node.getChildren().get(2));
                this.searchOps(node.getChildren().get(2).getChildren().get(1));
            }
            else if (node.getChildren().get(2).getKind().equals("ArrayAccess")){
                this.auxOperations.add(node.getChildren().get(2));
                if (table.isField(node.getChildren().get(2).getChildren().get(0).get("name"))){
                    this.auxOperations.add(node.getChildren().get(2).getChildren().get(0));
                }
                this.searchOps(node.getChildren().get(2).getChildren().get(1));
            }
            else {
                this.auxOperations.add(node.getChildren().get(2));
                this.searchOps(node.getChildren().get(2));
            }
            for (index = auxOperations.size() - 1; index >= 0; index--) {
                auxName = "aux" + count;
                processExpressions();
            }
            Ollir.append(leftSide + auxVarOps.get(node.getChildren().get(2)) + ".i32;" + nl);
        }
    }

    private void processExpressions() {
        if (auxOperations.get(index).getKind().equals("Not")){
            this.doNots();
        }
        else if (operations.contains(auxOperations.get(index).getKind()) || booleanOperations.contains(auxOperations.get(index).getKind())) {
            this.doOperations();
        } else if (auxOperations.get(index).getKind().equals("MethodCall")) {
            String methodString = this.getInvokeString(auxOperations.get(index));
            this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + methodString + ")." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
            auxVarOps.put(auxOperations.get(index), auxName);
        } else if (auxOperations.get(index).getKind().equals("NewInstance")) {
            doNewInstances();
        } else if (auxOperations.get(index).getKind().equals("ArrayAccess")) {
            doArrayAccesses();
        } else if (auxOperations.get(index).getKind().equals("NewArray")) {
            doNewArrays();
        } else if (auxOperations.get(index).getKind().equals("Length")){
            doLength();
        }
        else if (auxOperations.get(index).getKind().equals("Name")){
            doNames();
        }
        count++;
    }

    private void ifsHandler(JmmNode node){
        if (node.getChildren().get(0).getChildren().get(0).getKind().equals("True") || node.getChildren().get(0).getChildren().get(0).getKind().equals("False")){
            Ollir.append(this.ident + "if(" + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool ==.bool 0.bool) goto else" + this.ifLabelCount + ";" + nl);
        }
        else if (node.getChildren().get(0).getChildren().get(0).getKind().equals("Name")){
            if (table.isField(node.getChildren().get(0).getChildren().get(0).get("name"))) {
                Ollir.append(this.ident + "aux1.bool :=.i32 getfield(this, " + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool).bool;" + nl);
                Ollir.append(this.ident + "if(aux1.bool ==.bool 0.bool) goto else" + this.ifLabelCount + ";" + nl);
            }
            else{
                Ollir.append(this.ident + "if(" + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool ==.bool 0.bool) goto else" + this.ifLabelCount + ";" + nl);
            }
        }
        else{
            this.searchOps(node.getChildren().get(0));
            if (oOptimization){
                count = 1;
            }
            for (index = auxOperations.size() - 1; index >= 0; index--) {
                auxName = "aux" + count;
                processExpressions();
            }
            Ollir.append(this.ident + "if(" + auxName + ".bool ==.bool 0.bool) goto else" + this.ifLabelCount + ";" + nl);
        }
        this.ifLabelMap.put(this.getElseFromIf(node), this.ifLabelCount);
        this.ifLabelCount++;
        this.ident += tab;
        if (node.getChildren().get(1).getKind().equals("Body")){
            for (JmmNode bodyNode: node.getChildren().get(1).getChildren()){
                this.visitExpressions(bodyNode, this.reports);
            }
        }
        else{
            this.visitExpressions(node.getChildren().get(1), this.reports);
        }
        Ollir.append(this.ident + "goto endif" + this.ifLabelMap.get(this.getElseFromIf(node)) + ";" + nl);
        this.ident = this.ident.substring(0, this.ident.length() - 4);
    }

    private void elsesHandler(JmmNode node){
        Ollir.append(this.ident + "else" + this.ifLabelMap.get(node) + ":" + nl);
        this.ident += tab;
        if (node.getChildren().get(0).getKind().equals("Body")){
            for (JmmNode bodyNode: node.getChildren().get(0).getChildren()){
                this.visitExpressions(bodyNode, this.reports);
            }
        }
        else{
            this.visitExpressions(node.getChildren().get(0), this.reports);
        }
        this.ident = this.ident.substring(0, this.ident.length() - 4);
        Ollir.append(this.ident + "endif" + this.ifLabelMap.get(node) + ":" + nl);
    }

    private void whilesHandler(JmmNode node) {
        Ollir.append(this.ident + "Loop" + this.whileLabelCount + ":" + nl);
        this.ident += tab;
        if (node.getChildren().get(0).getChildren().get(0).getKind().equals("True") || node.getChildren().get(0).getChildren().get(0).getKind().equals("False")){
            Ollir.append(this.ident + "if(" + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool ==.bool 1.bool) goto Body" + this.whileLabelCount + ";" + nl);
        }
        else if (node.getChildren().get(0).getChildren().get(0).getKind().equals("Name")){
            if (table.isField(node.getChildren().get(0).getChildren().get(0).get("name"))) {
                Ollir.append(this.ident + "aux1.bool :=.i32 getfield(this, " + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool).bool;" + nl);
                Ollir.append(this.ident + "if(aux1.bool ==.bool 1.bool) goto Body" + this.whileLabelCount + ";" + nl);
            }
            else{
                Ollir.append(this.ident + "if(" + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool ==.bool 1.bool) goto Body" + this.whileLabelCount + ";" + nl);
            }
        }
        else{
            this.searchOps(node.getChildren().get(0));
            if (oOptimization){
                count = 1;
            }
            for (index = auxOperations.size() - 1; index >= 0; index--) {
                auxName = "aux" + count;
                processExpressions();
            }
            Ollir.append(this.ident + "if(" + auxName + ".bool ==.bool 1.bool) goto Body" + this.whileLabelCount + ";" + nl);
        }
        Ollir.append(this.ident + "goto EndLoop" + this.whileLabelCount + ";"  + nl);
        this.ident = this.ident.substring(0, this.ident.length() - 4);
        Ollir.append(this.ident + "Body" + this.whileLabelCount + ":" + nl);
        this.whileLabelMap.put(node, this.whileLabelCount);
        this.ident += tab;
        this.whileLabelCount++;
        for (JmmNode bodyNode: node.getChildren().get(1).getChildren()){
            this.visitExpressions(bodyNode, this.reports);
        }
        Ollir.append(this.ident + "goto Loop" + this.whileLabelMap.get(node) + ";" + nl);
        this.ident = this.ident.substring(0, this.ident.length() - 4);
        Ollir.append(this.ident + "EndLoop" + this.whileLabelMap.get(node) + ":" + nl);
    }

    private void optimizedWhilesHandler(JmmNode node) {
        Ollir.append(this.ident + "Loop" + this.whileLabelCount + ":" + nl);
        this.ident += tab;
        if (node.getChildren().get(0).getChildren().get(0).getKind().equals("True") || node.getChildren().get(0).getChildren().get(0).getKind().equals("False")){
            Ollir.append(this.ident + "if(" + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool ==.bool 0.bool) goto EndLoop" + this.whileLabelCount + ";" + nl);
        }
        else if (node.getChildren().get(0).getChildren().get(0).getKind().equals("Name")){
            if (table.isField(node.getChildren().get(0).getChildren().get(0).get("name"))) {
                Ollir.append(this.ident + "aux1.bool :=.i32 getfield(this, " + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool).bool;" + nl);
                Ollir.append(this.ident + "if(aux1.bool ==.bool 0.bool) goto EndLoop" + this.whileLabelCount + ";" + nl);
            }
            else{
                Ollir.append(this.ident + "if(" + nodeToName(node.getChildren().get(0).getChildren().get(0)) + ".bool ==.bool 0.bool) goto EndLoop" + this.whileLabelCount + ";" + nl);
            }
        }
        else{
            this.searchOps(node.getChildren().get(0));
            if (oOptimization){
                count = 1;
            }
            for (index = auxOperations.size() - 1; index >= 0; index--) {
                auxName = "aux" + count;
                processExpressions();
            }
            Ollir.append(this.ident + "if(" + auxName + ".bool ==.bool 0.bool) goto EndLoop" + this.whileLabelCount + ";" + nl);
        }
        this.ident = this.ident.substring(0, this.ident.length() - 4);
        this.whileLabelMap.put(node, this.whileLabelCount);
        this.ident += tab;
        this.whileLabelCount++;
        for (JmmNode bodyNode: node.getChildren().get(1).getChildren()){
            this.visitExpressions(bodyNode, this.reports);
        }
        Ollir.append(this.ident + "goto Loop" + this.whileLabelMap.get(node) + ";" + nl);
        this.ident = this.ident.substring(0, this.ident.length() - 4);
        Ollir.append(this.ident + "EndLoop" + this.whileLabelMap.get(node) + ":" + nl);
    }

    private void searchOps(JmmNode node){
        for (JmmNode child: node.getChildren()){
            if (operations.contains(child.getKind()) || booleanOperations.contains(child.getKind()) || child.getKind().equals("NewInstance") || child.getKind().equals("Length")){
                auxOperations.add(child);
                searchOps(child);
            }
            else if (child.getKind().equals("MethodCall")) {
                auxOperations.add(child);
                searchOps(child.getChildren().get(1));
            }
            else if (child.getKind().equals("ArrayAccess")){
                auxOperations.add(child);
                if (table.isField(child.getChildren().get(0).get("name"))){
                    auxOperations.add(child.getChildren().get(0));
                }
                searchOps(child.getChildren().get(1));
            }
            else if (child.getKind().equals("NewArray")){
                auxOperations.add(child);
                searchOps(child);
            }
            else if (child.getKind().equals("Name")){
                if (table.isField(child.get("name"))){
                    auxOperations.add(child);
                }
            }
        }
    }


    private String getFromAuxVarMethods(JmmNode node){
        for (JmmNode auxVar: this.auxVarOps.keySet()){
            if (auxVar.equals(node)){
                return this.auxVarOps.get(auxVar);
            }
        }
        return null;
    }


    private String getInvokeString(JmmNode nodeMethod){
        StringBuilder result = null;
        if (nodeMethod.getChildren().get(0).getKind().equals("This")) {
            result = new StringBuilder("invokevirtual(this, \"" + Utils.sanitize(nodeMethod.get("name")) + "\"");
        }
        else if (nodeMethod.getChildren().get(0).getKind().equals("NewInstance")){
            this.Ollir.append(this.ident + auxName + "." + Utils.sanitize(nodeMethod.getChildren().get(0).get("name")) + " :=." + Utils.sanitize(nodeMethod.getChildren().get(0).get("name")) + " new(" + Utils.sanitize(nodeMethod.getChildren().get(0).get("name")) + ")." + Utils.sanitize(nodeMethod.getChildren().get(0).get("name")) + ";\n");
            this.Ollir.append(this.ident + "invokespecial(" + auxName + "." + Utils.sanitize(nodeMethod.getChildren().get(0).get("name")) + ",\"<init>\").V;\n");
            result = new StringBuilder("invokevirtual(" + auxName + "." + Utils.sanitize(nodeMethod.getChildren().get(0).get("name")) + "," + "\"" + Utils.sanitize(nodeMethod.get("name")) + "\"");
            if (index == 0){
                auxName = getVarName(this.parentMethod.getName(), this.assignmentVar.getName());
            }
            else{
                count++;
                auxName = "aux" + count;
            }
        }
        else if (nodeMethod.getChildren().get(0).getKind().equals("Name")){
            for (String imp: this.table.getImports()){
                if (imp.equals(nodeMethod.getChildren().get(0).get("name"))){
                    result = new StringBuilder("invokestatic(" + imp + "," + "\"" + Utils.sanitize(nodeMethod.get("name")) + "\"");
                }
            }
            if (result == null){
                Symbol var = this.table.searchVariable(nodeMethod.getChildren().get(0).get("name"), this.parentMethod.getName());
                result = new StringBuilder("invokevirtual(" + var.getName() + "." + var.getType().getName() +  "," + "\"" + Utils.sanitize(nodeMethod.get("name")) + "\"");
            }
        }
        List<String> variables = new ArrayList<>();
        List<String> types = new ArrayList<>();
        if (nodeMethod.getChildren().get(1).getChildren().isEmpty()){
            return result.toString();
        }
        else{
            result.append(", ");
        }
        for (JmmNode arg: nodeMethod.getChildren().get(1).getChildren()){
            String variable = this.auxVarOps.get(arg);
            if (variable != null){
                variables.add(variable);
                types.add(this.nodeToTypeString(arg));
            }
            else{
                variables.add(this.nodeToName(arg));
                types.add(this.nodeToTypeString(arg));
            }
        }
        int index = 0;
        for (String variable: variables){
            if (index == variables.size() - 1){
                result.append(variable + "." + types.get(index));
                return result.toString();
            }
            result.append(variable + "." + types.get(index) + ", ");
            index++;
        }
        return "";
    }

    private String nodeToTypeString(JmmNode node){
        switch(node.getKind()){
            case "Num":
            case "Div":
            case "Mul":
            case "Add":
            case "Sub":
            case "ArrayAccess":
                return "i32";
            case "True":
            case "False":
            case "Boolean":
            case "And":
            case "LessThan":
            case "Not":
                return "bool";
            case "MethodCall":
                if (this.table.getMethodByNode(node) != null) return this.utils.typeToString(this.table.getMethodByNode(node).getReturnType());
                else if (node.getParent().getKind().equals("ArgsList")){
                    Method method = this.table.getMethodByNode(node.getParent().getParent());
                    if(method != null) {
                        int index = 0;
                        for (JmmNode argNode: node.getParent().getChildren()){
                            if (argNode.equals(node)){
                                break;
                            }
                            index++;
                        }
                        return this.utils.typeToString(method.getParameters().get(index).getType());
                    }
                    else{
                        return "V";
                    }
                }
                else if (node.getParent().getKind().equals("Assignment")){
                    return nodeToTypeString(node.getParent().getChildren().get(0));
                }
                else if (node.getParent().getKind().equals("Return")){
                    return this.utils.typeToString(parentMethod.getReturnType());
                }
                else if (node.getParent().getKind().equals("ArrayAssignment")){
                    return "i32";
                }
                else return "V";
            case "NewInstance":
                return node.get("name");
            case "NewArray":
                return "array.i32";
            case "Length":
                return "i32";
            case "Name":
                return this.utils.typeToString(this.table.searchVariable(node.get("name"), this.parentMethod.getName()).getType());
            default:
                break;
        }
        return "";
    }

    private String nodeToName(JmmNode node){
        switch(node.getKind()){
            case "True":
                return "1";
            case "False":
                return "0";
            case "Name":
                if (table.isField(node.get("name"))){
                    String result = this.auxVarOps.get(node);
                    if (result == null){
                        return getVarName(this.parentMethod.getName(), node.get("name"));
                    }
                    else{
                        return Utils.sanitize(result);
                    }

                }
                else{
                    return getVarName(this.parentMethod.getName(), node.get("name"));
                }
            case "Num":
                return node.get("value");
            case "Length":
                return getVarName(this.parentMethod.getName(), nodeToName(node.getChildren().get(0)));
            default:
                return auxVarOps.get(node);
        }
    }

    private void reset(){
        this.auxOperations.clear();
        this.auxVarOps.clear();
    }

    private String getVarName(String methodName, String varName) {
        int argIndex = this.table.isArgument(varName, methodName);
        String result;
        String replacedVar = varName;
        if (argIndex != 0) {
            result = "$" + argIndex + "." + Utils.sanitize(replacedVar);
        } else {
            result = Utils.sanitize(replacedVar);
        }
        return result;
    }


    private void doNots(){
        this.Ollir.append(this.ident + auxName + ".bool" + " :=.bool !.bool " + nodeToName(auxOperations.get(index).getChildren().get(0)) + ".bool;" + nl);
        auxVarOps.put(auxOperations.get(index), auxName);
    }

    private void doOperations() {
        JmmNode opNode1 = auxOperations.get(index).getChildren().get(0);
        JmmNode opNode2 = auxOperations.get(index).getChildren().get(1);

        if (opNode1.getKind().equals("Name")) {
            String varName = nodeToName(opNode1);
            this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + varName + "." + nodeToTypeString(opNode1));
            this.Ollir.append(" " + utils.opToString(auxOperations.get(index).getKind()));

        } else if (opNode1.getKind().equals("Num") || opNode1.getKind().equals("True") || opNode1.getKind().equals("False")) {
            this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + utils.nodeToValueString(opNode1) + "." + nodeToTypeString(opNode1));
            this.Ollir.append(" " + utils.opToString(auxOperations.get(index).getKind()));
        } else {
            String varName = this.getFromAuxVarMethods(opNode1);
            this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + varName + "." + nodeToTypeString(opNode1));
            this.Ollir.append(" " + utils.opToString(auxOperations.get(index).getKind()));
        }

        if (opNode2.getKind().equals("Name")) {
            String varName = nodeToName(opNode2);
            this.Ollir.append(" " + varName + "." + nodeToTypeString(opNode2) + ";" + nl);

        } else if (opNode2.getKind().equals("Num") || opNode2.getKind().equals("True") || opNode2.getKind().equals("False")) {
            this.Ollir.append(" " + utils.nodeToValueString(opNode2) + "." + nodeToTypeString(opNode2) + ";" + nl);
        } else {
            String varName = this.getFromAuxVarMethods(opNode2);
            this.Ollir.append(" " + varName + "." + nodeToTypeString(opNode2) + ";" + nl);
        }
        auxVarOps.put(auxOperations.get(index), auxName);
    }


    private void doNewInstances() {
        String newInstanceString = "new(" + Utils.sanitize(auxOperations.get(index).get("name"));
        this.Ollir.append(this.ident + auxName + "." + Utils.sanitize(auxOperations.get(index).get("name")) + " :=." + Utils.sanitize(auxOperations.get(index).get("name")) + " " + newInstanceString + ")." + Utils.sanitize(auxOperations.get(index).get("name")) + ";" + nl);
        this.Ollir.append(this.ident + "invokespecial(" + Utils.sanitize(auxName) + "." + Utils.sanitize(auxOperations.get(index).get("name")) + ",\"<init>\").V;\n");
        auxVarOps.put(auxOperations.get(index), auxName);
    }

    private void doLength() {
        this.Ollir.append(this.ident + auxName + ".i32" + " :=.i32 arraylength(" + nodeToName(auxOperations.get(index)) + ".array.i32).i32" + ";" + nl);
        auxVarOps.put(auxOperations.get(index), auxName);
    }

    private void doArrayAccesses() {
        String arrayAccessString = nodeToName(auxOperations.get(index).getChildren().get(0)) + "[";
        if (auxOperations.get(index).getChildren().get(1).getChildren().get(0).getKind().equals("Name")) {
            arrayAccessString += nodeToName(auxOperations.get(index).getChildren().get(1).getChildren().get(0)) + ".i32";
        } else if (auxOperations.get(index).getChildren().get(1).getChildren().get(0).getKind().equals("Num")) {
            this.Ollir.append(this.ident + "aux" + count + ".i32" + " :=.i32 " + auxOperations.get(index).getChildren().get(1).getChildren().get(0).get("value") + ".i32;" + nl);
            arrayAccessString += "aux" + count + ".i32";
            if (index == 0 && !auxOperations.get(index).getParent().getKind().equals("ArrayAssignment")) {
                this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + arrayAccessString + "]." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
                auxVarOps.put(auxOperations.get(index), auxName);
            } else {
                this.Ollir.append(this.ident + "aux" + (count + 1) + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + arrayAccessString + "]." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
                auxVarOps.put(auxOperations.get(index), "aux" + (count + 1));
            }
            count += 1;
            return;
        } else {
            arrayAccessString += this.auxVarOps.get(auxOperations.get(index).getChildren().get(1).getChildren().get(0)) + ".i32";
        }
        this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " " + arrayAccessString + "]." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
        auxVarOps.put(auxOperations.get(index), auxName);
    }

    //Actually takes care of fields (which are Names)
    private void doNames() {
        this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + " getfield(this, " + nodeToName(auxOperations.get(index)) + "." + nodeToTypeString(auxOperations.get(index)) + ")." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
        auxVarOps.put(auxOperations.get(index), auxName);
    }

    private void doNewArrays() {
        String newArrayString = " new(array, ";
        if (auxOperations.get(index).getChildren().get(0).getKind().equals("Num") || auxOperations.get(index).getChildren().get(0).getKind().equals("Name")) {
            newArrayString += nodeToName(auxOperations.get(index).getChildren().get(0)) + ".i32";
        } else {
            newArrayString += this.auxVarOps.get(this.auxOperations.get(index).getChildren().get(0)) + ".i32";
        }
        this.Ollir.append(this.ident + auxName + "." + nodeToTypeString(auxOperations.get(index)) + " :=." + nodeToTypeString(auxOperations.get(index)) + newArrayString + ")." + nodeToTypeString(auxOperations.get(index)) + ";" + nl);
        auxVarOps.put(auxOperations.get(index), auxName);
    }

    private JmmNode getElseFromIf(JmmNode ifNode){
        int index = 0;
        for (JmmNode siblingNode: ifNode.getParent().getChildren()){
            if (siblingNode.equals(ifNode)){
                break;
            }
            index++;
        }
        return ifNode.getParent().getChildren().get(index+1);
    }
}
