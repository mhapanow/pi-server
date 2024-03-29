package mobi.allshoppings.dao.spi;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.springframework.util.StringUtils;

import com.inodes.datanucleus.model.Key;

import mobi.allshoppings.dao.AuditLogDAO;
import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.exception.ASExceptionHelper;
import mobi.allshoppings.model.AuditLog;
import mobi.allshoppings.tools.CollectionFactory;
import mobi.allshoppings.tools.Range;

public class AuditLogDAOJDOImpl extends GenericDAOJDO<AuditLog> implements AuditLogDAO {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(AuditLogDAOJDOImpl.class.getName());

	public AuditLogDAOJDOImpl() {
		super(AuditLog.class);
	}

	/**
	 * Creates a new unique key for the area, using a random number based in
	 * this current unix time as seed.
	 */
	@Override
	public Key createKey() throws ASException {
		return keyHelper.createStringUniqueKey(AuditLog.class);
	}

	@Override
	public List<AuditLog> getUsingUserAndTypeAndRange(String userId, Integer eventType, Range range, String order, Map<String, String> attributes, boolean detachable) throws ASException {
		List<AuditLog> returnedObjs = CollectionFactory.createList();

		PersistenceManager pm;
		pm = DAOJDOPersistentManagerFactory.get().getPersistenceManager();

		try{
			Map<String, Object> parameters = CollectionFactory.createMap();
			List<String> declaredParams = CollectionFactory.createList();
			List<String> filters = CollectionFactory.createList();

			Query query = pm.newQuery(clazz);

			// Store id parameter
			if( StringUtils.hasText(userId) ) {
				declaredParams.add("String userIdParam");
				filters.add("userId == userIdParam");
				parameters.put("userIdParam", userId);
			}
			
			// Date parameter
			if( eventType != null ) {
				declaredParams.add("Integer eventTypeParam");
				filters.add("eventType == eventTypeParam");
				parameters.put("eventTypeParam", eventType);
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
			List<AuditLog> objs = parameters.size() > 0 ? (List<AuditLog>)query.executeWithMap(parameters) : (List<AuditLog>)query.execute();
			if (objs != null) {
				// force to read
				for (AuditLog obj : objs) {
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
