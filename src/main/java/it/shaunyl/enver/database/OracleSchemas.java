package it.shaunyl.enver.database;

import java.util.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class OracleSchemas {

    public static class Samples {

        public static List<String> list = new ArrayList<String>() {
            {
                add("BI");
                add("HR");
                add("IX");
                add("OE");
                add("PM");
                add("SCOTT");
                add("SH");
            }
        };
    }

    public static class Default {

        public static List<String> list = new ArrayList<String>() {
            {
                add("ANONYMOUS");
                add("APEX_030200");
                add("APEX_PUBLIC_USER");
                add("APPQOSSYS");
                add("CTXSYS");
                add("DBSNMP");
                add("DIP");
                add("EXFSYS");
                add("FLOWS_FILES");
                add("MDDATA");
                add("MDSYS");
                add("MGMT_VIEW");
                add("OLAPSYS");
                add("ORACLE_OCM");
                add("ORDDATA");
                add("ORDPLUGINS");
                add("ORDSYS");
                add("OUTLN");
                add("OWBSYS");
                add("OWBSYS_AUDIT");
                add("SI_INFORMTN_SCHEMA");
                add("SPATIAL_CSW_ADMIN_USR");
                add("SPATIAL_WFS_ADMIN_USR");
                add("SYS");
                add("SYSMAN");
                add("SYSTEM");
                add("WMSYS");
                add("XDB");
                add("XS$NULL");
                add("CSMIG");
            }
        };
    }
}
