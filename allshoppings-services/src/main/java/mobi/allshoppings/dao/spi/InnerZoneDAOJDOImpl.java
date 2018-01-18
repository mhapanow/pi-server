package mobi.allshoppings.dao.spi;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.springframework.util.StringUtils;

import com.inodes.datanucleus.model.Key;

import mobi.allshoppings.dao.InnerZoneDAO;
import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.exception.ASExceptionHelper;
import mobi.allshoppings.model.InnerZone;
import mobi.allshoppings.tools.CollectionFactory;
import mobi.allshoppings.tools.Range;

public class InnerZoneDAOJDOImpl extends GenericDAOJDO<InnerZone> implements InnerZoneDAO {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(InnerZoneDAOJDOImpl.class.getName());

	public InnerZoneDAOJDOImpl() {
		super(InnerZone.class);
	}

	/**
	 * Creates a new unique key for the area, using a random number based in
	 * this current unix time as seed.
	 */
	@Override
	public Key createKey() throws ASException {
		return keyHelper.createNumericUniqueKey(InnerZone.class);
	}
	
	
	@Override
	public List<InnerZone> getUsingEntityIdAndRange(String entityId, byte entityKind, Range range, String order, Map<String, String> attributes, boolean detachable) throws ASException {
		List<InnerZone> returnedObjs = CollectionFactory.createList();

		PersistenceManager pm;
		pm = DAOJDOPersistentManagerFactory.get().getPersistenceManager();

		try{
			Map<String, Object> parameters = CollectionFactory.createMap();
			List<String> declaredParams = CollectionFactory.createList();
			List<String> filters = CollectionFactory.createList();

			Query query = pm.newQuery(clazz);

			// Store id parameter
			if( StringUtils.hasText(entityId) ) {
				declaredParams.add("String entityIdParam");
				filters.add("entityId == entityIdParam");
				parameters.put("entityIdParam", entityId);
			}
			
			// Date parameter
			if( entityKind >= 0) {
				declaredParams.add("Integer entityKindParam");
				filters.add("entityKind >= entityKindParam");
				parameters.put("entityKindParam", entityKind);
			}

			query.declareParameters(toParameterList(declaredParams));
			query.setFilter(toWellParametrizedFilter(filters));

			// Do a counting of records
			if( attributes != null ) {
				query.setResult("count(this)");
				Long count = Long.parseLong(query.executeWithMap(parameters).toString());
				attributes.put("recordCount", String.valueOf(count));
				query.setResult(null);
			}

			// Adds a cursor to the ranged query
			if( range != null ) 
				query.setRange(range.getFrom(), range.getTo());

			// Sets order as required
			if( StringUtils.hasText(order)) query.setOrdering(order);

			@SuppressWarnings("unchecked")
			List<InnerZone> objs = parameters.size() > 0 ? (List<InnerZone>)query.executeWithMap(parameters) : (List<InnerZone>)query.execute();
			if (objs != null) {
				// force to read
				for (InnerZone obj : objs) {
					if( detachable )
						returnedObjs.add(pm.detachCopy(obj));
					else
						returnedObjs.add(obj);
				}
			}

		} catch(Exception e) {
			if(!( e instanceof ASException )) {
				throw ASExceptionHelper.defaultException(e.getMessage(), e);
			} else {
				throw e;
			}
		} finally  {
			pm.close();
		}

		return returnedObjs;

	}

}
