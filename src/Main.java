
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.StringReader;
import java.util.List;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {
		
		try {
		    Parser myParser = new Parser(new StringReader(jmmCode));
    		SimpleNode root = myParser.Program(); // returns reference to root node
            	
    		root.dump(""); // prints the tree on the screen
    	
    		return new JmmParserResult(root, myParser.getReports());
		} catch(ParseException e) {
			throw new RuntimeException("Error while parsing", e);
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Executing with args: " + Arrays.toString(args));

		File jmmFile = new File(args[0]);
		String file;
		file = Files.readString(jmmFile.toPath());

		//Args Check
		boolean oOptimization = false;
		int rOptimization = 0;
		List<String> argsList = Arrays.asList(args);
		for (String arg : argsList){
			if (arg.equals("-o")){
				oOptimization = true;
				System.out.println("Applying -o optimization (Loop Templates & Variable recycling)");
			}
			else if (arg.contains("-r=")){
				rOptimization = Integer.parseInt(String.valueOf(arg.split("=")[1]));
				System.out.println("Applying -r optimization (Local Variables) with n = " + rOptimization);
			}
		}

		//Jmm Parser Result
		JmmParserResult jmmParserResult = new Main().parse(file);
		FileWriter fileJSON  = new FileWriter("./JSON.txt", false);

		try{
			fileJSON.write(jmmParserResult.getRootNode().toJson());
			List<Report> reports = jmmParserResult.getReports();
			for (Report report: reports){
				System.out.println(report);
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			fileJSON.flush();
			fileJSON.close();
		}
		//Semantics Result
		JmmSemanticsResult jmmSemanticsResult = new AnalysisStage().semanticAnalysis(jmmParserResult);

		//Ollir Result
		OllirResult ollirResult = new OptimizationStage(oOptimization, rOptimization).toOllir(jmmSemanticsResult);

		//-r optimization
		if(rOptimization > 0){
			System.out.println("\n========== Local Variable Optimization ==========");
			System.out.println("=================================================");
			int min = 1;
			boolean stop = false;
			ClassUnit ollirClass = ollirResult.getOllirClass();
			ollirClass.buildVarTables();
			for(Method method: ollirClass.getMethods()){
				int varSize = method.isConstructMethod() ? method.getVarTable().size() : method.getVarTable().size()+1;
				min = Math.max(min, varSize);
				if(varSize > rOptimization){
					stop = true;
				}
				System.out.println("Method: " + method.getMethodName() + "\n\tLocal Variables used in JVM: " + varSize + "\n\tMaximum Number Requested: " + rOptimization + "\n" + "\tOptimization " + (varSize > rOptimization? "failed" + "\n=================================================" : "successful" + "\n================================================="));
			}
			if(stop){
				throw new RuntimeException("The number of local variables provided are not enough to compile the code. Minimum number required: " + min);
			}
			System.out.println("=================================================\n");
		}


		JasminResult jasminResult = new BackendStage().toJasmin(ollirResult);

		Path path = Paths.get(ollirResult.getSymbolTable().getClassName() + "_results/");
		if(!Files.exists(path)) {
			Files.createDirectories(path);
		}

		//AST
		FileWriter fileWriter  = new FileWriter(path + "/ast.json");
		fileWriter.write(jmmParserResult.toJson());
		fileWriter.close();

		// Symbol Table
		FileWriter fileWriter2  = new FileWriter(path + "/symbolTable.txt");
		fileWriter2.write(jmmSemanticsResult.getSymbolTable().print());
		fileWriter2.close();

		// Ollir
		FileWriter fileWriter3  = new FileWriter(path + "/" + ollirResult.getSymbolTable().getClassName() + ".ollir");
		fileWriter3.write(ollirResult.getOllirCode());
		fileWriter3.close();

		// Jasmin
		FileWriter fileWriter4 = new FileWriter(path + "/" + ollirResult.getSymbolTable().getClassName() + ".j");
		fileWriter4.write(jasminResult.getJasminCode());
		fileWriter4.close();

		jasminResult.compile(path.toFile());

		//var output = jasminResult.run("5");

		if (args[0].contains("fail")) {
			throw new RuntimeException("It's supposed to fail");
		}
	}


}