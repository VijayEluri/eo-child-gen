package com.bc.merci.geochildgen;

import com.bc.childgen.ChildGenException;
import com.bc.util.bean.BeanUtils;
import com.bc.util.geom.Geometry;
import com.bc.util.geom.GeometryParser;
import com.bc.util.sql.BcDatabase;
import com.bc.util.sql.DataSourceConfig;
import com.bc.util.sql.SqlUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class GeoChildGen {

    /**
     * @param params command line parameter
     * @throws IOException       on disk access errors
     * @throws ParseException    on malformed parameters
     * @throws ChildGenException on any other failure
     * @throws SQLException      on database access failures
     */
    public static void run(CmdLineParams params) throws IOException, ParseException, SQLException, ChildGenException {
        final String outputDirName = params.getOutputDirName();
        final File outputDir = new File(outputDirName);
        if (!(outputDir.isDirectory() || outputDir.mkdirs())) {
            System.err.println("Output directory '" + outputDir.getAbsolutePath() + "' does not exist or could not be created");
            return;
        }

        final GeoChildGenProperties config = readProperties(params.getPropertiesFileName());
        final List<Geometry> geometryList = getGeometryList(params, config);
        final List<String> inputFileList = getInputFileList(params);

        final FileProcessor fileProcessor = new FileProcessor(params, outputDir, config, geometryList);
        for (final String inputFileName : inputFileList) {
            try {
                fileProcessor.process(inputFileName);
            } catch (IOException e) {
                System.err.println("ERROR creating subset of: " + inputFileName);
                System.err.println(e.getMessage());
            } catch (ChildGenException e) {
                System.err.println("ERROR creating subset of: " + inputFileName);
                System.err.println(e.getMessage());
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    // package access for testing purpose only tb 2011-11-17
    static void printUsageTo(OutputStream out) {
        final PrintStream stream = new PrintStream(out);

        stream.print("geochildgen version ");
        stream.println(CmdLineConstants.VERSION_STRING);
        stream.println();
        stream.print("Usage: geochildgen [");
        stream.print(CmdLineConstants.GEO_PROPS_OPTION);
        stream.print(" <propertiesFile>] ");
        stream.print("[");
        stream.print(CmdLineConstants.DB_PROPS_OPTION);
        stream.println(" <propertiesFile>]");
        stream.print("       [");
        stream.print(CmdLineConstants.SITE_CAT_OPTION);
        stream.print(" <cat_a, cat_b, ...>] [");
        stream.print(CmdLineConstants.CREATE_CHILD_OPTION);
        stream.println("]");
        stream.print("       [");
        stream.print(CmdLineConstants.OUT_DIR_OPTION);
        stream.print(" <outputDir>] [");
        stream.print(CmdLineConstants.MERGE_INTERSECTIONS_OPTION);
        stream.print("] [");
        stream.print(CmdLineConstants.VERBOSE_OPTION);
        stream.print("] [");
        stream.print(CmdLineConstants.FILES_FROM_OPTION);
        stream.println("] <inputFile>...");
        stream.println();
        stream.println("Options:");
        stream.print("    ");
        stream.print(CmdLineConstants.GEO_PROPS_OPTION);
        stream.println(" - select to use geometry properties");
        stream.println("         from <propertiesFile>");
        stream.print("    ");
        stream.print(CmdLineConstants.DB_PROPS_OPTION);
        stream.println(" - select to use Site geometries from database");
        stream.println("         defined in <propertiesFile>");
        stream.print("    ");
        stream.print(CmdLineConstants.SITE_CAT_OPTION);
        stream.println(" - define site categories to be used as comma separated");
        stream.println("         list of category name. Use together with -d option.");
        stream.print("    ");
        stream.print(CmdLineConstants.CREATE_CHILD_OPTION);
        stream.println(" - select to create a child product in <outputDir>.");
        stream.println("         If not set, intersecting products are copied.");
        stream.print("    ");
        stream.print(CmdLineConstants.OUT_DIR_OPTION);
        stream.println(" - defines the <outputDir>.");
        stream.println("         If not set, current directory is used.");
        stream.print("    ");
        stream.print(CmdLineConstants.MERGE_INTERSECTIONS_OPTION);
        stream.println(" - select to merge geometries in case of multiple intersections.");
        stream.println("         If not set a subset will be generated for each intersection");
        stream.print("    ");
        stream.print(CmdLineConstants.FILES_FROM_OPTION);
        stream.println(" - switch to use a textfile with filepaths as input.");
        stream.println("         If set, <inputFile> must be a textfile. Please refer to");
        stream.println("         'inputFiles_example.txt' for the format definition.");
        stream.print("    ");
        stream.print(CmdLineConstants.VERBOSE_OPTION);
        stream.println(" - set program to verbose logging.");
    }

    /**
     * @param propertiesFileName the name of the properties file
     * @return the properties read from file
     * @throws IOException    on disk access failures
     * @throws ParseException
     */
    private static GeoChildGenProperties readProperties(final String propertiesFileName) throws IOException, ParseException {
        final File propertiesFile = new File(propertiesFileName);

        if (!propertiesFile.isFile()) {
            System.err.println("Properties file '" + propertiesFileName + "' does not exist");
        }

        final GeoChildGenProperties config = new GeoChildGenProperties();
        BeanUtils.setBeanProperties(config, propertiesFile);

        return config;
    }

    private static List<Geometry> getGeometryFromDatabase(DataSourceConfig dataSourceConfig) throws SQLException {
        final DataSource datasource = SqlUtils.createDatasource(dataSourceConfig);
        System.out.println("Accessing database:");
        System.out.println("  url : " + dataSourceConfig.getUrl());
        System.out.println("  user: " + dataSourceConfig.getUsername());

        final BcDatabase db = new BcDatabase(datasource);
        final List<Geometry> geometries = GeoChildGenUtils.getGeometriesFromDb(db, new String[0]);

        System.out.println("Read '" + geometries.size() + "' geometry/-ies from database");

        return geometries;
    }

    /**
     * @param geometries an array of geometry strings as WKT
     * @throws ParseException
     */
    private static List<Geometry> parseGeometryProperties(final String[] geometries) throws ParseException {
        final List<Geometry> geometryList = new ArrayList<Geometry>();
        final GeometryParser parser = new GeometryParser();

        for (int i = 0; i < geometries.length; ++i) {
            geometryList.add(parser.parseWKT(geometries[i]));
        }

        System.out.println("Read '" + geometries.length + "' geometry/-ies from file");

        return geometryList;
    }

    private static List<Geometry> getGeometryList(CmdLineParams params, GeoChildGenProperties config) throws SQLException, ParseException {
        final List<Geometry> geometryList;
        if (params.isDatabaseUsed()) {
            geometryList = getGeometryFromDatabase(config.getDataSourceConfig());
        } else {
            geometryList = parseGeometryProperties(config.getGeometry());
        }
        return geometryList;
    }

    private static List<String> getInputFileList(CmdLineParams params) throws IOException {
        if (params.isInputFromFile()) {
            final String inputSource = params.getInputSource();
            final List<String> inputExpressions = readInputFile(inputSource);

            final FileTreeExpander fileTreeExpander = new FileTreeExpander();

            final ArrayList<String> result = new ArrayList<String>();
            for (Iterator<String> iterator = inputExpressions.iterator(); iterator.hasNext(); ) {
                final List<File> sublist = fileTreeExpander.expand(iterator.next());
                for (Iterator<File> fileIterator = sublist.iterator(); fileIterator.hasNext(); ) {
                    result.add(fileIterator.next().getAbsolutePath());
                }
            }
            return result;
        } else {
            return params.getInputFileNameList();
        }
    }

    private static List<String> readInputFile(String inputSource) throws IOException {
        List<String> result;
        FileInputStream inputStream = null;

        try {
            final File inputSourceFile = new File(inputSource);
            final InputFileParser inputFileParser = new InputFileParser();

            inputStream = new FileInputStream(inputSourceFile);
            result = inputFileParser.parse(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return result;
    }
}
