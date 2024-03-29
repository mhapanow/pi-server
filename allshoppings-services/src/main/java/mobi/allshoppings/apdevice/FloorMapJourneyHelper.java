package mobi.allshoppings.apdevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.model.FloorMapJourney;
import mobi.allshoppings.tools.Range;

public interface FloorMapJourneyHelper {
	
	public List<String> select(Map<Integer, List<String>> mostValue,Integer limit);
	public HashMap<Integer, String[]> mostValuable(HashMap<Integer, String[]> map2);
	public Map<Integer, List<String>> reverse(HashMap<String, Integer> map);
	public String merge(List<String> arr);
	public List<FloorMapJourney> process(String floorMapId, String mac, String fromDate, String toDate, Range range) throws ASException;
}
