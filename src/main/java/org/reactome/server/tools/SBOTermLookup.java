package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.SpeciesReference;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class SBOTermLookup {

    /**
     * default SBO Terms to use
     */
    private final int defaultCompartment = 290;
    // http://www.ebi.ac.uk/sbo/main/SBO:0000290 physical compartment

    private final int defaultSpecies = 240;
    // http://www.ebi.ac.uk/sbo/main/SBO:0000240 material entity

    ///////////////////////////////////////////////////////////////////////////
    /**
     * Constructor
     */
    SBOTermLookup() {
    }

    /**
     * Sets the appropriate SBOTerm based on the databaseObject
     *
     * @param sbase SBML SBase object on which to set sbo term
     * @param obj DatabaseObject from ReactomeDB
     */
    void setTerm(SBase sbase, DatabaseObject obj) {
        int term = -1;
        if (obj instanceof org.reactome.server.graph.domain.model.Compartment) {
            term = getCompartmentTerm((org.reactome.server.graph.domain.model.Compartment)(obj));
        }
        else if (obj instanceof PhysicalEntity) {
            term = getSpeciesTerm((PhysicalEntity)(obj));
        }
        try {
            sbase.setSBOTerm(term);
        }
        catch (IllegalArgumentException e) {
            // do not set
        }
    }

    /**
     * Sets the sbo term based on the type provided
     * This is used for SBML SpeciesReference objects
     *
     * @param type  String indicating what type of species reference
     * @param sbase SBML SBase object on which to set sbo term
     */
    void setTerm(String type, SBase sbase) {
        int term = getSpeciesReferenceTerm(type);
        try {
            sbase.setSBOTerm(term);
        }
        catch (IllegalArgumentException e) {
            // do not set
        }

    }

    //////////////////////////////////////////////////////////////////////
    // Private functions

    /**
     * Gets the speciesReference sbo based on type
     *
     * @param type  String indicating what type of species reference
     *
     * @return Integer representing the sbo term or -1 if none
     */
    private int getSpeciesReferenceTerm(String type) {
        int term = -1;

        if (type.equals("reactant")) {
            // http://www.ebi.ac.uk/sbo/main/SBO:0000010 reactant
            term = 10;
        }
        else if (type.equals("product")){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000011 product
            term = 11;
        }
        else if (type.equals("catalyst")){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000013 catalyst
            term = 13;
        }
        else if (type.equals("pos_regulator")){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000459 stimulator
            term = 459;
        }
        else if (type.equals("neg_regulator")){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000020 inhibitor
            term = 20;
        }
        return term;
    }

    /**
     * Get the SBML Compartment sbo term
     *
     * @param compartment Compartment from ReactomeDB
     *
     * @return Integer representing the sbo term or -1 if none
     */
    private int getCompartmentTerm(org.reactome.server.graph.domain.model.Compartment compartment) {
        // for now always use physical compartment
        return defaultCompartment;
    }

    /**
     * Get the SBML Species sbo term
     *
     * @param pe PhysicalEntity from ReactomeDB
     *
     * @return Integer representing the sbo term or -1 if none
     */
    private int getSpeciesTerm(PhysicalEntity pe) {
        int term = -1;
        if (pe instanceof SimpleEntity){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000247 simple chemical
            term = 247;
        }
        else if (pe instanceof EntityWithAccessionedSequence || pe instanceof  GenomeEncodedEntity){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000297 protein complex
            term = 297;
        }
        else if (pe instanceof Complex){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000253 non-covalent complex
            term = 253;
        }
        else if (pe instanceof Polymer || pe instanceof OtherEntity){
            term = defaultSpecies; // material entity
        }
        else if (pe instanceof Drug) {
            // http://www.ebi.ac.uk/sbo/main/SBO:0000298 synthetic chemical compound
            term = 298;
        }
        else if (pe instanceof EntitySet){
            term = -1; // this means the sbo term is not set
        }
        else {
            // FIX_Unknown_Physical_Entity
            // here we have encountered a physical entity type that did not exist in the graph database
            // when this code was written
            // See Unknown_PhysicalEntity.md in SBMLExporter/dev directory for details
            System.err.println("Function SBOTermLookup::getSpeciesTerm Encountered unknown PhysicalEntity " + pe.getStId());
        }

        return term;
    }
}
