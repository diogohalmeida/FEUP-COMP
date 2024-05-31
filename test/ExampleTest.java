import static org.junit.Assert.*;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {


    //String jmmCode  = SpecsIo.getResource("./fixtures/public/fail/syntactical/NestedLoop.jmm");

    String jmmCode  = SpecsIo.getResource("./fixtures/public/HelloWorld.jmm");

    @Test
    public void testExpression() throws IOException {
        JmmParserResult parserResult = TestUtils.parse(jmmCode);
        //System.out.println(TestUtils.parse(jmmCode).getRootNode().toJson());

        FileWriter file  = new FileWriter("./JSON.txt");
        try{
            file.write(parserResult.getRootNode().toJson());
            List<Report> reports = parserResult.getReports();
            for (Report report: reports){
                System.out.println(report);
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        finally {
            file.flush();
            file.close();
        }
    }

    @Test
    public void analysisTest() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/Life.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }
}

