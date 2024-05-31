package ollir;

import pt.up.fe.comp.jmm.JmmNode;
import table.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public String methodParametersToString(Method method){
        StringBuilder result = new StringBuilder("(");
        int count = 0;
        for (Symbol parameter: method.getParameters()){
            result.append(symbolToString(parameter));
            count++;
            if (count <= method.getParameters().size()-1){
                result.append(",");
            }

        }
        result.append(")." + typeToString(method.getReturnType()));

        return result.toString();
    }


    public String symbolToString(Symbol symbol){
        StringBuilder result = new StringBuilder("");
        result.append(Utils.sanitize(symbol.getName())).append(".");
        result.append(typeToString(symbol.getType()));

        return result.toString();
    }

    public String typeToString(Type type){
        StringBuilder result = new StringBuilder("");
        if (type.isArray()){
            result.append("array.");
        }
        switch (type.getName())
        {
            case "Int":
                result.append("i32");
                break;
            case "Boolean":
                result.append("bool");
                break;
            case "String":
                result.append("String");
                break;
            case "void":
                result.append("V");
                break;
            default:
                result.append(type.getName());
                break;
        }

        return result.toString();
    }


    public String opToString(String op){
        switch(op){
            case "Sub":
                return "-.i32";
            case "Mul":
                return "*.i32";
            case "Add":
                return "+.i32";
            case "Div":
                return "/.i32";
            case "And":
                return "&&.bool";
            case "Or":
                return "||.bool";
            case "Not":
                return "!.bool";
            case "LessThan":
                return "<.i32";
            default:
                break;
        }
        return "";
    }


    public String nodeToValueString(JmmNode node){
        switch(node.getKind()){
            case "Num":
                return node.get("value");
            case "True":
                return "1";
            case "False":
                return "0";
        }
        return "";
    }

    public static String sanitize(String name){
        List<String> reserved = new ArrayList<>(List.of("field", "ret", "invoke", "putfield", "getfield", "array"));

        for (String word: reserved){
            if (name.contains(word)){
                return "variable_" + name + "_sanitized";
            }
        }
        return name;
    }
}
