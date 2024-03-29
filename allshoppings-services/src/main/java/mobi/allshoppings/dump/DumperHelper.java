package mobi.allshoppings.dump;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.model.interfaces.ModelKey;

public interface DumperHelper<T extends ModelKey> {

	/**
	 * Registers a new plugin
	 * 
	 * @param plugin
	 *            The plugin to register
	 */
	void registerPlugin(DumperPlugin<ModelKey> plugin);

	/**
	 * Unregisters a registered plugin
	 * 
	 * @param plugin
	 *            The plugin to unregister
	 */
	void unregisterPlugin(DumperPlugin<ModelKey> plugin);

	/**
	 * Registers a new Cloud File Manager
	 * 
	 * @param cloudFileManager
	 *            The Cloud File Manager to register
	 */
	void registerCloudFileManager(CloudFileManager cloudFileManager);

	/**
	 * Unregisters the previous cloud file manager
	 */
	void unregisterCloudFileManager();
	
	/**
	 * Triggers a flush to the registered Cloud File Manager
	 * 
	 * @throws ASException
	 */
	void flush() throws ASException;

	/**
	 * Triggers a start prefetch
	 * 
	 * @throws ASException
	 */
	void startPrefetch(boolean expectNotFound) throws ASException;

	/**
	 * Applies all the plugins registered for an object before dumping the
	 * object
	 * 
	 * @param obj
	 *            The object to be affected by the plugins
	 * @throws ASException
	 */
	void applyPreDumpPlugins(T obj) throws ASException;

	/**
	 * Applies all the plugins registered for an object after dumping the
	 * object
	 * 
	 * @param obj
	 *            The object to be affected by the plugins
	 * @throws ASException
	 */
	void applyPostDumpPlugins(T obj) throws ASException;

	/**
	 * Specifies a file name filter
	 * 
	 * @param filter
	 *            The specified file name filter
	 */
	void setFilter(String filter);

	/**
	 * Specifies a time frame for rolling files
	 * 
	 * @param timeFrame
	 *            The time frame to specify
	 */
	void setTimeFrame(long timeFrame);
	
	/**
	 * Specifies if the working directory is temporary
	 * 
	 * @param tmpDir
	 *            true if the directory is temporary
	 */
	public void setTmpDir(boolean tmpDir);
	
	/**
	 * Obtains the current file name filter
	 * 
	 * @return The current file name filter
	 */
	String getFilter();

	/**
	 * Dumps a ModelKey database to a set of files
	 * 
	 * @param fromDate
	 *            From which date
	 * @param toDate
	 *            To which date
	 * @param deleteAfterDump
	 *            Do I have to delete the object after successful dump?
	 * @throws ASException
	 */
	void dumpModelKey(String collection, Date fromDate, Date toDate, boolean deleteAfterDump, boolean moveCollectionBeforeDump) throws ASException;

	/**
	 * Special dump from APDVisit
	 * 
	 * @param fromDate
	 *            From which date
	 * @param toDate
	 *            To which date
	 * @param deleteAfterDump
	 *            Do I have to delete the object after successful dump?
	 * @throws ASException
	 */
	void dumpAPDVisit(String collection, Date fromDate, Date toDate, boolean deleteAfterDump, boolean moveCollectionBeforeDump) throws ASException;
	
	/**
	 * Dumps a single ModelKey object
	 * 
	 * @param baseDir
	 *            The base directory to write
	 * @param obj
	 *            DeviceLocationHistory Object to dump
	 * @throws IOException
	 */
	void dump(T obj) throws ASException;

	/**
	 * Obtains a list of potential names for a date
	 * 
	 * @param fromDate
	 *            The specified date
	 * @return A fully formed list of names
	 */
	List<String> getMultipleFileOptions(Date date);

	/**
	 * Obtains a list of potential names for a date
	 * 
	 * @param fromDate
	 *            The specified date
	 * @return A fully formed list of names
	 */
	List<String> getMultipleNameOptions(Date date);

	/**
	 * Resolves the file name for a dump file based in its date and time
	 * 
	 * @param forDate
	 *            The date and time to calculate the name from
	 * @param element
	 *            The element to resolve from
	 * @return A fully formed file name for the dump file
	 */
	String resolveDumpFileName(Date forDate, T element);

	/**
	 * Gets an iterator with all the saved entities in a date range
	 * 
	 * @param fromDate
	 *            Date and time from which to retrieve entities
	 * @param toDate
	 *            Date and time to which to retrieve entities
	 * @return An iterator with all the selected records
	 */
	Iterator<T> iterator(Date fromDate, Date toDate, boolean notFoundExpected);

	/**
	 * Gets an iterator with all the saved entities in a date range, in its
	 * String representation
	 * 
	 * @param fromDate
	 *            Date and time from which to retrieve entities
	 * @param toDate
	 *            Date and time to which to retrieve entities
	 * @return An iterator with all the selected records
	 */
	Iterator<String> stringIterator(Date fromDate, Date toDate, boolean notFoundExpected);

	/**
	 * Gets an iterator with all the saved entities in a date range, in its
	 * JSON representation
	 * 
	 * @param fromDate
	 *            Date and time from which to retrieve entities
	 * @param toDate
	 *            Date and time to which to retrieve entities
	 * @return An iterator with all the selected records
	 */
	Iterator<JSONObject> jsonIterator(Date fromDate, Date toDate, boolean notFoundExpected);

	/**
	 * Adds a new name discriminator to the name helper
	 * 
	 * @param discriminator
	 *            The field that will be used as discriminator
	 */
	void registerFileNameResolver(DumperFileNameResolver<ModelKey> discriminator);

	/**
	 * Removes a name discriminator from the name helper
	 */
	void unregisterFileNameResolver();
	
	/**
	 * Finalizes the object
	 */
	void dispose();
	

}