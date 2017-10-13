package mobi.allshoppings.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.datastore.JDOConnection;

import org.springframework.context.ApplicationContext;

import com.inodes.util.CollectionFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import mobi.allshoppings.dao.spi.DAOJDOPersistentManagerFactory;
import mobi.allshoppings.dump.DumperHelper;
import mobi.allshoppings.dump.impl.DumpFactory;
import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.exception.ASExceptionHelper;
import mobi.allshoppings.model.APHotspot;

public class HotspotComparer extends AbstractCLI {

	private static final String FROM_DATE_PARAM = "fromDate";
	private static final String TO_DATE_PARAM = "toDate";
	private static final String HOSTNAME = "hostname";
	private static final String COLLECTIONS_PARAM = "collections";
	
	private static final Logger LOG = Logger.getLogger(HotspotComparer.class.getName());
	
	public static void setApplicationContext(ApplicationContext ctx) {
		context = ctx;
	}

	public static OptionParser buildOptionParser(OptionParser base) {
		if( base == null ) parser = new OptionParser();
		else parser = base;
		parser.accepts(FROM_DATE_PARAM, "Export from date (yyyy-MM-dd)").withRequiredArg()
				.ofType( String.class );
		parser.accepts(TO_DATE_PARAM, "Export to date (yyyy-MM-dd)").withRequiredArg()
				.ofType( String.class );
		parser.accepts(HOSTNAME, "APHostname").withRequiredArg().ofType( String.class );
		parser.accepts(COLLECTIONS_PARAM, "Mongo table names (sepparated by comma) to search for missing "
				+ "data in Mordor").withRequiredArg().ofType(String.class);
		return parser;
	}
	
	public static void main(String[] args) throws ASException {
		OptionSet options = parser.parse(args);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date fromDate, toDate;
		String hostname;
		
		try {
			if(!options.has(HOSTNAME))
				throw ASExceptionHelper.defaultException("hostname is required", null);
			if(!options.has(COLLECTIONS_PARAM))
				throw ASExceptionHelper.defaultException("A collecion is required", null);
			hostname = options.valueOf(HOSTNAME).toString();
			fromDate = options.has(FROM_DATE_PARAM) ?
					sdf.parse(options.valueOf(FROM_DATE_PARAM).toString()) : new Date();
			toDate = options.has(TO_DATE_PARAM) ?
					sdf.parse(options.valueOf(TO_DATE_PARAM).toString()) : fromDate;
		} catch(ParseException e) {
			throw ASExceptionHelper.defaultException(e.getMessage(), e);
		}
		Map<String, APHotspot> hotspots = CollectionFactory.createMap();
		
		// Creates JDO Connection
		PersistenceManager pm;
		pm = DAOJDOPersistentManagerFactory.get().getPersistenceManager();
		pm.currentTransaction().begin();
		
		// Gets Native connection from JDO
		JDOConnection jdoConn = pm.getDataStoreConnection();
		DB db = (DB)jdoConn.getNativeConnection();
		for(String collection : options.valueOf(COLLECTIONS_PARAM).toString().split(",")) {
			collection = collection.trim();
			LOG.log(Level.INFO, "Searching for missing data in collection " +collection);
			DumperHelper<APHotspot> dumper = new DumpFactory<APHotspot>().build(null, APHotspot.class);
			dumper.setFilter(hostname);
			DBObject obj;
			BasicDBObject query = new BasicDBObject();
			query.put("hostname", hostname);
			/*fromDate = new DateTime(fromDate, DateTimeZone.getDefault()).toDate();
			toDate = new DateTime(toDate, DateTimeZone.getDefault()).toDate();*/
			query.put("firstSeen", new BasicDBObject("$gte", fromDate));
			query.put("lastSeen", new BasicDBObject("$lte", toDate));
			DBCursor c = db.getCollection(collection).find(query);
			LOG.log(Level.INFO, "Found " +c.count() +" entries in collection " +collection);
			c.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
			Iterator<DBObject> dbDumpIt = c.iterator();
			File file = new File("/tmp/aphotspot/" +collection +".json");
			file.getParentFile().mkdirs();
			PrintWriter writer = null;
			int count = 0;
			try {
				writer = new PrintWriter(file);
			} catch(IOException e) {
				LOG.log(Level.INFO, "Problem creataing dump file " +file, e);
				continue;
			} 
			Iterator<APHotspot> hotspotIterator = dumper.iterator(fromDate, toDate);
			hotspots.clear();
			while(hotspotIterator.hasNext()) {
				APHotspot hotspot = hotspotIterator.next();
				hotspots.put("APHotspot(\\\"" +hotspot.getIdentifier() +"\\\")", hotspot);
			} while(dbDumpIt.hasNext()) {
				obj = dbDumpIt.next();
				/*if(!dumper.iterator(new DateTime(obj.get("firstSeen")).toDate(),
						new DateTime(obj.get("lastSeen")).toDate()).hasNext()) {*/
				if(hotspots.get(obj.get("_id")) == null) {
					writer.println(obj);
					count++;
				}
			}
			c.close();
			if(count > 0) {
				LOG.log(Level.INFO, "Found " +count +" entries missing in mordor in collection " +collection
						+" from dates " +fromDate +" - " +toDate +" and hostname " +hostname);
			} else {
				LOG.log(Level.INFO, "No report from hotspot " +hostname +" is missing in mordor"
						+" from dates " +fromDate +" - " +toDate +" in collection " +collection
						+". Great job! :)");
			}
			writer.flush();
			writer.close();
			dumper.dispose();
		}
				
	}
	
}
