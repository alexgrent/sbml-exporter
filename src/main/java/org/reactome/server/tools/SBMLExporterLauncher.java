
package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;

import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SBMLExporterLauncher {

    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption(  "host",     JSAP.STRING_PARSER, "localhost",     JSAP.REQUIRED,     'h', "host",     "The neo4j host"),
                        new FlaggedOption(  "port",     JSAP.STRING_PARSER, "7474",          JSAP.NOT_REQUIRED, 'b', "port",     "The neo4j port"),
                        new FlaggedOption(  "user",     JSAP.STRING_PARSER, "neo4j",         JSAP.REQUIRED,     'u', "user",     "The neo4j user"),
                        new FlaggedOption(  "password", JSAP.STRING_PARSER, "reactome",      JSAP.REQUIRED,     'p', "password", "The neo4j password"),
                }
        );
        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphQANeo4jConfig.class);

        GeneralService genericService = ReactomeGraphCore.getService(GeneralService.class);
        System.out.println("Database name: " + genericService.getDBName());
        System.out.println("Database version: " + genericService.getDBVersion());

        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);

//        long dbid = 5663205L; // infectious disease
//        long dbid = 167168L;  // HIV transcription termination (pathway no events)
//        long dbid = 180627L; // reaction
//        long dbid = 168275L; // pathway with a single child reaction
//        long dbid = 168255L; // influenza life cycle - which is where my pathway 168275 comes from
//        long dbid = 2978092L; // pathway with a catalysis
//        long dbid = 5619071L; // failed reaction
//        long dbid = 69205L; // black box event
//        long dbid = 392023L; // reaction
        long dbid = 5602410L; // species genome encoded entity

//        long dbid = 453279L;// path with black box
//        long dbid = 76009L; // path with reaction

        int option = 1;

        switch (option) {
            case 1:
                outputFile(dbid, databaseObjectService, genericService.getDBVersion());
                break;
            case 2:
                lookupPaths(databaseObjectService);
                break;
            case 3:
                outputFileNoAnnot(dbid, databaseObjectService, genericService.getDBVersion());
                break;
            case 4:
                showGOProcessBug(databaseObjectService);
                break;

        }
    }

    private static void lookupPaths(DatabaseObjectService databaseObjectService){
        Species homoSapiens = (Species) databaseObjectService.findByIdNoRelations(48887L);
        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);
        int count = 0;
        int total = 0;
        for (Pathway path : schemaService.getByClass(Pathway.class, homoSapiens)) {
            List<Event> events = path.getHasEvent();
            if (events != null && events.size() == 1) {
                for (Event e : events) {
                    if (e instanceof ReactionLikeEvent) {
                        if (((ReactionLikeEvent) e).getInput() != null) {
                            for (PhysicalEntity pe : ((ReactionLikeEvent) e).getInput()) {
                                if (pe instanceof Polymer) {
                                    System.out.println("Pathway " + path.getDbId() + " matches");
                                    count++;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            total++;
        }
        System.out.println("Found " + count + " of " + total);


    }

    private static void outputFile(long dbid, DatabaseObjectService databaseObjectService, Integer dbVersion){
        Event pathway = (Event) databaseObjectService.findById(dbid);
        @SuppressWarnings("ConstantConditions") WriteSBML sbml = new WriteSBML((Pathway)(pathway));
        sbml.setDBVersion(dbVersion);
        sbml.createModel();
        sbml.toStdOut();
        sbml.toFile("out.xml");
    }

    private static void outputFileNoAnnot(long dbid, DatabaseObjectService databaseObjectService, Integer dbVersion){
        Event pathway = (Event) databaseObjectService.findById(dbid);
        @SuppressWarnings("ConstantConditions") WriteSBML sbml = new WriteSBML((Pathway)(pathway));
        sbml.setDBVersion(dbVersion);
        sbml.setAnnotationFlag(false);
        sbml.createModel();
        sbml.toStdOut();
        sbml.toFile("out.xml");
    }

    private static void showGOProcessBug(DatabaseObjectService databaseObjectService){
        Species homoSapiens = (Species) databaseObjectService.findByIdNoRelations(48887L);
        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);
        int count = 0;
        int total = 0;
        for (Pathway path : schemaService.getByClass(Pathway.class, homoSapiens)) {
            List<Event> events = path.getHasEvent();
            if (events != null && events.size() == 1) {
                for (Event e : events) {
                    if (e instanceof Reaction) {
                        List<CatalystActivity> cats = ((Reaction) e).getCatalystActivity();
                        if (cats != null) {
                            GO_MolecularFunction goterms = cats.get(0).getActivity();
                            if (goterms == null) {
                                System.out.println("Catalyst activity " + path.getDbId() + " has NULL GOBiologicalProcess");
                            }
                            else{
                                System.out.println("Catalyst activity  " + path.getDbId() + " has " + goterms.getName() + " GOBiologicalProcess");
                            }
                            count++;
                            break;
                        }
                    }
                }
            }
            total++;
        }
        System.out.println("Found " + count + " of " + total);


    }


}

