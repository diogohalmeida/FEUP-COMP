import java.util.*;

import org.specs.comp.ollir.*;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

import javax.swing.text.html.HTMLDocument;

/**
 * Copyright 2021 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class BackendStage implements JasminBackend {
    private StringBuilder jasminCodeBuilder;
    Stack<String> localVariableStack = new Stack<>();
    private String className, fullClassName, superClassName;
    ArrayList<String> imports = new ArrayList<>();
    int current_label = 0;
    int stack_counter = 0, stack_max = 0;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {

            // Example of what you can do with the OLLIR class
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method
            ollirClass.show(); // print to console main information about the input OLLIR

            StringBuilder finalCode = new StringBuilder();

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            String jasminCode = ""; // Convert node ...
            jasminCodeBuilder = new StringBuilder();
            String accessModifier = !ollirClass.getClassAccessModifier().name().equals("DEFAULT") ? " " + ollirClass.getClassAccessModifier().name().toLowerCase() : "";
            String finalStr = ollirClass.isFinalClass() ? " final " : "";
            String staticStr = ollirClass.isStaticClass() ? " static " : "";
            jasminCodeBuilder.append(".class" + staticStr + finalStr +  accessModifier + " " + ollirClass.getClassName() + "\n");
            className = "";
            fullClassName = (ollirClass.getPackage() != null) ? ollirClass.getPackage() + "/" + className : ollirClass.getClassName();
            imports = ollirClass.getImports();
            for (String importStr: imports) System.out.println("import: " + importStr);

            className += ollirClass.getClassName();
            if (ollirClass.getSuperClass() != null) {
                superClassName = ollirClass.getSuperClass();
                jasminCodeBuilder.append(".super " + ollirClass.getSuperClass() + "\n");
            }
            else {
                superClassName = "java/lang/Object";
                jasminCodeBuilder.append(".super java/lang/Object\n");
            }
            for (var field : ollirClass.getFields()) {
                String fieldAccessModifier = field.getFieldAccessModifier() == AccessModifiers.DEFAULT ? "" : " " + field.getFieldAccessModifier().name().toLowerCase() + " ";
                jasminCodeBuilder.append(".field" + fieldAccessModifier + field.getFieldName()  + " " + generateParameter(field.getFieldType()));
                if (field.isInitialized()) {
                    jasminCodeBuilder.append(" " + field.getInitialValue());
                }
                jasminCodeBuilder.append("\n");
            }
            finalCode.append(jasminCodeBuilder.toString());

            buildMethods(ollirClass, finalCode);

            finalCode.deleteCharAt(finalCode.length()-1); //delete last '\n'
            jasminCode = finalCode.toString();

            System.out.println(jasminCode);


            // More reports from this stage
            List<Report> reports = new ArrayList<>();

            return new JasminResult(ollirResult, jasminCode, reports);
            //return null;

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), jasminCodeBuilder.toString(),
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }

    private void buildMethods(ClassUnit ollirClass, StringBuilder code) {
        for (Method method: ollirClass.getMethods()) {
            localVariableStack = new Stack<>();
            jasminCodeBuilder = new StringBuilder();
            StringBuilder methodHeader = new StringBuilder();
            stack_counter = 0;
            stack_max = 0;
            if (method.isConstructMethod()) {
                methodHeader.append(".method public <init>()" + method.getReturnType().toString().charAt(0) + "\n");
            }
            else {
                String isStatic = method.isStaticMethod() ? "static " : "";
                String isFinal = method.isFinalMethod() ? "final " : "";
                methodHeader.append(".method " + method.getMethodAccessModifier().name().toLowerCase() + " " + isStatic +  isFinal + method.getMethodName());
                methodHeader.append("(");
                for (var parameter: method.getParams()) {
                    methodHeader.append(generateParameter(parameter.getType()));
                }
                methodHeader.append(")" + generateParameter(method.getReturnType()) + "\n");
                methodHeader.append("\t.limit locals " + (method.getVarTable().size() + 1) + "\n");
            }

            for (var instruction: method.getInstructions()) {
                placeLabel(method, instruction);
                switch(instruction.getInstType()) {
                    case ASSIGN:
                        generateInstruction((AssignInstruction) instruction, method);
                        break;
                    case CALL:
                        generateInstruction((CallInstruction) instruction, method, null);
                        break;
                    case RETURN:
                        generateInstruction((ReturnInstruction) instruction, method);
                        break;
                    case PUTFIELD:
                        generateInstruction((PutFieldInstruction) instruction, method);
                        break;
                    case GOTO:
                        generateInstruction((GotoInstruction) instruction, method);
                        break;
                    case BRANCH:
                        generateInstruction((CondBranchInstruction) instruction, method);
                        break;

                }
            }
            if (method.isConstructMethod()) jasminCodeBuilder.append("\treturn\n");
            jasminCodeBuilder.append(".end method\n");
            //System.out.println("LOCALS: " + stack_max);
            if (!method.isConstructMethod())
                methodHeader.append("\t.limit stack " + stack_max +"\n");
            methodHeader.append(jasminCodeBuilder.toString());
            code.append(methodHeader.toString());
        }
    }

    private void incrementStack() {
        if (stack_counter < 0 ) stack_counter = 0;
        stack_counter++;
        if (stack_counter > stack_max) stack_max = stack_counter;
    }

    private void placeLabel(Method method, Instruction instruction) {
        Iterator it = method.getLabels().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (instruction == pair.getValue()) {
                jasminCodeBuilder.append(pair.getKey() + ":\n");
                //return;
            }
        }
    }


    private String generateParameter(Type parameterType) {
        String result = "";
        switch (parameterType.getTypeOfElement().name()) {
            case "ARRAYREF":
                var arrayType = ((ArrayType) parameterType);
                for (int i = 0; i < arrayType.getNumDimensions(); i++) {
                    result += "[";
                }
                if (arrayType.getTypeOfElements().name().equals("INT32")) {
                    result += "I";
                }

                else if (arrayType.getTypeOfElements().name().equals("STRING")) {
                    result += "Ljava/lang/String;";
                }
                break;

            case "BOOLEAN":
                result += "Z";
                break;

            case "INT32":
                result += "I";
                break;

            case "OBJECTREF":
                var classType = ((ClassType) parameterType);
                String name = null;
                if (classType.getName().equals(className)) name = fullClassName;
                else name = getFullClassName(classType.getName());
                result += "L" + name;
                break;

            case "VOID":
                result += "V";
                break;
        }
        return result;
    }

    private void pushElement(Element e, Method method) {
        if (e instanceof Operand) {
            Operand op = (Operand) e;
            if (e.getType().toString().equals("INT32") || e.getType().toString().equals("BOOLEAN"))
                jasminCodeBuilder.append("\tiload" + selectRegister(method, op.getName()) + "\n");
            else {
                jasminCodeBuilder.append("\taload" + selectRegister(method, op.getName()) + "\n");
            }
        }

        else if (e instanceof LiteralElement) {
            LiteralElement literalElement = (LiteralElement) e;
            pushConstant(literalElement.getLiteral());
        }
        incrementStack();
    }

    private void generateInstruction(CondBranchInstruction instruction, Method method) {
        if (instruction.getRightOperand().isLiteral() && Integer.parseInt(((LiteralElement) instruction.getRightOperand()).getLiteral()) == 0 && instruction.getCondOperation().getOpType().name().equals("EQ")) {
            if (instruction.getLeftOperand() != null) pushElement(instruction.getLeftOperand(), method);

            switch (instruction.getCondOperation().getOpType().name()) {
                case "EQ":
                    jasminCodeBuilder.append("\tifeq " + instruction.getLabel() + "\n");
                    break;

                case "GTE":
                    jasminCodeBuilder.append("\tifge " + instruction.getLabel() + "\n");
                    break;

                case "LTH":
                    jasminCodeBuilder.append("\tiflt " + instruction.getLabel() + "\n");
                    break;
                default: break;
            }
            stack_counter--;
        }

        else {
            if (instruction.getLeftOperand() != null) pushElement(instruction.getLeftOperand(), method);

            if (instruction.getRightOperand() != null) pushElement(instruction.getRightOperand(), method);

            switch (instruction.getCondOperation().getOpType().name()) {
                case "EQ":
                    jasminCodeBuilder.append("\tif_icmpeq " + instruction.getLabel() + "\n");
                    break;

                case "GTE":
                    jasminCodeBuilder.append("\tif_icmpge " + instruction.getLabel() + "\n");
                    break;

                case "LTH":
                    jasminCodeBuilder.append("\tif_icmplt " + instruction.getLabel() + "\n");
                    break;
                default: break;
            }
            stack_counter -= 2;
        }

    }

    private void generateInstruction(GotoInstruction instruction, Method method) {
        //instruction.show();
        jasminCodeBuilder.append("\tgoto " + instruction.getLabel() + "\n");
    }

    private void generateInstruction(AssignInstruction instruction, Method method) {
        String first = ((Operand)instruction.getDest()).getName();

        if (instruction.getRhs().getClass() == SingleOpInstruction.class){
            Element element = ((SingleOpInstruction) instruction.getRhs()).getSingleOperand();

            if (!instruction.getDest().isLiteral() && instruction.getDest() instanceof ArrayOperand) {

                jasminCodeBuilder.append("\taload" + selectRegister(method, first) + "\n");
                incrementStack();
                pushElement(((ArrayOperand) instruction.getDest()).getIndexOperands().get(0), method);
                pushElement(((SingleOpInstruction) instruction.getRhs()).getSingleOperand(), method);
                jasminCodeBuilder.append("\tiastore\n");
                stack_counter -= 3;
            }

            else if (element.isLiteral()){
                String literal = ((LiteralElement) element).getLiteral();
                pushConstant(literal); //pushes int literal into frame stack
                jasminCodeBuilder.append("\tistore" + selectRegister(method, first) + "\n"); //pops literal from stack
                stack_counter--;

            }

            else if (((SingleOpInstruction) instruction.getRhs()).getSingleOperand() instanceof ArrayOperand){
                var index = ((ArrayOperand) element).getIndexOperands().get(0);
                String literalIndex = ((Operand) index).getName();
                var arrayName = ((ArrayOperand) element).getName();
                jasminCodeBuilder.append("\taload" + selectRegister(method, arrayName) + "\n");  //pushes array reference into stack
                incrementStack();
                jasminCodeBuilder.append("\tiload" + selectRegister(method, literalIndex) + "\n");  //pushes int index reference into stack
                incrementStack();
                jasminCodeBuilder.append("\tiaload\n");
                stack_counter--;
                jasminCodeBuilder.append("\tistore" + selectRegister(method, first) + "\n");
                stack_counter--;
            }

            else {
                //SingleOpInstruction instanceof Operand
                var operand = ((Operand) ((SingleOpInstruction) instruction.getRhs()).getSingleOperand());

                if (operand.getType().getTypeOfElement().name().equals("INT32") || operand.getType().getTypeOfElement().name().equals("BOOLEAN")) {
                    jasminCodeBuilder.append("\tiload" + selectRegister(method, operand.getName()) + "\n");
                    incrementStack();
                    jasminCodeBuilder.append("\tistore" + selectRegister(method, first) + "\n");
                    stack_counter--;
                }
                else {
                    jasminCodeBuilder.append("\taload" + selectRegister(method, operand.getName()) + "\n");
                    incrementStack();
                    jasminCodeBuilder.append("\tastore" + selectRegister(method, first) + "\n");
                    stack_counter--;
                }
            }
        }

        else if (instruction.getRhs().getClass().equals(CallInstruction.class)) {
            generateInstruction((CallInstruction) instruction.getRhs(), method, instruction.getDest());
        }

        else if (instruction.getRhs().getClass().equals(BinaryOpInstruction.class)){
            var dest = ((Operand) instruction.getDest());
            generateInstruction((BinaryOpInstruction) instruction.getRhs(), method, dest);
        }

        else if (instruction.getRhs().getClass().equals(GetFieldInstruction.class)) {
            var dest = ((Operand) instruction.getDest());
            generateInstruction((GetFieldInstruction) instruction.getRhs(), method, dest);
        }

        else if (instruction.getRhs().getClass().equals(UnaryOpInstruction.class)) {
            generateInstruction((UnaryOpInstruction) instruction.getRhs(), method, instruction.getDest());
        }
    }

    private void generateInstruction(UnaryOpInstruction instruction, Method method, Element dest) {
        //1 - var(Z)
        var rightOperand = ((Operand) instruction.getRightOperand());
        jasminCodeBuilder.append("\ticonst_1\n");
        jasminCodeBuilder.append("\tiload" + selectRegister(method, rightOperand.getName()) + "\n");
        incrementStack();
        incrementStack();
        jasminCodeBuilder.append("\tisub\n");
        stack_counter --;
        jasminCodeBuilder.append("\tistore" + selectRegister(method, ((Operand) dest).getName()) + "\n");
        stack_counter--;
    }

    private void checkFrameStack(Operand varName, Method method) {
        ArrayList<String> topElements = new ArrayList<>();
        if (!localVariableStack.empty()) {
            var first = localVariableStack.get(0);
            topElements.add(first);
        }
        if (localVariableStack.size() > 1) {
            var second = localVariableStack.get(1);
            topElements.add(second);
        }

        if (!topElements.contains(varName.getName())) {
            var type = varName.getType().getTypeOfElement().name();
            if (type.equals("INT32") || type.equals("BOOLEAN")  ) {
                jasminCodeBuilder.append("\tiload" + selectRegister(method, varName.getName()) + "\n");
            }
            else
                jasminCodeBuilder.append("\taload" + selectRegister(method, varName.getName()) + "\n");
            //localVariableStack.push(varName.getName());
            incrementStack();
        }
    }


    private void generateInstruction(BinaryOpInstruction instruction, Method method, Element dest) {
        var leftOperand = instruction.getLeftOperand();
        var rightOperand = instruction.getRightOperand();
        boolean oneByteLong = true;

        if (rightOperand.isLiteral() && (Integer.parseInt(((LiteralElement) rightOperand).getLiteral()) > 32767 || Integer.parseInt(((LiteralElement) rightOperand).getLiteral()) < -32768)) {
            oneByteLong = false;
        }

        if (dest.getType().toString().equals("INT32") && !leftOperand.isLiteral() && rightOperand.isLiteral() && dest != null && oneByteLong && ((Operand) dest).getName().equals(((Operand) leftOperand).getName())) {
            var left = (Operand) leftOperand;
            var literal = (LiteralElement) rightOperand;
            var destName = ((Operand) dest).getName();

            //pushElement();

            if (instruction.getUnaryOperation().getOpType().name().equals("ADD") && destName.equals(left.getName())) {
                jasminCodeBuilder.append("\tiinc " + method.getVarTable().get(left.getName()).getVirtualReg() + " " + literal.getLiteral() + " \n");
            }

            else if (instruction.getUnaryOperation().getOpType().name().equals("SUB") && destName.equals(left.getName())) {
                jasminCodeBuilder.append("\tiinc " + method.getVarTable().get(left.getName()).getVirtualReg() + " -" + literal.getLiteral() + " \n");
            }
        }

        else {
            if (leftOperand.isLiteral()) {
                var literal = ((LiteralElement) leftOperand).getLiteral();
                pushConstant(literal);
            }
            else {
                checkFrameStack((Operand) leftOperand, method);
            }

            if (rightOperand.isLiteral()) {
                var literal = ((LiteralElement) rightOperand).getLiteral();
                pushConstant(literal);
            }

            else {
                checkFrameStack((Operand) rightOperand, method);
            }

            String opType = instruction.getUnaryOperation().getOpType().name();
            switch (opType) {
                case "ADD":
                    jasminCodeBuilder.append("\tiadd\n");
                    stack_counter--;
                    break;
                case "MUL":
                    jasminCodeBuilder.append("\timul\n");
                    stack_counter--;
                    break;
                case "SUB":
                    jasminCodeBuilder.append("\tisub\n");
                    stack_counter--;
                    break;
                case "DIV":
                    jasminCodeBuilder.append("\tidiv\n");
                    stack_counter--;
                    break;
                case "ANDB":
                    jasminCodeBuilder.append("\tiand\n");
                    stack_counter--;
                    break;
                case "LTH":
                    jasminCodeBuilder.append("\tif_icmpge less" + current_label + "\n");
                    jasminCodeBuilder.append("\ticonst_1\n");
                    incrementStack();
                    jasminCodeBuilder.append("\tgoto less" + (current_label + 1) + "\n");
                    jasminCodeBuilder.append("less" + current_label + ":\n\ticonst_0\n");
                    jasminCodeBuilder.append("less" + (current_label + 1) + ":\n\tistore" + selectRegister(method, ((Operand)dest).getName()) + "\n");
                    current_label += 2;
                    stack_counter--;
                    break;
            }

            if (!opType.equals("LTH")) {
                jasminCodeBuilder.append("\tistore" + selectRegister(method, ((Operand)dest).getName()) + "\n");
                stack_counter--;
            }

        }




    }


    private void generateInstruction(CallInstruction instruction, Method method, Element dest) {
        System.out.println(OllirAccesser.getCallInvocation(instruction));
        CallType callInvocation = OllirAccesser.getCallInvocation(instruction);
        String call = callInvocation.toString();
        var destOperand = ((Operand) dest);
        switch(call) {
            case "invokespecial":
                //handle class constructor
                var operand = ((Operand)instruction.getFirstArg());
                if (operand.getType().toString().equals("THIS")) { //enters the following segment if it is a construct method
                    if (localVariableStack.empty() || !localVariableStack.get(0).equals(operand.getName())) {
                        jasminCodeBuilder.append("\taload" + selectRegister(method, operand.getName()) + "\n");
                        incrementStack();
                    }
                    LiteralElement secondArg = null;
                    if (instruction.getSecondArg() != null && instruction.getSecondArg().isLiteral()) {
                        secondArg = ((LiteralElement) instruction.getSecondArg());
                    }
                    String secondArgStr = secondArg != null ? secondArg.getLiteral() : "";
                    secondArgStr = secondArgStr.replaceAll("\"", "");
                    jasminCodeBuilder.append("\tinvokespecial " + superClassName + "." + secondArgStr + "()" + generateParameter(instruction.getReturnType()) +"\n");
                    stack_counter--;
                }

                break;

            case "arraylength":
                var arrayName = ((Operand) instruction.getFirstArg());
                if (arrayName.getType().getTypeOfElement().name().equals("ARRAYREF")) {
                    if (localVariableStack.empty() || !localVariableStack.get(0).equals(arrayName.getName())) {
                        jasminCodeBuilder.append("\taload" + selectRegister(method, arrayName.getName()) + "\n");
                        incrementStack();
                    }
                    jasminCodeBuilder.append("\tarraylength\n");

                    jasminCodeBuilder.append("\tistore" + selectRegister(method, destOperand.getName()) + "\n");
                    stack_counter--;
                }
                break;

            case "NEW":
                if (instruction.getFirstArg().getType() instanceof ArrayType){
                    if (instruction.getListOfOperands().get(0).isLiteral()) {
                        LiteralElement literal = ((LiteralElement) instruction.getListOfOperands().get(0));
                        pushConstant(literal.getLiteral()); //pushes number of array elements into array
                    }
                    else {
                        var arrayOperand = ((Operand) instruction.getListOfOperands().get(0));
                        if (localVariableStack.empty() || !localVariableStack.get(0).equals(arrayOperand.getName())) {
                            jasminCodeBuilder.append("\tiload"  + selectRegister(method, arrayOperand.getName()) + "\n");
                            incrementStack();
                        }
                    }
                    jasminCodeBuilder.append("\tnewarray int\n");
                    jasminCodeBuilder.append("\tastore" + selectRegister(method, destOperand.getName()) + "\n");
                    stack_counter--;
                }


                else if (instruction.getFirstArg().getType() instanceof ClassType) {
                    var classNameStr = ((Operand) instruction.getFirstArg()).getName();
                    if (classNameStr.equals(className)){
                        classNameStr = fullClassName;
                    }
                    else {
                        classNameStr = getFullClassName(classNameStr);
                    }

                    jasminCodeBuilder.append("\tnew " + classNameStr + "\n"); //create object
                    incrementStack();
                    jasminCodeBuilder.append("\tdup\n"); //duplicate object reference on top of the stack
                    incrementStack();
                    jasminCodeBuilder.append("\tinvokespecial " + classNameStr +"/<init>()" + "V" +"\n");
                    stack_counter--;
                    jasminCodeBuilder.append("\tastore" + selectRegister(method, destOperand.getName()) + "\n");
                    stack_counter--;
                }

                break;

            case "invokevirtual":
                var invoke = ((Operand) instruction.getFirstArg());

                String invokeCall = invoke.getName();
                jasminCodeBuilder.append("\taload" + selectRegister(method, invokeCall) + "\n");
                incrementStack();

                invokeCall = ((ClassType) invoke.getType()).getName();
                if (invokeCall.equals(className)) {
                    invokeCall = fullClassName;
                }
                else invokeCall = getFullClassName(invokeCall);
                if (instruction.getSecondArg() != null) {
                    invokeCall += "." + ((LiteralElement) instruction.getSecondArg()).getLiteral();
                }
                invokeCall += "(";
                for (var elem: instruction.getListOfOperands()) {
                    //Push operands into the frame stack
                    if (elem.isLiteral()) {
                        var literalElem = ((LiteralElement) elem);
                        pushConstant(literalElem.getLiteral());
                    }
                    else {
                        var operandElem = ((Operand) elem);
                        if (operandElem.getType().toString().equals("INT32") || operandElem.getType().toString().equals("BOOLEAN")) {
                            jasminCodeBuilder.append("\tiload" + selectRegister(method, operandElem.getName()) + "\n");
                        }
                        else {
                            jasminCodeBuilder.append("\taload" + selectRegister(method, operandElem.getName()) + "\n");
                        }
                        incrementStack();
                    }
                    invokeCall += generateParameter(elem.getType());
                }
                invokeCall += ")" + generateParameter(instruction.getReturnType());
                invokeCall = invokeCall.replaceAll("\"", "");
                jasminCodeBuilder.append("\tinvokevirtual " + invokeCall + "\n");
                for (var ignored : instruction.getListOfOperands()) stack_counter--;

                if (dest != null) { //store call result into dest Element
                    if (destOperand.getType().toString().equals("INT32") || destOperand.getType().toString().equals("BOOLEAN"))
                        jasminCodeBuilder.append("\tistore" + selectRegister(method, destOperand.getName()) + "\n");
                    else jasminCodeBuilder.append("\tastore" + selectRegister(method, destOperand.getName()) + "\n");
                    stack_counter--;
                }
                 else if (!instruction.getReturnType().toString().equals("VOID")){
                    jasminCodeBuilder.append("\tpop\n");
                }
                break;

            case "invokestatic":
                //push the arguments into the stack
                String params = "";
                for (var elem: instruction.getListOfOperands()) {
                    params += generateParameter(elem.getType());
                    if (elem instanceof Operand) {
                        var oper = ((Operand) elem);
                        if (oper.getType().getTypeOfElement().name().equals("INT32") || oper.getType().getTypeOfElement().name().equals("BOOLEAN")) {
                            jasminCodeBuilder.append("\tiload" + selectRegister(method, oper.getName()) + "\n");
                        }
                        else {
                            jasminCodeBuilder.append("\taload" + selectRegister(method, oper.getName()) + "\n");
                        }
                        incrementStack();
                    }
                    else if (elem instanceof LiteralElement) {
                        var literal = ((LiteralElement) elem);
                        pushConstant(literal.getLiteral());
                    }
                }
                var classNameStr = ((Operand) instruction.getFirstArg()).getName();
                if (classNameStr.equals(className)) classNameStr = fullClassName;
                else classNameStr = getFullClassName(classNameStr);
                var methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replaceAll("\"", "");
                jasminCodeBuilder.append("\tinvokestatic " + classNameStr + "." + methodName + "(" + params + ")" + generateParameter(instruction.getReturnType()) + "\n");
                for (var ignored : instruction.getListOfOperands()) stack_counter--;

                if (dest != null) { //store call result into dest Element
                    if (destOperand.getType().toString().equals("INT32") || destOperand.getType().toString().equals("BOOLEAN"))
                        jasminCodeBuilder.append("\tistore" + selectRegister(method, destOperand.getName()) + "\n");
                    else jasminCodeBuilder.append("\tastore" + selectRegister(method, destOperand.getName()) + "\n");
                    stack_counter--;
                }
                if (dest == null && !instruction.getReturnType().toString().equals("VOID")) {
                    jasminCodeBuilder.append("\tpop\n");
                }
                break;

            case "ldc":
                break;
        }
    }

    private String getFullClassName(String classNameStr) {
        for (String importStr: imports) {
            String[] packages = importStr.split("\\.");
            String classImport = importStr.split("\\.")[packages.length - 1];
            if (classNameStr.equals(classImport)) return importStr.replaceAll("\\.", "/");
        }
        return null;
    }

    private void generateInstruction(ReturnInstruction instruction, Method method) {
        if (instruction.getOperand() == null) {
            jasminCodeBuilder.append("\treturn\n");
        }

        else if (instruction.getOperand() != null && instruction.getOperand().isLiteral()) {
            var literal = ((LiteralElement) instruction.getOperand()).getLiteral();
            var typeLiteral = instruction.getOperand().getType().getTypeOfElement().name();
            if (typeLiteral.equals("INT32") || typeLiteral.equals("BOOLEAN")) {
                pushConstant(literal);
                jasminCodeBuilder.append("\tireturn\n");
            }

        }

        else {
            var operand = ((Operand) instruction.getOperand());
            if (localVariableStack.empty() || !localVariableStack.get(0).equals(operand.getName())) {
                var type = operand.getType().getTypeOfElement().name();
                if (type.equals("INT32") || type.equals("BOOLEAN")) {
                    jasminCodeBuilder.append("\tiload" + selectRegister(method, operand.getName()) + "\n\tireturn\n");
                }
                else {
                    jasminCodeBuilder.append("\taload" + selectRegister(method, operand.getName()) + "\n\tareturn\n");
                }
                incrementStack();
            }
        }
    }

    private void generateInstruction(PutFieldInstruction instruction, Method method) {
        var thirdOperand = instruction.getThirdOperand();
        String name = null;
        var classNameStr = ((ClassType) instruction.getFirstOperand().getType()).getName();
        if (classNameStr.equals(className)) {
            name =  fullClassName;
        }
        else name = getFullClassName(classNameStr);
        var firstOper = ((Operand) instruction.getFirstOperand()).getName();
        jasminCodeBuilder.append("\taload" + selectRegister(method, firstOper) + "\n");
        incrementStack();

        if (thirdOperand.isLiteral()) {
            var literal = ((LiteralElement) thirdOperand).getLiteral();
            pushConstant(literal);
            var secondOper = ((Operand) instruction.getSecondOperand()).getName();
            jasminCodeBuilder.append("\tputfield " + name + "/" + secondOper + " " + generateParameter(thirdOperand.getType()) + "\n");
            stack_counter--;
        }
        else {
            var operand = ((Operand) thirdOperand);
            if (operand.getType().toString().equals("INT32") || operand.getType().toString().equals("BOOLEAN")) {
                jasminCodeBuilder.append("\tiload" + selectRegister(method, operand.getName()) + "\n");
            }
            else {
                jasminCodeBuilder.append("\taload" + selectRegister(method, operand.getName()) + "\n");
            }
            incrementStack();
            var methodName = ((Operand) instruction.getSecondOperand()).getName();
            jasminCodeBuilder.append("\tputfield " + name + "/" + methodName + " " + generateParameter(thirdOperand.getType()) + "\n");
            stack_counter--;
        }
    }

    private void generateInstruction(GetFieldInstruction instruction, Method method, Element dest) {
        var firstOperand = ((Operand) instruction.getFirstOperand());
        var secondOperand = ((Operand) instruction.getSecondOperand());
        var destOperand = ((Operand) dest);
        var classType = ((ClassType) firstOperand.getType()).getName();
        jasminCodeBuilder.append("\taload" + selectRegister(method, firstOperand.getName()) + "\n");
        incrementStack();

        jasminCodeBuilder.append("\tgetfield " + classType + "/" + secondOperand.getName() + " " + generateParameter(secondOperand.getType()) + "\n");
        incrementStack();

        if (destOperand.getType().toString().equals("INT32") || destOperand.getType().toString().equals("BOOLEAN"))
            jasminCodeBuilder.append("\tistore" + selectRegister(method, destOperand.getName()) + "\n");
        else jasminCodeBuilder.append("\tastore" + selectRegister(method, destOperand.getName()) + "\n");
        stack_counter--;
    }

    private void pushConstant(String literal) {
        int literalInt = Integer.parseInt(literal);
        if (literalInt > 32767 || literalInt < -32768) {
            //bipush is a 2 byte instruction but way less efficient
            jasminCodeBuilder.append("\tldc " + literal + "\n");
            incrementStack();
        }
        else if (literalInt > 127 || literalInt < -128) {
            jasminCodeBuilder.append("\tsipush " + literal + "\n");
            incrementStack();
            incrementStack();
        }
        else if (literalInt > 5 || literalInt < -1) {
            jasminCodeBuilder.append("\tbipush " + literal + "\n");
            incrementStack();
        }
        else {
            jasminCodeBuilder.append("\ticonst_" + literal + "\n");
            incrementStack();
        }
    }

    private String selectRegister(Method method, String name){
        int register = method.getVarTable().get(name).getVirtualReg();
        return register > 3 ? " " + register : "_" + register;
    }
}