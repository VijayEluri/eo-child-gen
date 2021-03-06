package com.bc.merci.geochildgen;

import com.bc.childgen.ChildGenException;
import com.bc.util.test.BcTestUtils;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

public class GeoChildGenAcceptanceTest extends TestCase {

    private File propertiesFile;
    private File testDir;

    public void testSubsetFrom_AATSR_copyProduct() throws IOException, SQLException, ChildGenException, ParseException {
        if (isIOTestsSuppressed()) {
            System.err.println("testSubsetFrom_AATSR_copyProduct() suppressed");
            return;
        }

        final File aatsrFile = getTestDataFile("ATS_TOA_1PPTOM20070110_192521_000000822054_00328_25432_0001.N1");

        final File propsFile = createPropertiesFile("polygon((-170 -68,-159 -70,-164 -74,-176 -71,-170 -68))");
        final CmdLineParams cmdLineParams = new CmdLineParams();
        cmdLineParams.setPropertiesFileName(propsFile.getAbsolutePath());
        cmdLineParams.setOutputDirName(testDir.getAbsolutePath());
        cmdLineParams.addInputFileName(aatsrFile.getAbsolutePath());

        GeoChildGen.run(cmdLineParams);

        assertTargetFileCreated(aatsrFile.getName(), 10365697L);
    }

    public void testSubsetFrom_AATSR_createSubset() throws IOException, SQLException, ChildGenException, ParseException {
        if (isIOTestsSuppressed()) {
            System.err.println("testSubsetFrom_AATSR_createSubset() suppressed");
            return;
        }

        final File aatsrFile = getTestDataFile("ATS_TOA_1PPTOM20070110_192521_000000822054_00328_25432_0001.N1");

        final File propsFile = createPropertiesFile("polygon((-170 -68,-159 -70,-164 -74,-176 -71,-170 -68))");
        final CmdLineParams cmdLineParams = new CmdLineParams();
        cmdLineParams.setPropertiesFileName(propsFile.getAbsolutePath());
        cmdLineParams.setOutputDirName(testDir.getAbsolutePath());
        cmdLineParams.setCreateChildOption(true);
        cmdLineParams.addInputFileName(aatsrFile.getAbsolutePath());

        GeoChildGen.run(cmdLineParams);

        assertTargetFileCreated("ATS_TOA_1PPMAP20070110_192521_000000772054_00328_25432_0001.N1", 9738161L);
    }

    public void testSubsetFrom_ATSR1_double_intersection_splitGeometries() throws IOException, SQLException, ChildGenException, ParseException {
        if (isIOTestsSuppressed()) {
            System.err.println("testSubsetFrom_ATSR1_double_intersection_splitGeometries() suppressed");
            return;
        }

        final File ats1File = getTestDataFile("AT1_NR__2PTRAL19930614_131152_000000004013_00338_10002_0000.E1");
        final File propsFile = createPropertiesFile("polygon((-52 10,-45 10,-45 -4,-55 -4,-55 -3,-47 -3,-47 9,-52 9,-52 10))");
        final CmdLineParams cmdLineParams = new CmdLineParams();
        cmdLineParams.setPropertiesFileName(propsFile.getAbsolutePath());
        cmdLineParams.setOutputDirName(testDir.getAbsolutePath());
        cmdLineParams.setCreateChildOption(true);
        cmdLineParams.addInputFileName(ats1File.getAbsolutePath());

        GeoChildGen.run(cmdLineParams);

        assertTargetFileCreated("AT1_NR__2PTMAP19930614_135859_000000484013_00338_10002_0001.E1", 1062953L);
        assertTargetFileCreated("AT1_NR__2PTMAP19930614_140240_000000484013_00338_10002_0001.E1", 1062953L);
    }

    public void testSubsetFrom_ATSR1_double_intersection_mergeGeometries() throws IOException, SQLException, ChildGenException, ParseException {
        if (isIOTestsSuppressed()) {
            System.err.println("testSubsetFrom_ATSR1_double_intersection_mergeGeometries() suppressed");
            return;
        }

        final File ats1File = getTestDataFile("AT1_NR__2PTRAL19930614_131152_000000004013_00338_10002_0000.E1");
        final File propsFile = createPropertiesFile("polygon((-52 10,-45 10,-45 -4,-55 -4,-55 -3,-47 -3,-47 9,-52 9,-52 10))");
        final CmdLineParams cmdLineParams = new CmdLineParams();
        cmdLineParams.setPropertiesFileName(propsFile.getAbsolutePath());
        cmdLineParams.setOutputDirName(testDir.getAbsolutePath());
        cmdLineParams.setCreateChildOption(true);
        cmdLineParams.addInputFileName(ats1File.getAbsolutePath());
        cmdLineParams.setMergeIntersections(true);

        GeoChildGen.run(cmdLineParams);

        assertTargetFileCreated("AT1_NR__2PTMAP19930614_135859_000002694013_00338_10002_0001.E1", 5891739L);
    }

    ////////////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ////////////////////////////////////////////////////////////////////////////////


    @Override
    protected void setUp() throws Exception {
        testDir = new File("testDir");
        if (!testDir.mkdirs()) {
            fail("unable to create test directory: " + testDir.getAbsolutePath());
        }
    }

    @Override
    protected void tearDown() {
        if (propertiesFile != null) {
            if (!propertiesFile.delete()) {
                fail("unable to delete test file: " + propertiesFile.getAbsolutePath());
            }
        }

        if (testDir != null) {
            TestUtils.deleteFileTree(testDir);
            if (testDir.isDirectory()) {
                fail("unable to delete test directory - check your file streams!");
            }
        }
    }

    // @todo 3 tb/** this is also a common check - move to testing framework if exists - tb 2011-11-17
    private static boolean isIOTestsSuppressed() {
        return "true".equals(System.getProperty("noiotests"));
    }

    private void assertTargetFileCreated(String name, long expected) {
        final File targetFile = new File(testDir, name);
        assertTrue(targetFile.isFile());
        assertEquals(expected, targetFile.length());
    }

    private static File getTestDataFile(String filename) throws IOException {
        final String testDataPath = BcTestUtils.getPropertyFromResource("/com/bc/merci/geochildgen/testData.properties", "testDataPath");
        final File testDataFile = new File(testDataPath, filename);
        if (!testDataFile.isFile()) {
            fail("test data file not found: " + testDataFile.getAbsolutePath());
        }
        return testDataFile;
    }

    private File createPropertiesFile(String geometryWKT) throws IOException {
        propertiesFile = new File("test_geochildgen.properties");
        if (!propertiesFile.createNewFile()) {
            fail("unable to create test file: " + propertiesFile.getAbsolutePath());
        }

        final FileWriter writer = new FileWriter(propertiesFile);
        writer.write("childProductOriginatorId = map\n");
        writer.write("geometry[0] = ");
        writer.write(geometryWKT);
        writer.flush();
        writer.close();

        return propertiesFile;
    }
}


