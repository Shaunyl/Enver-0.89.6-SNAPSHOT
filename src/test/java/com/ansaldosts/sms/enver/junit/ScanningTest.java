package com.ansaldosts.sms.enver.junit;

import it.shaunyl.enver.exception.EnverException;
import it.shaunyl.enver.exception.CommandLineParsingException;
import it.shaunyl.enver.Main;
import java.io.IOException;
import java.util.ServiceLoader;
import org.junit.Test;
import it.shaunyl.enver.task.compare.ICompareTaskMode;
import java.util.Iterator;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class ScanningTest {

//    @Test
    public void testCharacterSetScanMode() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "csscan", "-catalog=n", "-frep=y", "-samples=y", "-schemas=HR" };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testVersionShell() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "version" };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testHelpShell() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "help", "-expxml" };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testJUniversalMode() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "juniversal", "-filename=/home/sms/prova.txt" };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testImportXML() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "xload", "-directory=/home/sms/enver/xml/upload", "-format=dd-MMM-yyyy HH.mm.ss" };
        new Main().parseCommandLine(args);
    }

    @Test
    public void testExportXML() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "expxml", "-schemas=HR", "-tables=REGIONS, REGIONS, COUNTRIES, LOCATIONS" };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testExportCSV() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "expcsv", "-query=SELECT * from MONTEVARCHI_TEST.TRANSITS", "-start=1", "-end=10", "-filename=provafile", };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testExportEXCEL() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "expexcel", "-param=src/main/resources/parameterFile.properties", "-format=xlsx", "-start=100"
        };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testCompareSchemasMode() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "compare", "-mode=structure", "-local=HR", "-remote=ENVER/ENVER", "-tnsname=TESTDB" };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void LiquibaseMode() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "liquibase", "-mode=migrate", "-changelog=/home/sms/Desktop/migrate.sql", "-filename=/home/sms/enverLiquibaseDiff", "-format=raw"
        };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testImportExcel() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "impexcel", "-filename=C:/download/PROVA.DB_VER.xls", "-truncate=y"
        };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testImportCSV() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "impcsv", "-filename=C:/download/PROVA.ASSETS.csv", "-truncate=y"
        };
        new Main().parseCommandLine(args);
    }
//    @Test
//    public void testFuffa() throws IOException, CommandLineParsingException, EnverException {
//        new Main();
//    }
    //        Runtime run = Runtime.getRuntime();
////        Process proc = run.exec(new String[]{ "/bin/sh", "-c", "echo 5 | ./prog" });
//        Process p = run.exec(new String[]{ "ssh", "oracle@192.168.1.13", "<<", "EOF",
//        "export ORACLE_HOME=/home/u01/app/oracle/product/11.2.0/db_1",
//        "export ORACLE_SID=etest",
//        "export ORACLE_BASE=/home/u01/app/oracle",
//        "sqlplus / as sysdba @wind.sql", "EOF"});
//        BufferedReader stdin = new BufferedReader(new InputStreamReader(p.getInputStream()));
//        String line = null;
//
//        while ((line = stdin.readLine()) != null) {
//            System.out.println(line);
//        }

//    @Test
    public void testInstallENVER() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "install", "-directory=/home/oracle/enver", "-datafiledir=/home/oracle/oradata/etest/"
        };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testShell() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "shell"
        };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void testExportGRAPH() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "expgraph", "-query=SELECT min_salary, max_salary FROM hr.jobs ORDER BY 1, 2", "-filename=image2", "-mode=cartesian", "-title=Prova"
        };
        new Main().parseCommandLine(args);
    }

//    @Test
    public void a() {
        ServiceLoader<ICompareTaskMode> load = ServiceLoader.load(ICompareTaskMode.class); // TEMPME

        Iterator<ICompareTaskMode> iterator = load.iterator();
        while (iterator.hasNext()) {
            ICompareTaskMode next = iterator.next();

            boolean isGood = next.identify("structure");
            System.out.println(isGood);
        }
    }

//    @Test
    public void testCompareData() throws IOException, CommandLineParsingException, EnverException {
        String[] args = new String[]{
            "compare", "-mode=data", "-comparisonName=comp_prova", "-scanMode=FULL",
            "-schemas=HR, HR", "-tables=REGIONS, REGIONI"
        , "-converge=y"};
        new Main().parseCommandLine(args);
    }
}