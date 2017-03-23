package mobi.allshoppings.bdb.dashboard.bz.spi;


import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import mobi.allshoppings.bdb.bz.BDBDashboardBzService;
import mobi.allshoppings.bdb.bz.BDBRestBaseServerResource;
import mobi.allshoppings.dao.InnerZoneDAO;
import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.exception.ASExceptionHelper;
import mobi.allshoppings.model.EntityKind;
import mobi.allshoppings.model.InnerZone;
import mobi.allshoppings.model.adapter.NameAndIdAdapter;
import mobi.allshoppings.tools.CollectionFactory;


/**
 *
 */
public class InnerZoneListBzServiceJSONImpl
extends BDBRestBaseServerResource
implements BDBDashboardBzService {

	private static final Logger log = Logger.getLogger(InnerZoneListBzServiceJSONImpl.class.getName());

	@Autowired
	private InnerZoneDAO innerZoneDao;
	
	/**
	 * Obtains a list of FloorMap points
	 * 
	 * @return A JSON representation of the selected fields for a FloorMap
	 */
	@Override
	public String retrieve()
	{
		long start = markStart();
		JSONObject returnValue = null;
		try {
			// obtain the id and validates the auth token
			getUserFromToken();
			List<InnerZone> retList = CollectionFactory.createList();
			long diff = 0;
			
			long millisPre = new Date().getTime();

			String entityId = obtainStringValue("entityId", null);
			Integer entityKind = obtainIntegerValue("entityKind", null);

			List<InnerZone> l1 = innerZoneDao.getUsingEntityIdAndRange(entityId, entityKind, null, "name", null, true);
			for( InnerZone z1 : l1 ) {
				retList.add(z1);
				List<InnerZone> l2 = innerZoneDao.getUsingEntityIdAndRange(z1.getIdentifier(),
						EntityKind.KIND_INNER_ZONE, null, "name", null, true);
				for( InnerZone z2 : l2 ) {
					z2.setName("-- " + z2.getName());
					retList.add(z2);
				}
			}
			
			diff = new Date().getTime() - millisPre;

			// Logs the result
			log.info("Number of stores found [" + retList.size() + "] in " + diff + " millis");

			// Creates the list
			List<NameAndIdAdapter> adapter = CollectionFactory.createList();
			for( InnerZone b : retList ) {
					NameAndIdAdapter obj = new NameAndIdAdapter();
					obj.setIdentifier(b.getIdentifier());
					obj.setName(b.getName());
					obj.setAvatarId(b.getAvatarId());
					adapter.add(obj);
			}
			
			returnValue = this.getJSONRepresentationFromArrayOfObjects(
					adapter, this.obtainOutputFields(NameAndIdAdapter.class));

			return returnValue.toString();

		} catch (ASException e) {
			if( e.getErrorCode() == ASExceptionHelper.AS_EXCEPTION_AUTHTOKENEXPIRED_CODE || 
					e.getErrorCode() == ASExceptionHelper.AS_EXCEPTION_AUTHTOKENMISSING_CODE) {
				log.log(Level.INFO, e.getMessage());
			} else {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			return getJSONRepresentationFromException(e).toString();
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return getJSONRepresentationFromException(ASExceptionHelper.defaultException(e.getMessage(), e)).toString();
		} finally {
			markEnd(start);
		}
	}
	
}