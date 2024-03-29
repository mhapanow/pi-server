package mobi.allshoppings.exporter.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaRenderer;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.google.common.io.Files;

import mobi.allshoppings.dao.APDAssignationDAO;
import mobi.allshoppings.dao.APUptimeDAO;
import mobi.allshoppings.dao.DashboardIndicatorDataDAO;
import mobi.allshoppings.dao.StoreDAO;
import mobi.allshoppings.dao.StoreItemDAO;
import mobi.allshoppings.dao.StoreRevenueDAO;
import mobi.allshoppings.dao.StoreTicketByHourDAO;
import mobi.allshoppings.dao.StoreTicketDAO;
import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.exception.ASExceptionHelper;
import mobi.allshoppings.exporter.ExcelExportHelper;
import mobi.allshoppings.mail.MailHelper;
import mobi.allshoppings.model.APDAssignation;
import mobi.allshoppings.model.APUptime;
import mobi.allshoppings.model.DashboardIndicatorData;
import mobi.allshoppings.model.EntityKind;
import mobi.allshoppings.model.Store;
import mobi.allshoppings.model.StoreItem;
import mobi.allshoppings.model.StoreRevenue;
import mobi.allshoppings.model.StoreTicket;
import mobi.allshoppings.model.StoreTicketByHour;
import mobi.allshoppings.model.SystemConfiguration;
import mobi.allshoppings.model.User;
import mobi.allshoppings.tools.CollectionFactory;

public class ExcelExportHelperImpl implements ExcelExportHelper {

	private static final Logger log = Logger.getLogger(ExcelExportHelperImpl.class.getName());

	static final DecimalFormat DF = new DecimalFormat("00");

	@Autowired
	private APDAssignationDAO asdao;
	@Autowired
	private APUptimeDAO apudao;
	@Autowired
	DashboardIndicatorDataDAO didDao;
	@Autowired
	private StoreDAO storeDao;
	@Autowired
	private SystemConfiguration systemConfiguration;
	@Autowired
	private StoreRevenueDAO sRevenueDao;
	@Autowired
	private StoreItemDAO sItemDao;
	@Autowired
	private StoreTicketDAO sTicketDao;
	@Autowired
	private StoreTicketByHourDAO sTicketHourDao;
	@Autowired
	private DashboardIndicatorDataDAO dashboardDataDao;
	@Autowired
	private MailHelper mailHelper;

	private static final SimpleDateFormat year = new SimpleDateFormat("yyyy");
	private static final SimpleDateFormat month = new SimpleDateFormat("MM");
	private static final int DAY_IN_MILLIS = 86400000;
	private static final int HOUR_IN_MILLIS = 3600000;

	private static final byte DATE_CELL_INDEX = 0;
	private static final byte MONTH_CELL_INDEX = 1;
	private static final byte WEEK_OF_YEAR_INDEX = 2;
	private static final byte DAY_OF_WEEK_INDEX = 3;
	private static final byte PEASENTS_CELL_INDEX = 4;
	private static final byte VISITS_CELL_INDEX = 5;
	private static final byte TICKET_CELL_INDEX = 6;
	private static final byte STORE_NAME_CELL_INDEX = 7;
	private static final byte UPTIME_BY_HOUR_PERCENTAGE_INDEX = 8;

	private static final byte HOUR_CELL_INDEX = 4;

	private static final String DATE_CELL_TITLE = "Fecha";
	private static final String MONTH_CELL_TITLE = "Mes";
	private static final String WEEK_OF_YEAR_CELL_TITLE = "Semana";
	private static final String DAY_OF_WEEK_CELL_TITLE = "D\u00EDa";
	private static final String PEASENTS_CELL_TITLE = "Paseantes";
	private static final String VISITS_CELL_TITLE = "Visitas";
	private static final String TICKET_CELL_TITLE = "Ticket";
	private static final String STORE_NAME_CELL_TITLE = "Tienda";
	private static final String UPTIME_BY_HOUR_PERCENTAGE = "Uptime";

	private static final String HOUR_CELL_TITLE = "Hora";

	@SuppressWarnings("deprecation")
	@Override
	public byte[] export(String storeId, String fromDate, String toDate, int weeks, String outDir, User toNotify)
			throws ASException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			final Store store = storeDao.get(storeId, false);
			final String brandId = store.getBrandId();
			final String storeName = store.getName();

			// Defines working variables
			Map<String, TrafficEntry> trafficMap = CollectionFactory.createMap();
			Map<String, HourEntry> hourMap = CollectionFactory.createMap();
			Map<String, PermanenceEntry> permanenceMap = CollectionFactory.createMap();
			Map<String, DateAndHourEntry> dateAndHourMap = CollectionFactory.createMap();
			Date initialDate = sdf.parse(fromDate);
			Date finalDate = sdf.parse(toDate);
			finalDate.setTime(finalDate.getTime() + DAY_IN_MILLIS);
			Date curDate = new Date(initialDate.getTime());
			String dateName = getStringDate(finalDate);
			Calendar cal = Calendar.getInstance();
			cal.setTime(finalDate);
			cal.add(Calendar.DATE, -(weeks * 7));
			cal.add(Calendar.DATE, +1);
			Date limitDate = cal.getTime();

			log.log(Level.INFO, "Processing store " + storeName + "...");

			// Creates the data map
			while (curDate.compareTo(finalDate) < 1) {
				TrafficEntry e = new TrafficEntry();
				e.setUnformmatedDate(curDate);
				trafficMap.put(sdf.format(curDate), e);
				curDate.setTime(curDate.getTime() + DAY_IN_MILLIS);
			}

			// Creates the data map
			for (int j = 0; j < 24; j++) {
				HourEntry e = new HourEntry();
				e.setUnformattedHour(j);
				hourMap.put(e.getHour(), e);
			}

			// Creates the data map
			for (int j = 0; j < 24; j++) {
				PermanenceEntry e = new PermanenceEntry();
				e.setUnformattedHour(j);
				permanenceMap.put(e.getHour(), e);
			}

			// Creates the data map
			for (int j = 0; j < 24; j++) {
				for (int k = 1; k < 8; k++) {
					DateAndHourEntry e = new DateAndHourEntry();
					e.setDay(k);
					e.setHour(j);
					dateAndHourMap.put(e.getKey(), e);
				}
			}

			String parsedInitD = sdf.format(initialDate);
			String parsedFinalD = sdf.format(finalDate);

			// Now iterates the Dashboard Indicator Data List for Traffic by Day
			// graph
			List<DashboardIndicatorData> list = didDao.getUsingFilters(brandId, EntityKind.KIND_BRAND,
					Arrays.asList("apd_visitor"),
					Arrays.asList("visitor_total_peasents", "visitor_total_visits", "visitor_total_tickets"), null,
					storeId, null, parsedInitD, parsedFinalD, null, null, null, null, null, null, null, null);

			log.log(Level.INFO, "Using " + list.size() + " elements for apd_visitor tickets...");

			Iterator<DashboardIndicatorData> i = list.iterator();
			while (i.hasNext()) {
				DashboardIndicatorData obj = i.next();
				TrafficEntry e = trafficMap.get(obj.getStringDate());
				try {
					if ("visitor_total_peasents".equals(obj.getElementSubId())) {
						e.setPeasants(e.getPeasants() + obj.getDoubleValue().longValue());
					} else if ("visitor_total_visits".equals(obj.getElementSubId())) {
						e.setVisits(e.getVisits() + obj.getDoubleValue().longValue());
					} else if ("visitor_total_tickets".equals(obj.getElementSubId())) {
						e.setTickets(e.getTickets() + obj.getDoubleValue().longValue());
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				trafficMap.put(obj.getStringDate(), e);
			}

			String parsedLimitD = sdf.format(limitDate);

			// How iterates the Dashboard Indicator Data List for Traffic by
			// Hour graph
			list = didDao.getUsingFilters(brandId, EntityKind.KIND_BRAND, Arrays.asList("apd_visitor"),
					Arrays.asList("visitor_total_peasents", "visitor_total_visits"), null, storeId, null, parsedLimitD,
					parsedFinalD, null, null, null, null, null, null, null, null);

			log.log(Level.INFO, "Using " + list.size() + " elements for apd_visitor totals...");

			i = list.iterator();
			while (i.hasNext()) {
				DashboardIndicatorData obj = i.next();
				HourEntry e = hourMap.get(HourEntry.calculateHour(obj.getTimeZone()));
				if ("visitor_total_peasents".equals(obj.getElementSubId())) {
					e.setPeasants(e.getPeasants() + obj.getDoubleValue().longValue());
				} else if ("visitor_total_visits".equals(obj.getElementSubId())) {
					e.setVisits(e.getVisits() + obj.getDoubleValue().longValue());

					DateAndHourEntry e2 = dateAndHourMap.get(DateAndHourEntry.getKey(obj.getDate(), obj.getTimeZone()));
					e2.setVisits(e2.getVisits() + obj.getDoubleValue().longValue());
					dateAndHourMap.put(e2.getKey(), e2);

				}
				hourMap.put(e.getHour(), e);
			}

			// Now iterates the Dashboard Indicator Data List for Permanence
			// Graph
			list = didDao.getUsingFilters(brandId, EntityKind.KIND_BRAND, Arrays.asList("apd_permanence"),
					Arrays.asList("permanence_hourly_peasents", "permanence_hourly_visits"), null, storeId, null,
					parsedLimitD, parsedFinalD, null, null, null, null, null, null, null, null);

			log.log(Level.INFO, "Using " + list.size() + " elements for apd_permanence...");

			long totalVisitsPermanence = 0;
			long totalVisitsCount = 0;
			i = list.iterator();
			while (i.hasNext()) {
				DashboardIndicatorData obj = i.next();
				PermanenceEntry e = permanenceMap.get(PermanenceEntry.calculateHour(obj.getTimeZone()));
				if ("permanence_hourly_peasents".equals(obj.getElementSubId())) {
					e.setPeasants(e.getPeasants() + obj.getDoubleValue().longValue());
					e.setPeasantsCount(e.getPeasantsCount() + obj.getRecordCount());
				} else if ("permanence_hourly_visits".equals(obj.getElementSubId())) {
					e.setVisits(e.getVisits() + obj.getDoubleValue().longValue());
					e.setVisitsCount(e.getVisitsCount() + obj.getRecordCount());
					totalVisitsPermanence += obj.getDoubleValue().longValue();
					totalVisitsCount += obj.getRecordCount();
				}
				permanenceMap.put(e.getHour(), e);
			}

			// Opens the template
			ZipSecureFile.setMinInflateRatio(0);

			String filename = resolveDumpFileName(outDir, storeName, finalDate);
			File dir = new File(filename).getParentFile();
			if (!dir.exists())
				dir.mkdirs();
			Files.copy(new File(
					systemConfiguration.getResourcesDir() + File.separator + systemConfiguration.getExcelTemplate()),
					new File(filename));
			XSSFWorkbook workbook = new XSSFWorkbook(filename);
			XSSFSheet trafficByDay = workbook.getSheet("Trafico por Dia");
			XSSFSheet trafficByHour = workbook.getSheet("Trafico por Hora");
			XSSFSheet permanence = workbook.getSheet("Permanencia");
			XSSFSheet highHours = workbook.getSheet("Horas Pico");
			XSSFSheet deadHours = workbook.getSheet("Horas Muertas");
			XSSFSheet formulae = workbook.getSheet("Formulae");
			XSSFSheet print = workbook.getSheet("Impresión PDF");

			// Gets the model cells
			CellCopyPolicy policy = new CellCopyPolicy();
			policy.setCopyCellFormula(true);
			policy.setCopyCellStyle(true);

			List<XSSFCell> modelTrafficByDay1 = CollectionFactory.createList();
			List<XSSFCell> modelTrafficByDay2 = CollectionFactory.createList();
			List<XSSFCell> modelTrafficByHour = CollectionFactory.createList();
			List<XSSFCell> modelPermanence = CollectionFactory.createList();
			List<XSSFCell> modelHour = CollectionFactory.createList();

			// Gets the model cells
			Iterator<Row> rows = formulae.iterator();
			while (rows.hasNext()) {
				XSSFRow row = (XSSFRow) rows.next();
				getTemplateRow(modelTrafficByDay1, row, "Trafico por Dia 1");
				getTemplateRow(modelTrafficByDay2, row, "Trafico por Dia 2");
				getTemplateRow(modelTrafficByHour, row, "Trafico por Hora");
				getTemplateRow(modelPermanence, row, "Permanencia");
				getTemplateRow(modelHour, row, "Horas");
			}

			// Iterates the data map for the Traffic by day sheet
			int rowIndex = 10;
			curDate.setTime(initialDate.getTime());
			int partialIndex = 0;
			while (curDate.compareTo(finalDate) < 1) {

				StoreRevenue revenue;
				StoreItem items;
				String parsedDate = sdf.format(curDate);
				try {
					revenue = sRevenueDao.getUsingStoreIdAndDate(storeId, parsedDate, true);
				} catch (ASException e) {
					revenue = new StoreRevenue();
					revenue.setQty(0.0);
				}
				try {
					items = sItemDao.getUsingStoreIdAndDate(storeId, parsedDate, false);
				} catch (ASException e) {
					items = new StoreItem();
					items.setQty(0);
				}
				TrafficEntry e = trafficMap.get(parsedDate);
				XSSFRow row = trafficByDay.getRow(rowIndex);
				if (null == row)
					row = trafficByDay.createRow(rowIndex);
				List<XSSFCell> model = partialIndex == 0 ? modelTrafficByDay1 : modelTrafficByDay2;

				XSSFCell cell;
				for (int j = 0; j < model.size(); j++) {
					cell = row.createCell(j);
					cell.copyCellFrom(model.get(j), policy);
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<stringDate>")) {
						cell.setCellType(CellType.STRING);
						cell.setCellValue(e.getDate());
					} else if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<peasants>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(e.getPeasants());
					}
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<visits>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(e.getVisits());
					}
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<tickets>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(e.getTickets());
					}
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<items>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(items.getQty());
					}
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<revenue>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(revenue.getQty());
					}
					if (model.get(j).getCellTypeEnum() == CellType.FORMULA) {
						XSSFEvaluationWorkbook fpWb = XSSFEvaluationWorkbook.create(workbook);
						Ptg[] tokens = FormulaParser.parse(model.get(j).getCellFormula(), fpWb, FormulaType.CELL,
								workbook.getSheetIndex(formulae));
						for (Ptg token : tokens) {
							if (token instanceof RefPtg) {
								RefPtg refPtg = (RefPtg) token;
								refPtg.setColumn(refPtg.getColumn() - 1);
								if (refPtg.getRow() == model.get(j).getRowIndex())
									refPtg.setRow(rowIndex);
								else
									refPtg.setRow(refPtg.getRow() + rowIndex - 1);
							}
						}
						cell.setCellFormula(FormulaRenderer.toFormulaString(fpWb, tokens));
					}

				}

				curDate.setTime(curDate.getTime() + DAY_IN_MILLIS);
				partialIndex++;
				rowIndex++;
			}

			// Iterates the data map for the Traffic by hour sheet
			rowIndex = 3;
			partialIndex = 0;
			for (int k = 0; k < 24; k++) {
				HourEntry e = hourMap.get(HourEntry.calculateHour(k));

				XSSFRow row = trafficByHour.getRow(rowIndex);
				if (null == row)
					row = trafficByHour.createRow(rowIndex);
				List<XSSFCell> model = modelTrafficByHour;

				XSSFCell cell;
				for (int j = 0; j < model.size(); j++) {
					cell = row.createCell(j);
					cell.copyCellFrom(model.get(j), policy);
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<stringHour>")) {
						cell.setCellType(CellType.STRING);
						cell.setCellValue(e.getHour());
					} else if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<peasants>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(e.getPeasants());
					}
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<visits>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(e.getVisits());
					}
					if (model.get(j).getCellTypeEnum() == CellType.FORMULA) {
						XSSFEvaluationWorkbook fpWb = XSSFEvaluationWorkbook.create(workbook);
						Ptg[] tokens = FormulaParser.parse(model.get(j).getCellFormula(), fpWb, FormulaType.CELL,
								workbook.getSheetIndex(formulae));
						for (Ptg token : tokens) {
							if (token instanceof RefPtg) {
								RefPtg refPtg = (RefPtg) token;
								refPtg.setColumn(refPtg.getColumn() - 1);
								if (refPtg.getRow() == model.get(j).getRowIndex())
									refPtg.setRow(rowIndex);
								else
									refPtg.setRow(refPtg.getRow() + rowIndex - 1);
							}
						}
						cell.setCellFormula(FormulaRenderer.toFormulaString(fpWb, tokens));
					}
				}
				partialIndex++;
				rowIndex++;
			}

			// Iterates the data map for the Permanence sheet
			rowIndex = 3;
			partialIndex = 0;
			for (int k = 0; k < 24; k++) {
				PermanenceEntry e = permanenceMap.get(PermanenceEntry.calculateHour(k));

				XSSFRow row = permanence.getRow(rowIndex);
				if (null == row)
					row = permanence.createRow(rowIndex);
				List<XSSFCell> model = modelPermanence;

				XSSFCell cell;
				for (int j = 0; j < model.size(); j++) {
					cell = row.createCell(j);
					cell.copyCellFrom(model.get(j), policy);
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<stringHour>")) {
						cell.setCellType(CellType.STRING);
						cell.setCellValue(e.getHour());
					} else if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<peasants>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(e.getFormattedPeasants());
					}
					if (model.get(j).getCellTypeEnum() == CellType.STRING
							&& model.get(j).getStringCellValue().equals("<visits>")) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(e.getFormattedVisits());
					}
					if (model.get(j).getCellTypeEnum() == CellType.FORMULA) {
						XSSFEvaluationWorkbook fpWb = XSSFEvaluationWorkbook.create(workbook);
						Ptg[] tokens = FormulaParser.parse(model.get(j).getCellFormula(), fpWb, FormulaType.CELL,
								workbook.getSheetIndex(formulae));
						for (Ptg token : tokens) {
							if (token instanceof RefPtg) {
								RefPtg refPtg = (RefPtg) token;
								refPtg.setColumn(refPtg.getColumn() - 1);
								if (refPtg.getRow() == model.get(j).getRowIndex())
									refPtg.setRow(rowIndex);
								else
									refPtg.setRow(refPtg.getRow() + rowIndex - 1);
							}
						}
						cell.setCellFormula(FormulaRenderer.toFormulaString(fpWb, tokens));
					}
				}
				partialIndex++;
				rowIndex++;
			}
			// Iterates the data map for the High and Dead hours sheets
			int cellIndex = 0;
			Iterator<String> keys = dateAndHourMap.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();
				DateAndHourEntry e = dateAndHourMap.get(key);
				switch (e.getDay()) {
				case Calendar.SUNDAY:
					rowIndex = 8;
					break;
				case Calendar.MONDAY:
					rowIndex = 2;
					break;
				case Calendar.TUESDAY:
					rowIndex = 3;
					break;
				case Calendar.WEDNESDAY:
					rowIndex = 4;
					break;
				case Calendar.THURSDAY:
					rowIndex = 5;
					break;
				case Calendar.FRIDAY:
					rowIndex = 6;
					break;
				case Calendar.SATURDAY:
					rowIndex = 7;
					break;
				}
				cellIndex = 4 + e.getHour();
				XSSFRow row = highHours.getRow(rowIndex);
				if (null == row)
					row = highHours.createRow(rowIndex);
				List<XSSFCell> model = modelHour;
				XSSFCell cell = row.getCell(cellIndex);
				if (cell == null)
					cell = row.createCell(cellIndex);
				cell.copyCellFrom(model.get(0), policy);
				if (e.getVisits() > 0) {
					cell.setCellValue(e.getVisits());
				}
				cellIndex = 3 + e.getHour();
				row = deadHours.getRow(rowIndex);
				if (null == row)
					row = deadHours.createRow(rowIndex);
				cell = row.getCell(cellIndex);
				if (cell == null)
					cell = row.createCell(cellIndex);
				cell.copyCellFrom(model.get(0), policy);
				if (e.getVisits() > 0) {
					cell.setCellValue(e.getVisits());
				}
			}
			// Iterates the printing page sheets
			Iterator<Row> prows = print.iterator();
			while (prows.hasNext()) {
				XSSFRow row = (XSSFRow) prows.next();
				Iterator<Cell> pcells = row.iterator();
				while (pcells.hasNext()) {
					XSSFCell cell = (XSSFCell) pcells.next();
					if (cell.getCellTypeEnum() == CellType.STRING && cell.getStringCellValue().equals("<storeName>")) {
						cell.setCellValue(storeName);
					}
					if (cell.getCellTypeEnum() == CellType.STRING && cell.getStringCellValue().equals("<dateName>")) {
						cell.setCellValue(dateName);
					}
					if (cell.getCellTypeEnum() == CellType.STRING && cell.getStringCellValue().equals("<permanence>")) {
						int p = (int) (totalVisitsCount > 0 ? (totalVisitsPermanence / totalVisitsCount / 60000) : 0);
						cell.setCellValue(p);
					}
				}
			}
			log.log(Level.INFO, "Rendering Formulae...");
			try {
				XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
			} catch (Exception e) {
				e.printStackTrace();
			}
			File tmp = File.createTempFile(storeId, ".xlsx");
			FileOutputStream fos = new FileOutputStream(tmp);
			log.log(Level.INFO, "Written to: " + tmp.getAbsolutePath());
			workbook.write(fos);
			fos.flush();
			fos.close();
			workbook.write(bos);
			bos.close();

			if (toNotify != null)
				sendReportMail(mailHelper, toNotify, tmp, log);
			else
				tmp.delete();

			workbook.close();
			return bos.toByteArray();
		} catch (Exception ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			throw ASExceptionHelper.defaultException(ex.getMessage(), ex);
		}
	}

	public void getTemplateRow(List<XSSFCell> target, XSSFRow row, String search) {
		XSSFCell cell = row.getCell(0);
		if (null != cell && cell.getStringCellValue().equals(search)) {
			target.clear();
			Iterator<Cell> cells = row.iterator();
			int index = 0;
			while (cells.hasNext()) {
				cell = (XSSFCell) cells.next();
				if (index > 0) {
					target.add(cell);
				}
				index++;
			}
		}
	}

	public String getStringDate(Date date) {
		StringBuffer sb = new StringBuffer();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int month = cal.get(Calendar.MONTH);
		switch (month) {
		case Calendar.JANUARY:
			sb.append("Enero ");
			break;
		case Calendar.FEBRUARY:
			sb.append("Febrero ");
			break;
		case Calendar.MARCH:
			sb.append("Marzo ");
			break;
		case Calendar.APRIL:
			sb.append("Abril ");
			break;
		case Calendar.MAY:
			sb.append("Mayo ");
			break;
		case Calendar.JUNE:
			sb.append("Julio ");
			break;
		case Calendar.JULY:
			sb.append("Julio ");
			break;
		case Calendar.AUGUST:
			sb.append("Agosto ");
			break;
		case Calendar.SEPTEMBER:
			sb.append("Septiembre ");
			break;
		case Calendar.OCTOBER:
			sb.append("Octubre ");
			break;
		case Calendar.NOVEMBER:
			sb.append("Noviembre ");
			break;
		case Calendar.DECEMBER:
			sb.append("Diciembre ");
			break;
		}

		sb.append(cal.get(Calendar.YEAR));
		return sb.toString();
	}

	public static class DateAndHourEntry {
		private int day;
		private int hour;
		private long visits;

		public DateAndHourEntry() {
			day = 0;
			hour = 0;
			visits = 0;
		}

		public static String getKey(Date date, int hour) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			int day = cal.get(Calendar.DAY_OF_WEEK);
			return DF.format(day) + "-" + DF.format(hour);
		}

		public String getKey() {
			DecimalFormat DF = new DecimalFormat("00");
			return DF.format(day) + "-" + DF.format(hour);
		}

		/**
		 * @return the day
		 */
		public int getDay() {
			return day;
		}

		/**
		 * @param day
		 *            the day to set
		 */
		public void setDay(int day) {
			this.day = day;
		}

		/**
		 * @param day
		 *            the day to set
		 */
		public void setDay(Date day) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(day);
			this.day = cal.get(Calendar.DATE);
		}

		/**
		 * @return the hour
		 */
		public int getHour() {
			return hour;
		}

		/**
		 * @param hour
		 *            the hour to set
		 */
		public void setHour(int hour) {
			this.hour = hour;
		}

		/**
		 * @return the visits
		 */
		public long getVisits() {
			return visits;
		}

		/**
		 * @param visits
		 *            the visits to set
		 */
		public void setVisits(long visits) {
			this.visits = visits;
		}

	}

	public static class HourEntry {

		private String hour;
		private long peasants;
		private long visits;

		public HourEntry() {
			hour = "";
			peasants = 0;
			visits = 0;
		}

		/**
		 * @return the hour
		 */
		public String getHour() {
			return hour;
		}

		/**
		 * @param hour
		 *            the hour to set
		 */
		public void setHour(String hour) {
			this.hour = hour;
		}

		/**
		 * @return the peasants
		 */
		public long getPeasants() {
			return peasants;
		}

		/**
		 * @param peasants
		 *            the peasants to set
		 */
		public void setPeasants(long peasants) {
			this.peasants = peasants;
		}

		/**
		 * @return the visits
		 */
		public long getVisits() {
			return visits;
		}

		/**
		 * @param visits
		 *            the visits to set
		 */
		public void setVisits(long visits) {
			this.visits = visits;
		}

		public void setUnformattedHour(int hour) {
			setHour(calculateHour(hour));
		}

		public static String calculateHour(int hour) {
			DecimalFormat df = new DecimalFormat("00");
			return df.format(hour) + ":00Hs";
		}

		public String toString() {
			return hour + ", " + peasants + ", " + visits;
		}

	}

	public static class PermanenceEntry {

		private String hour;
		private long peasants;
		private long peasantsCount;
		private long visits;
		private long visitsCount;

		public PermanenceEntry() {
			hour = "";
			peasants = 0;
			peasantsCount = 0;
			visits = 0;
			visitsCount = 0;
		}

		/**
		 * @return the hour
		 */
		public String getHour() {
			return hour;
		}

		/**
		 * @param hour
		 *            the hour to set
		 */
		public void setHour(String hour) {
			this.hour = hour;
		}

		/**
		 * @return the peasants
		 */
		public long getPeasants() {
			return peasants;
		}

		public long getFormattedPeasants() {
			if (peasantsCount > 0)
				return peasants / peasantsCount / 60000;
			else
				return 0;
		}

		public long getFormattedVisits() {
			if (visitsCount > 0)
				return visits / visitsCount / 60000;
			else
				return 0;
		}

		/**
		 * @param peasants
		 *            the peasants to set
		 */
		public void setPeasants(long peasants) {
			this.peasants = peasants;
		}

		/**
		 * @return the peasantsCount
		 */
		public long getPeasantsCount() {
			return peasantsCount;
		}

		/**
		 * @param peasantsCount
		 *            the peasantsCount to set
		 */
		public void setPeasantsCount(long peasantsCount) {
			this.peasantsCount = peasantsCount;
		}

		/**
		 * @return the visits
		 */
		public long getVisits() {
			return visits;
		}

		/**
		 * @param visits
		 *            the visits to set
		 */
		public void setVisits(long visits) {
			this.visits = visits;
		}

		/**
		 * @return the visitsCount
		 */
		public long getVisitsCount() {
			return visitsCount;
		}

		/**
		 * @param visitsCount
		 *            the visitsCount to set
		 */
		public void setVisitsCount(long visitsCount) {
			this.visitsCount = visitsCount;
		}

		public void setUnformattedHour(int hour) {
			setHour(calculateHour(hour));
		}

		public static String calculateHour(int hour) {
			return DF.format(hour) + ":00Hs";
		}

		public String toString() {
			return hour + ", " + peasants + ", " + visits;
		}

	}

	public class TrafficEntry {

		private String date;
		private long peasants;
		private long visits;
		private long tickets;

		public TrafficEntry() {
			date = "";
			peasants = 0;
			visits = 0;
			tickets = 0;
		}

		/**
		 * @return the date
		 */
		public String getDate() {
			return date;
		}

		/**
		 * @param date
		 *            the date to set
		 */
		public void setDate(String date) {
			this.date = date;
		}

		/**
		 * @return the peasants
		 */
		public long getPeasants() {
			return peasants;
		}

		/**
		 * @param peasants
		 *            the peasants to set
		 */
		public void setPeasants(long peasants) {
			this.peasants = peasants;
		}

		/**
		 * @return the visits
		 */
		public long getVisits() {
			return visits;
		}

		/**
		 * @param visits
		 *            the visits to set
		 */
		public void setVisits(long visits) {
			this.visits = visits;
		}

		/**
		 * @return the tickets
		 */
		public long getTickets() {
			return tickets;
		}

		/**
		 * @param tickets
		 *            the tickets to set
		 */
		public void setTickets(long tickets) {
			this.tickets = tickets;
		}

		public void setUnformmatedDate(Date date) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			StringBuffer sb = new StringBuffer();

			int dof = cal.get(Calendar.DAY_OF_WEEK);
			switch (dof) {
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

			sb.append(DF.format(cal.get(Calendar.DATE)));
			sb.append("/");
			sb.append(DF.format(cal.get(Calendar.MONTH) + 1));

			this.setDate(sb.toString());
		}

		public String toString() {
			return date + ", " + peasants + ", " + visits + ", " + tickets;
		}
	}

	public static String resolveDumpFileName(String baseDir, String baseName, Date forDate) {
		String myYear = year.format(forDate);
		String myMonth = month.format(forDate);
		baseName = baseName.replaceAll("\t", " ");

		StringBuffer sb = new StringBuffer();
		sb.append(baseDir);
		if (!baseDir.endsWith(File.separator))
			sb.append(File.separator);
		sb.append("visits").append(File.separator);
		sb.append(myYear).append(File.separator);
		sb.append(myMonth).append(File.separator);
		sb.append(baseName).append(".xlsm");

		return sb.toString();
	}

	@Override
	public byte[] exportDB(List<String> storesId, String brandId, String fromDate, String toDate, String outDir,
			boolean saveTmp, User toNotify) throws ASException {
		// FIXME optimize to use only one loop for both daily and hourly data
		
		
		if (brandId != null && StringUtils.hasText(brandId)) {
			for (Store current : storeDao.getUsingBrandAndStatus(brandId, null, null)) {
				if (!storesId.contains(current.getIdentifier()))
					storesId.add(current.getIdentifier());
			}
		}
		Workbook workbook = new XSSFWorkbook();
		Sheet currentSheet;
		Row row;
		CreationHelper helper = workbook.getCreationHelper();
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		/*
		 * CellStyle checkinStyle = workbook.createCellStyle();
		 * checkinStyle.setDataFormat(helper.createDataFormat().
		 * getFormat("m/d/yy h:mm:ss"));
		 */
		CellStyle dataDStyle = workbook.createCellStyle();
		dataDStyle.setDataFormat(helper.createDataFormat().getFormat("m/d/yy"));
		CellStyle hourStyle = workbook.createCellStyle();
		hourStyle.setDataFormat(helper.createDataFormat().getFormat("h:mm"));
		/*
		 * CellStyle currencyStyle = workbook.createCellStyle();
		 * currencyStyle.setDataFormat(helper.createDataFormat().getFormat(
		 * "$#,##0.00"));
		 */
		Cell cell;
		int rowIndex = 0;
		Store store;
		Date initialDate, finalDate, curDate;
		List<APDAssignation> apdassignation = CollectionFactory.createList();
		List<APUptime> uptime = null;
		Calendar calforcomp = Calendar.getInstance();
		final SimpleDateFormat hourlySdf = new SimpleDateFormat("HH:mm");
		int dayLoop;
		int total = 0 ;
		int contador = 0;
		try {
			initialDate = sdf.parse(fromDate);
			finalDate = sdf.parse(toDate);
		} catch (ParseException ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			try {
				workbook.close();
			} catch (IOException e) {
			}
			throw ASExceptionHelper.defaultException(ex.getMessage(), ex);
		}
		if(brandId == null) brandId = "Tienda - ";
		currentSheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(brandId + " datos diarios"));
		String parsedDate;
		StoreTicket tickets;
		Calendar cal = Calendar.getInstance();
		String[] daysOfWeek = new DateFormatSymbols(new Locale("es")).getShortWeekdays();
		row = currentSheet.createRow(rowIndex++);
		row.createCell(DATE_CELL_INDEX).setCellValue(helper.createRichTextString(DATE_CELL_TITLE));
		row.createCell(MONTH_CELL_INDEX).setCellValue(helper.createRichTextString(MONTH_CELL_TITLE));
		row.createCell(WEEK_OF_YEAR_INDEX).setCellValue(helper.createRichTextString(WEEK_OF_YEAR_CELL_TITLE));
		row.createCell(DAY_OF_WEEK_INDEX).setCellValue(helper.createRichTextString(DAY_OF_WEEK_CELL_TITLE));
		row.createCell(PEASENTS_CELL_INDEX).setCellValue(helper.createRichTextString(PEASENTS_CELL_TITLE));
		row.createCell(VISITS_CELL_INDEX).setCellValue(helper.createRichTextString(VISITS_CELL_TITLE));
		row.createCell(TICKET_CELL_INDEX).setCellValue(helper.createRichTextString(TICKET_CELL_TITLE));
		row.createCell(STORE_NAME_CELL_INDEX).setCellValue(helper.createRichTextString(STORE_NAME_CELL_TITLE));
		// processing begins
		// adds daily values
		log.log(Level.INFO, "Adding stores daily data");
		for (String storeId : storesId) {
			store = storeDao.get(storeId, false);
			
			log.log(Level.INFO, "Processing store " + store.getName() + " for period " + fromDate
					+ " - " + toDate + "...");
			curDate = new Date(initialDate.getTime());
			// adds a row for each segment of data
			while (curDate.compareTo(finalDate) < 0) {
				parsedDate = sdf.format(curDate);
				log.log(Level.INFO, "Doing " + parsedDate +" (whole day)...");
				row = currentSheet.createRow(rowIndex++);
				ArrayList<String> stId = new ArrayList<String>();
				stId.add(storeId);
				try {
					tickets = sTicketDao.getUsingStoreIdAndDate(storeId, parsedDate, false);
				} catch (ASException e) {
					tickets = new StoreTicket();
					tickets.setQty(0);
				}
				cell = row.createCell(DATE_CELL_INDEX);
				cell.setCellValue(curDate);
				cell.setCellStyle(dataDStyle);
				cal.setTime(curDate);
				row.createCell(MONTH_CELL_INDEX).setCellValue(cal.get(Calendar.MONTH) + 1);
				row.createCell(WEEK_OF_YEAR_INDEX).setCellValue(cal.get(Calendar.WEEK_OF_YEAR));
				row.createCell(DAY_OF_WEEK_INDEX).setCellValue(daysOfWeek[cal.get(Calendar.DAY_OF_WEEK)]);
				int visitCount = 0;
				int peasantCount = 0;
				visitCount = peasantCount = 0;
				List<DashboardIndicatorData> visits = dashboardDataDao.getUsingFilters(store.getBrandId(), null,
						"apd_visitor", "visitor_total_visits", null, storeId, null, parsedDate, parsedDate,
						null, null, null, null, null, null, null, null);
				List<DashboardIndicatorData> peaseants = dashboardDataDao.getUsingFilters(store.getBrandId(), null, 
						"apd_visitor", "visitor_total_peasents", null, storeId, null, parsedDate, parsedDate, null,
						null, null, null, null, null, null, null);
				for (DashboardIndicatorData visit : visits) {
					visitCount += visit.getDoubleValue().intValue();
				}
				for (DashboardIndicatorData peaseant : peaseants) {
					peasantCount += peaseant.getDoubleValue().intValue();
				}
				row.createCell(PEASENTS_CELL_INDEX).setCellValue(peasantCount);
				row.createCell(VISITS_CELL_INDEX).setCellValue(visitCount);
				row.createCell(TICKET_CELL_INDEX).setCellValue(tickets.getQty());
				row.createCell(STORE_NAME_CELL_INDEX).setCellValue(store.getName());
				curDate.setTime(curDate.getTime() + DAY_IN_MILLIS);
			} // for each day
		} // for each store */
		// currentSheet.autoSizeColumn(DATE_CELL_INDEX);
		currentSheet.autoSizeColumn(MONTH_CELL_INDEX);
		currentSheet.autoSizeColumn(WEEK_OF_YEAR_INDEX);
		currentSheet.autoSizeColumn(DAY_OF_WEEK_INDEX);
		currentSheet.autoSizeColumn(PEASENTS_CELL_INDEX);
		currentSheet.autoSizeColumn(VISITS_CELL_INDEX);
		currentSheet.autoSizeColumn(TICKET_CELL_INDEX);
		currentSheet.autoSizeColumn(STORE_NAME_CELL_INDEX);
		currentSheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(brandId + " datos por hora"));
		rowIndex = 0;
		row = currentSheet.createRow(rowIndex++);
		row.createCell(DATE_CELL_INDEX).setCellValue(helper.createRichTextString(DATE_CELL_TITLE));
		row.createCell(MONTH_CELL_INDEX).setCellValue(helper.createRichTextString(MONTH_CELL_TITLE));
		row.createCell(WEEK_OF_YEAR_INDEX).setCellValue(helper.createRichTextString(WEEK_OF_YEAR_CELL_TITLE));
		row.createCell(DAY_OF_WEEK_INDEX).setCellValue(helper.createRichTextString(DAY_OF_WEEK_CELL_TITLE));
		row.createCell(HOUR_CELL_INDEX).setCellValue(helper.createRichTextString(HOUR_CELL_TITLE));
		row.createCell(PEASENTS_CELL_INDEX + 1).setCellValue(helper.createRichTextString(PEASENTS_CELL_TITLE));
		row.createCell(VISITS_CELL_INDEX + 1).setCellValue(helper.createRichTextString(VISITS_CELL_TITLE));
		row.createCell(TICKET_CELL_INDEX + 1).setCellValue(helper.createRichTextString(TICKET_CELL_TITLE));
		row.createCell(STORE_NAME_CELL_INDEX + 1).setCellValue(helper.createRichTextString(STORE_NAME_CELL_TITLE));
		row.createCell(UPTIME_BY_HOUR_PERCENTAGE_INDEX+1).setCellValue(helper.createRichTextString(UPTIME_BY_HOUR_PERCENTAGE));
		Map<Integer, DashboardIndicatorData> visits = CollectionFactory.createMap();
		Map<Integer, DashboardIndicatorData> peasents = CollectionFactory.createMap();
		// adding hourly data
		log.log(Level.INFO, "Adding stores hourly data");
		for (String storeId : storesId) {
			store = storeDao.get(storeId, false);
			curDate = new Date(initialDate.getTime());
			calforcomp.setTime(curDate);
			//apdassignation.clear();
			apdassignation = asdao.getUsingEntityIdAndEntityKind(storeId, EntityKind.KIND_STORE);
			for (APDAssignation assing : apdassignation)
			{
			uptime = apudao.getUsingHostnameAndDates(assing.getHostname(),curDate,finalDate);
			
			}
			
			log.log(Level.INFO, "Processing store " + store.getName() + " for period " + fromDate
					+ " - " + toDate + "...");			

			// adds a row for each segment of data
			dayLoop = 0;
			while (curDate.compareTo(finalDate) < 0) {
				parsedDate = sdf.format(curDate);
				row = currentSheet.createRow(rowIndex++);
				cell = row.createCell(DATE_CELL_INDEX);
				cell.setCellValue(curDate);
				cell.setCellStyle(dataDStyle);

				if (dayLoop == 0) {
					log.log(Level.INFO, "Doing hourly " + parsedDate +"...");
					cal.setTime(curDate);
					visits.clear();
					peasents.clear();
					List<DashboardIndicatorData> data = dashboardDataDao.getUsingFilters(store.getBrandId(),
							null, "apd_visitor", "visitor_total_visits", null, storeId, null, parsedDate,
							parsedDate, null, null, null, null, null, null, null, null);
					for (DashboardIndicatorData singleData : data)
						visits.put(singleData.timeZone, singleData);
					data = dashboardDataDao.getUsingFilters(store.getBrandId(), null, "apd_visitor",
							"visitor_total_peasents", null, storeId, null, parsedDate, parsedDate, null,
							null, null, null, null, null, null, null);
					for (DashboardIndicatorData singleData : data)
						peasents.put(singleData.timeZone, singleData);
				}
				row.createCell(MONTH_CELL_INDEX).setCellValue(cal.get(Calendar.MONTH) + 1);
				row.createCell(WEEK_OF_YEAR_INDEX).setCellValue(cal.get(Calendar.WEEK_OF_YEAR));
				row.createCell(DAY_OF_WEEK_INDEX).setCellValue(daysOfWeek[cal.get(Calendar.DAY_OF_WEEK)]);
				cell = row.createCell(HOUR_CELL_INDEX);
				cell.setCellValue(hourlySdf.format(curDate));
				cell.setCellStyle(hourStyle);
				calforcomp.add(Calendar.HOUR_OF_DAY, 1);
				for (APUptime i : uptime) {
					Map<String, Integer> uptimetime = i.getRecord();
					Iterator<Entry<String, Integer>> j = uptimetime.entrySet().iterator();
					while(j.hasNext())
					{
						Map.Entry<String, Integer> hora= j.next();
						if(hora.getKey().compareTo(hourlySdf.format(curDate)) >= 0 && hora.getKey().compareTo(hourlySdf.format(calforcomp.getTime())) < 0)
						{
							if(hora.getValue() == 1) {
							contador++;
							total++;
							}else 
							{
								total++;
							}
						}

				}
				
				}
				row.createCell(PEASENTS_CELL_INDEX + 1)
						.setCellValue(peasents.containsKey(dayLoop) ? peasents.get(dayLoop).getDoubleValue() : 0);
				row.createCell(VISITS_CELL_INDEX + 1)
						.setCellValue(visits.containsKey(dayLoop) ? visits.get(dayLoop).getDoubleValue() : 0);
				try {
					StoreTicketByHour hourlyTickets = sTicketHourDao.getUsingStoreIdAndDateAndHour(storeId, parsedDate,
							hourlySdf.format(curDate), false);
					row.createCell(TICKET_CELL_INDEX + 1).setCellValue(hourlyTickets.getQty());
				} catch (ASException e) {
					row.createCell(TICKET_CELL_INDEX + 1).setCellValue(helper.createRichTextString("NA"));
				}
				row.createCell(STORE_NAME_CELL_INDEX + 1).setCellValue(store.getName());
				if(total== 0)
				{
					row.createCell(UPTIME_BY_HOUR_PERCENTAGE_INDEX + 1).setCellValue(0);
				}else
				row.createCell(UPTIME_BY_HOUR_PERCENTAGE_INDEX + 1).setCellValue((contador *100)/total);
				curDate.setTime(curDate.getTime() + HOUR_IN_MILLIS);
				dayLoop = (dayLoop + 1) % 24;
				total= 0;
				contador=0;
			} // por cada día
		} // por cada tienda

		currentSheet.autoSizeColumn(DATE_CELL_INDEX);
		currentSheet.autoSizeColumn(MONTH_CELL_INDEX);
		currentSheet.autoSizeColumn(WEEK_OF_YEAR_INDEX);
		currentSheet.autoSizeColumn(DAY_OF_WEEK_INDEX);
		currentSheet.autoSizeColumn(HOUR_CELL_INDEX);
		currentSheet.autoSizeColumn(PEASENTS_CELL_INDEX + 1);
		currentSheet.autoSizeColumn(VISITS_CELL_INDEX + 1);
		currentSheet.autoSizeColumn(TICKET_CELL_INDEX + 1);
		currentSheet.autoSizeColumn(STORE_NAME_CELL_INDEX + 1);
		currentSheet.autoSizeColumn(UPTIME_BY_HOUR_PERCENTAGE_INDEX+1);
		try {
			if (saveTmp || toNotify != null) {
				FileOutputStream fos;
				File tmp = File.createTempFile(outDir + "_", ".xlsx");
				fos = new FileOutputStream(tmp);
				workbook.write(fos);
				fos.flush();
				log.log(Level.INFO, "Excel created in " + tmp.getAbsolutePath());
				fos.close();

				ExcelExportHelperImpl.sendReportMail(
						mailHelper, toNotify, tmp, log);
			}
			ByteArrayOutputStream bos;
			bos = new ByteArrayOutputStream();
			workbook.write(bos);
			bos.flush();
			workbook.close();
			return bos.toByteArray();
		} catch (IOException ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			throw ASExceptionHelper.defaultException(ex.getMessage(), ex);
		}
		
	}

	/**
	 * Helper method to send a notification mail no the requester user.
	 * 
	 * @param helper
	 *            - The mail helper bean to use.
	 * @param user
	 *            - The user to mail.
	 * @param report
	 *            - The resultant excel report to attach to mail.
	 * @param log
	 *            - A Logger to print debug logs. Its a mandatory field.
	 */
	public static void sendReportMail(MailHelper helper, User user, File report, Logger log) {
		try {
			helper.sendMessageWithAttachMents(user, "Getin - Su reporte está " + "listo",
					"<p>Su reporte se ha generado y se ha adjuntado a este"
							+ " mensaje para su comodidad.</p><p>El equipo de "
							+ "<a href=\"getin.mx\">Getin</a> le desea un excelente" + " d&iacute;a.",
					report.getAbsolutePath(), report.getName());
		} catch (Exception e) {
			log.log(Level.INFO, "Failed to send email notification", e);
		}
	}

	@Override
	public void importDB(String brandId, String inputFile) throws ASException {
		// TODO import uptime
		// TODO import innerzone
		XSSFWorkbook workbook;
		try {
			workbook = new XSSFWorkbook(inputFile);
		} catch(IOException e) {
			throw ASExceptionHelper.notFoundException();
		}
		// daily data 
		XSSFSheet currentSheet = workbook.getSheetAt(1);
		Calendar cal = Calendar.getInstance();
		TimeZone tz = TimeZone.getTimeZone("GMT");
		cal.setTimeZone(tz);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setTimeZone(tz);
		List<String> name = CollectionFactory.createList();
		List<DashboardIndicatorData> indicators = CollectionFactory.createList();
		Date lowerL = new Date(0);
		Date upperL = new Date();
		String fromDate = null, toDate = null;
		List<StoreTicket> dailyTickets = CollectionFactory.createList();
		//List<StoreTicketByHour> hourlyTickets = CollectionFactory.createList();
		HashSet<String> subentityIds = new HashSet<>();
		byte hour = 0;
		double doubleV;
		for(Iterator<Row> rows = currentSheet.iterator(); rows.hasNext();) {
			Row row = rows.next();
			Date time = row.getCell(DATE_CELL_INDEX).getDateCellValue();
			String date = sdf.format(time);
			name.clear();
			name.add(row.getCell(STORE_NAME_CELL_INDEX).getStringCellValue());
			Store s = storeDao.getUsingNameAndBrandId(name, null).get(0);
			subentityIds.add(s.getIdentifier());
			if(lowerL.compareTo(time) > 0) {
				lowerL = time;
				fromDate = date;
			}
			if(upperL.compareTo(time) < 0) {
				upperL = time;
				toDate = date;
			}
			doubleV = row.getCell(PEASENTS_CELL_INDEX).getNumericCellValue();
			if(doubleV != 0) {
				indicators.add(buildIndicator(cal, date, time, doubleV, "apd_visitor", "Visitantes",
						"visitor_total_peasents", "Paseantes", brandId, s.getIdentifier(), name.get(0), hour));
			}
			doubleV = row.getCell(VISITS_CELL_INDEX).getNumericCellValue();
			if(doubleV != 0) {
				indicators.add(buildIndicator(cal, date, time, doubleV, "apd_visitor", "Visitantes",
						"visitor_total_visits", "Visitas", brandId, s.getIdentifier(), name.get(0), hour));
			}
			doubleV = row.getCell(TICKET_CELL_INDEX).getNumericCellValue();
			if(doubleV != 0) {
				indicators.add(buildIndicator(cal, date, time, doubleV, "apd_visitor", "Visitantes",
						"visitor_total_tickets", "Tickets", brandId, s.getIdentifier(), name.get(0), hour));
				StoreTicket ticket = new StoreTicket();
				ticket.setBrandId(brandId);
				ticket.setDate(date);
				ticket.setQty((int) doubleV);
				ticket.setStoreId(s.getIdentifier());
				ticket.setKey(sTicketDao.createKey());
				dailyTickets.add(ticket);
			}
		} for(String subentityId : subentityIds) {
			didDao.deleteUsingSubentityIdAndElementIdAndDate(subentityId, null, lowerL, upperL, tz);
			for(StoreTicket ticket : sTicketDao.getUsingStoreIdAndDatesAndRange(subentityId, fromDate, toDate,
					null, null, true)) sTicketDao.delete(ticket);
		} for(StoreTicket ticket : dailyTickets) {
			sTicketDao.createOrUpdate(ticket);
		} dailyTickets = null;
		/*/ hourly data FIXME only fetches totals
		currentSheet = workbook.getSheetAt(1);
		for(Iterator<Row> rows = currentSheet.iterator(); rows.hasNext();) {
			Row row = rows.next();
			Date time = row.getCell(DATE_CELL_INDEX).getDateCellValue();
			String date = sdf.format(time);
			if(lowerL.compareTo(time) > 0) {
				lowerL = time;
				fromDate = date;
			}
			if(upperL.compareTo(time) < 0) {
				upperL = time;
				toDate = date;
			}
			if(doubleV != 0) {
				indicators.add(buildIndicator(cal, date, time, doubleV, elementId, elementName, eSubId, eSubName, brandId, storeId, storeName, hour));
			}
			
		}*/
		for(DashboardIndicatorData did : indicators) {
			didDao.createOrUpdate(did);
		}
		try {
			workbook.close();
		} catch(IOException e) {
			throw new ASException(e.getMessage(), ASExceptionHelper.AS_EXCEPTION_INTERNALERROR_CODE);
		}
	}//importDb
	
	private DashboardIndicatorData buildIndicator(Calendar cal, String date, Date time, double value,
			String elementId, String elementName, String eSubId, String eSubName, String brandId, String storeId,
			String storeName, byte hour) throws ASException {
		DashboardIndicatorData did = new DashboardIndicatorData();
		cal.setTime(time);
		did.setDate(time);
		did.setDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
		did.setDoubleValue(value);
		did.setElementId(elementId);
		did.setElementName(elementName);
		did.setElementSubId(eSubId);
		did.setElementSubName(eSubName);
		did.setEntityId(brandId);
		did.setEntityKind(EntityKind.KIND_STORE);
		did.setRecordCount(1);
		did.setStringDate(date);
		did.setSubentityId(storeId);
		did.setSubentityName(storeName);
		did.setTimeZone((int)hour);
		did.setKey(didDao.createKey(did));
		return did;
	}

}
