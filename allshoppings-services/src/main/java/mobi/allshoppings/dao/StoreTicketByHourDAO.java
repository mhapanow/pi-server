package mobi.allshoppings.dao;


import java.util.List;

import com.inodes.datanucleus.model.Key;

import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.model.StoreTicketByHour;
import mobi.allshoppings.tools.Range;

/**
 * Describes a DAO for the Store Tickey By Hour model.
 * @author Matias Hapanowicz
 * @author <a href="mailto:ignacio@getin.mx" >Manuel "Nachintoch" Castillo</a>
 * @version 1.0, 
 * @since Allshoppings
 */
public interface StoreTicketByHourDAO extends GenericDAO<StoreTicketByHour> {

	/**
	 * Creates a DB key for data.
	 * @return Key - the key to use within the DB.
	 * @throws ASException - If something goes wrong
	 */
	Key createKey() throws ASException;
	
	/**
	 * Retrieves Store Tickets By Hour using a Store ID, and Date.
	 * @param storeId - The store whose Tickets By Hour are desired.
	 * @param date - The date to fetch tickets from.
	 * @param fromHour - The initial hour to fetch.
	 * @param toHour - The final hour to fetch (exclusive).
	 * @param range - ???
	 * @param order - The order for the tickets.
	 * @param detachable - ???
	 * @return List&lt;SticketByHour&gt; - A list with the results of the query.
	 * @throws ASException - If not found or else.
	 */
	List<StoreTicketByHour> getUsingStoreIdAndDateAndRange(String storeId, String date, String fromHour,
			String toHour, Range range, String order, boolean detachable) throws ASException;
	
	/**
	 * Retreives Store Tickets By Hour using the belonging Store ID, the date of the sales and an specific
	 * sale hour.
	 * @param storeId - The Store whose Tickets By Hour are desired.
	 * @param date - The date to fetch tickets from.
	 * @param hour - The hour to fetch tickets from.
	 * @param detachable - ???
	 * @return List&lt;SticketByHour&gt; - A list with the results of the query.
	 * @throws ASException - If not found or else.
	 */
	StoreTicketByHour getUsingStoreIdAndDateAndHour(String storeId, String date, String hour,
			boolean detachable) throws ASException;
	
}//Store Ticket By Hour DAO
