package mobi.allshoppings.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
	
	private static final String DEST_DIR = "/tmp/missing-aphotspot";
	
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
		File file = new File(DEST_DIR);
		if(file.exists()) {
			if(file.isDirectory()) clearRun(file);
			else throw ASExceptionHelper.defaultException("There is a file named " +DEST_DIR, null);
		}
		Map<String, APHotspot> hotspots = CollectionFactory.createMap();
		Calendar cal = Calendar.getInstance();
		DumperHelper<APHotspot> dumper = new DumpFactory<APHotspot>().build(null, APHotspot.class);
		dumper.setFilter(hostname);
		Iterator<APHotspot> hotspotIterator = dumper.iterator(fromDate, toDate);
		hotspots.clear();
		while(hotspotIterator.hasNext()) {
			APHotspot hotspot = hotspotIterator.next();
			hotspots.put("APHotspot(\"" +hotspot.getIdentifier() +"\")", hotspot);
		}
		
		// Creates JDO Connection
		PersistenceManager pm;
		pm = DAOJDOPersistentManagerFactory.get().getPersistenceManager();
		pm.currentTransaction().begin();
		dumper.dispose();
		
		// Gets Native connection from JDO
		JDOConnection jdoConn = pm.getDataStoreConnection();
		DB db = (DB)jdoConn.getNativeConnection();
		for(String collection : options.valueOf(COLLECTIONS_PARAM).toString().split(",")) {
			collection = collection.trim();
			LOG.log(Level.INFO, "Searching for missing data in collection " +collection);
			BasicDBObject query = new BasicDBObject();
			query.put("hostname", hostname);
			query.put("firstSeen", new BasicDBObject("$gte", fromDate));
			query.put("lastSeen", new BasicDBObject("$lte", toDate));
			DBCursor c = db.getCollection(collection).find(query);
			LOG.log(Level.INFO, "Found " +c.count() +" entries in collection " +collection);
			c.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
			Iterator<DBObject> dbDumpIt = c.iterator();
			int count = 0;
			while(dbDumpIt.hasNext()) {
				DBObject obj = dbDumpIt.next();
				if(hotspots.get(obj.get("_id").toString()) == null) {
					cal.setTime((Date) obj.get("creationDateTime"));// TODO asegurar que los digitos son a 2
					file = new File(DEST_DIR +"/" +cal.get(Calendar.YEAR) +"/" +(Calendar.MONTH +1) +"/"
							+cal.get(Calendar.DAY_OF_MONTH) +"/" +cal.get(Calendar.HOUR_OF_DAY) +"/"
							+hostname +".json");
					file.getParentFile().mkdirs();
					PrintWriter writer = null;
					try {
						writer = new PrintWriter(new FileOutputStream(file, true));
						writer.println(obj);
						writer.flush();
						writer.close();
						count++;
					} catch(IOException e) {
						LOG.log(Level.INFO, "Problem creataing dump file " +file, e);
					}
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
		}
	}
	
	private static void clearRun(File dir) {
		File[] subFs = dir.listFiles();
		if(subFs == null) return;
		for(File f : subFs) {
			if(f.isDirectory()) clearRun(f);
			else f.delete();
		}
		dir.delete();
	}
	
}