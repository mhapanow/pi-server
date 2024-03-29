package mobi.allshoppings.bdb.dashboard.bz.spi;


import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import mobi.allshoppings.bdb.bz.BDBRestBaseServerResource;
import mobi.allshoppings.bdb.bz.BDBTimelineHourBzService;
import mobi.allshoppings.dao.DashboardIndicatorDataDAO;
import mobi.allshoppings.dao.StoreDAO;
import mobi.allshoppings.dashboards.DashboardAPDeviceMapperService;
import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.exception.ASExceptionHelper;
import mobi.allshoppings.model.DashboardIndicatorData;
import mobi.allshoppings.model.Store;
import mobi.allshoppings.model.User;
import mobi.allshoppings.model.tools.StatusHelper;
import mobi.allshoppings.tools.CollectionFactory;


/**
 *
 */
public class TimelineHourBzServiceJSONImpl
extends BDBRestBaseServerResource
implements BDBTimelineHourBzService {

	private static final Logger log = Logger.getLogger(TimelineHourBzServiceJSONImpl.class.getName());
	@Autowired
	private DashboardIndicatorDataDAO dao;
	@Autowired
	private StoreDAO daoSRt;

	/**
	 * Obtains information about a user
	 * 
	 * @return A JSON representation of the selected fields for a user
	 */
	@Override
	public String retrieve()
	{
		long start = markStart();
		try {
			// obtain the id and validates the auth token
			User user = getUserFromToken();

			String entityId = obtainStringValue("entityId", null);
			String elementId = obtainStringValue("elementId", null);
			String subentityId = obtainStringValue("subentityId", null);
			String fromStringDate = obtainStringValue("fromStringDate", null);
			String toStringDate = obtainStringValue("toStringDate", null);
			Integer dayOfWeek = obtainIntegerValue("dayOfWeek", null);
			Integer timezone = obtainIntegerValue("timezone", null);
			String subIdOrder = obtainStringValue("subIdOrder", null);
			Boolean average = obtainBooleanValue("average", false);
			Boolean toMinutes = obtainBooleanValue("toMinutes", false);
			Boolean eraseBlanks = obtainBooleanValue("eraseBlanks", false);
			String region = obtainStringValue("region", null);
			String format = obtainStringValue("storeFormat", null);
			String district = obtainStringValue("district", null);
			String orderx = obtainStringValue("order", null);
			List<String> subname = CollectionFactory.createList();
			boolean notEmptySubentity = StringUtils.hasText(subentityId);
			boolean singleData = notEmptySubentity ||
					(!StringUtils.hasText(region) && !StringUtils.hasText(format) && !StringUtils.hasText(district));
			if(notEmptySubentity) subname.add(subentityId);
			else {
				if(singleData) {
					for(Store i : daoSRt.getUsingRegionAndFormatAndDistrict(entityId, null, null,
							StatusHelper.statusActive(), region, format, district, orderx)) subname.add(i.getIdentifier());
				}
			}
			//on all brands, null entityId  may produce undesired behaviour
			List<DashboardIndicatorData> list = dao.getUsingFilters(null, null, Arrays.asList(elementId), null, null,
					subname, null, fromStringDate, toStringDate, null, null, dayOfWeek, timezone, null, null, null, null);

			List<String> categories = CollectionFactory.createList();

			// Creates the order list and alias map
			List<String> orderList = CollectionFactory.createList();
			if(StringUtils.hasText(subIdOrder))
				orderList.addAll(Arrays.asList(subIdOrder.split(",")));
			
			Map<String, String> aliasMap = new HashMap<>(DashboardAPDeviceMapperService.INDICATORS_ALIASES);

			// Creates the Hour Map
			Map<Integer, Integer> hourMap = CollectionFactory.createMap();
			for( int hourMapPosition = 0; hourMapPosition < 24; hourMapPosition++ ) {
				hourMap.put(hourMapPosition, hourMapPosition);
				categories.add(getHourName(hourMapPosition));
			}
			
			// Creates the result map
			Map<String, Long[]> resultMap = CollectionFactory.createMap();
			if(!CollectionUtils.isEmpty(orderList)) {
				for( String order : orderList ) {
					String key = aliasMap.get(order);
					Long[] valArray = resultMap.get(key);
					if( valArray == null ) {
						valArray = new Long[hourMap.keySet().size()];
						for( int i = 0; i < hourMap.keySet().size(); i++ ) {
							valArray[i] = 0l;
						}
					}
					resultMap.put(key, valArray);
				}
			}
			
			// Creates the counterMap
			Map<String, Integer[]> counterMap = CollectionFactory.createMap();
			if(!CollectionUtils.isEmpty(orderList)) {
				for( String order : orderList ) {
					String key = aliasMap.get(order);
					Integer[] valArray = counterMap.get(key);
					if( valArray == null ) {
						valArray = new Integer[hourMap.keySet().size()];
						for( int i = 0; i < hourMap.keySet().size(); i++ ) {
							valArray[i] = 0;
						}
					}
					counterMap.put(key, valArray);
				}
			}
			
			for(DashboardIndicatorData obj : list) {
				if( isValidForUser(user, obj)) {
					String key = obj.getElementSubName();
					String orderKey = obj.getElementSubId();
					if(!StringUtils.hasText(subIdOrder) || orderList.contains(orderKey)) {
						aliasMap.put(orderKey, key);
						Long[] valArray = resultMap.get(key);
						if( valArray == null ) {
							valArray = new Long[hourMap.keySet().size()];
							for( int i = 0; i < hourMap.keySet().size(); i++ ) {
								valArray[i] = new Long(0);
							}
						}
						Integer[] counterArray = counterMap.get(key);
						if( counterArray == null ) {
							counterArray = new Integer[hourMap.keySet().size()];
							for( int i = 0; i < hourMap.keySet().size(); i++ ) {
								counterArray[i] = new Integer(0);
							}
						}

						// Position calc according to the timezone
						int position = hourMap.get(obj.getTimeZone());

						if( average ) {
							if( obj.getDoubleValue() != null )
								valArray[position] += obj.getDoubleValue().intValue();
							else 
								log.log(Level.WARNING, "Inconsistent DashboardIndicator: " + obj.toString());

							if( obj.getRecordCount() != null )
								counterArray[position] += obj.getRecordCount().intValue();
							else 
								log.log(Level.WARNING, "Inconsistent DashboardIndicator: " + obj.toString());

						} else {
							if( obj.getDoubleValue() != null )
								valArray[position] += obj.getDoubleValue().intValue();
							else 
								log.log(Level.WARNING, "Inconsistent DashboardIndicator: " + obj.toString());
						}
						resultMap.put(key, valArray);
					}
				}
			}

			// Checks for average return
			if( average ) {
				Iterator<String> i = resultMap.keySet().iterator();
				while(i.hasNext()) {
					String key = i.next();
					Long[] valArray = resultMap.get(key);
					Integer[] counterArray = counterMap.get(key);
					for( int x = 0; x < valArray.length; x++) {
						if(counterArray == null || counterArray[x] == null) continue;
						if( counterArray[x] != 0 ) {
							if( toMinutes ) {
								valArray[x] = new Long(Math.round(valArray[x] / counterArray[x] / 60000));
							} else {
								valArray[x] = new Long(Math.round(valArray[x] / counterArray[x]));
							}
						}
					}
				}
			}
			
			// Checks if it has to erase blank spaces
			if( eraseBlanks) {
				Integer[] eraseMap = new Integer[hourMap.keySet().size()];
				for( int i = 0; i < eraseMap.length; i++ )
					eraseMap[i] = 0;

				Iterator<String> it = resultMap.keySet().iterator();
				while(it.hasNext()) {
					String key = it.next();
					Long[] valArray = resultMap.get(key);
					for( int x = 0; x < valArray.length; x++ )
						if( valArray[x] > 0 ) eraseMap[x]++;
				}
				
				int newCount = 0;
				for( int i = 0; i < eraseMap.length; i++ )
					if( eraseMap[i] > 0 ) newCount++;

				it = resultMap.keySet().iterator();
				while(it.hasNext()) {
					String key = it.next();
					Long[] valArray = resultMap.get(key);
					Long[] newArray = new Long[newCount];
					int x = 0;
					for( int i = 0; i < valArray.length; i++ ) {
						if(eraseMap[i] > 0 ) {
							newArray[x] = valArray[i];
							x++;
						}
					}
					resultMap.put(key, newArray);
				}

				List<String> newCategories = CollectionFactory.createList();
				int i = 0;
				for( String item : categories ) {
					if( eraseMap[i] > 0 )
						newCategories.add(item);
					i++;
				}
				categories = newCategories;
				
			}
			
			// Creates the final JSON Array
			JSONArray jsonArray = new JSONArray();
			if(!StringUtils.hasText(subIdOrder)) {
				Iterator<String> i = resultMap.keySet().iterator();
				while(i.hasNext()) {
					String key = i.next();
					JSONObject jsonObj = new JSONObject();
					jsonObj.put("name", key);
					jsonObj.put("type", "spline");
					jsonObj.put("data", resultMap.get(key) == null ? 0 : resultMap.get(key));
					jsonArray.put(jsonObj);
				}
			} else {
				for( String orderKey : orderList ) {
					String key = aliasMap.get(orderKey);
					JSONObject jsonObj = new JSONObject();
					jsonObj.put("name", key);
					jsonObj.put("type", "spline");
					jsonObj.put("data", resultMap.get(key) == null ? 0 : resultMap.get(key));
					jsonArray.put(jsonObj);
				}
			}

			// Returns the final value
			JSONObject ret = new JSONObject();
			ret.put("categories", categories);
			ret.put("series", jsonArray);
			return ret.toString();
			
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

	public String getHourName(int hour) {
		return new String(hour + ":00Hs");
	}
	
	public String getDateName(Date date) {
		StringBuffer sb = new StringBuffer();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int dof = cal.get(Calendar.DAY_OF_WEEK);
		
		switch(dof) {
		case Calendar.SUNDAY:
			sb.append("Dom ");
			break;
		case Calendar.MONDAY:
			sb.append("Lun ");
			break;
		case Calendar.TUESDAY:
			sb.append("Mar ");
			break;
		case Calendar.WEDNESDAY:
			sb.append("Mie ");
			break;
		case Calendar.THURSDAY:
			sb.append("Jue ");
			break;
		case Calendar.FRIDAY:
			sb.append("Vie ");
			break;
		case Calendar.SATURDAY:
			sb.append("Sab ");
			break;
		}

		sb.append(sdf.format(date));
		
		return sb.toString();
	}
}
