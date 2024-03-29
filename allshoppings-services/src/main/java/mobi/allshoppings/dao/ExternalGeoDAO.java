package mobi.allshoppings.dao;


import java.util.List;

import com.inodes.datanucleus.model.Key;

import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.model.ExternalGeo;
import mobi.allshoppings.tools.Range;
import mobi.allshoppings.tx.PersistenceProvider;

public interface ExternalGeoDAO extends GenericDAO<ExternalGeo> {

	Key createKey(ExternalGeo obj) throws ASException;
	String getHash(Float lat, Float lon, String period, String entityId, Integer entityKind, Integer type);
	int getType(int checkinType, int hour);
	
	List<ExternalGeo> getUsingVenueAndPeriod(PersistenceProvider pp, String venue, String period, Range range, String order, boolean detachable) throws ASException;
	String getVenuesAndPeriods(PersistenceProvider pp, String venue, String period) throws ASException;
	
	List<ExternalGeo> getUsingEntityIdAndPeriod(PersistenceProvider pp, String entityId, Integer entityKind, Integer type, String period, Range range, String order, boolean detachable) throws ASException;
	void deleteUsingEntityIdAndPeriod(PersistenceProvider pp, List<String> entityId, Integer entityKind, Integer type, String period) throws ASException;
	String getEntityIdAndPeriods(PersistenceProvider pp, String entityId, Integer entityKind, Integer type, String period) throws ASException;
}
