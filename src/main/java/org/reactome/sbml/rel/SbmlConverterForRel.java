package org.reactome.sbml.rel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.server.graph.domain.model.DatabaseObject;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.tools.sbml.converter.SbmlConverter;
import org.reactome.server.tools.sbml.data.model.ParticipantDetails;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A customized SBMLConverter to handle objects directly loaded from a RelationDatabase.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
public class SbmlConverterForRel extends SbmlConverter {
    private static final Logger logger = LoggerFactory.getLogger(SbmlConverterForRel.class);
    private MySQLAdaptor dba;
    private InstanceToModelConverter instanceConverter;
    private GKInstance topEvent;

    public SbmlConverterForRel(String targetId) {
        this(targetId, 0); // Default version is 0, meaning it is not defined.
    }

    /**
     * Both stable id and DB_ID are supported in this subclass.
     * @param targetId
     * @param version
     */
    public SbmlConverterForRel(String targetId, Integer version) {
        super(targetId, version);
        instanceConverter = new InstanceToModelConverter();
    }

    public void setDBA(MySQLAdaptor dba) {
        this.dba = dba;
        // Need to have a fake pathway for the superclass
        try {
            GKInstance instance = fetchEvent(targetStId);
            // This may be a reaction
            DatabaseObject databaseObject = instanceConverter.convert(instance);
            if (databaseObject instanceof Pathway)
                pathway = (Pathway) databaseObject;
            topEvent = instance;
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private GKInstance fetchEvent(String eventId) throws Exception {
        GKInstance instance = null;
        if (targetStId.startsWith("R-")) // This is a stable id
            instance = fetchEventForStableId(dba, targetStId);
        else
            instance = dba.fetchInstance(new Long(targetStId));
        if (instance == null)
            throw new IllegalArgumentException("Cannot find an Event with id " + targetStId + " in the provided database.");
        return instance;
    }

    private GKInstance fetchEventForStableId(MySQLAdaptor dba, String stableId) throws Exception {
        Collection<GKInstance> stableIdInst = dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier,
                                                                           ReactomeJavaConstants.identifier,
                                                                           "=",
                                                                           stableId);
        if (stableIdInst == null || stableIdInst.size() == 0)
            return null;
        Collection<GKInstance> events = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Event,
                                                                     ReactomeJavaConstants.stableIdentifier,
                                                                     "=",
                                                                     stableIdInst.iterator().next());
        if (events == null || events.size() == 0)
            return null;
        return events.iterator().next();
    }

    @Override
    public SBMLDocument convert() {
        // Have to make sure this is a dba available
        if (dba == null)
            throw new IllegalStateException("No MySQLAdaptor specified.");
        if (targetStId == null)
            throw new IllegalStateException("No target id specified.");
        return super.convert();
    }

    @Override
    protected Collection<ParticipantDetails> getParticipantDetails() {
        List<ParticipantDetails> rtn = new ArrayList<>();
        try {
            Set<GKInstance> reactions = getReactions();
            Set<GKInstance> pes = new HashSet<>();
            for (GKInstance rxt : reactions)
                pes.addAll(InstanceUtilities.getReactionParticipants(rxt));
            for (GKInstance pe : pes) {
                // Need the attributes for PhysicalEntity
                DatabaseObject databaseObj = instanceConverter.convert(pe);
                if (!(databaseObj instanceof PhysicalEntity)) {
                    throw new IllegalStateException(pe + " cannot be converted into a PhysicalEntity.");
                }
                ParticipantDetails details = new ParticipantDetails();
                PhysicalEntity peObj = (PhysicalEntity) databaseObj;
                details.setPhysicalEntity(peObj);
                instanceConverter.fillInDetails(pe, details);
                rtn.add(details);
            }
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
        return rtn;
    }
    
    private Set<GKInstance> getReactions() throws Exception {
        Set<GKInstance> contained = InstanceUtilities.getContainedEvents(topEvent);
        contained.add(topEvent); // In case event itself is a RLE
        return contained.stream()
                .filter(e -> e.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
                .collect(Collectors.toSet());
    }

    @Override
    protected Collection<ReactionBase> getReactionList() {
        List<ReactionBase> rtn = new ArrayList<>();
        return rtn;
    }

    public static void main(String[] args) throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central_050620",
                                            "root",
                "macmysql01");
        String targetStId = "R-MMU-211119";
        targetStId = "3927939"; // Reactions with entities having inferFrom
        SbmlConverterForRel converter = new SbmlConverterForRel(targetStId);
        converter.setDBA(dba);
        SBMLDocument doc = converter.convert();
        converter.writeToFile("output");
    }

}
