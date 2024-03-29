package mobi.allshoppings.cli;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import mobi.allshoppings.apdevice.APHHelper;
import mobi.allshoppings.exception.ASException;
import mobi.allshoppings.exception.ASExceptionHelper;
import mobi.allshoppings.tools.CollectionFactory;
import mx.getin.Constants;


public class GenerateAPHEFromExternal extends AbstractCLI {

	private static final Logger log = Logger.getLogger(GenerateAPHEFromExternal.class.getName());

	public static OptionParser buildOptionParser(OptionParser base) {
		if( base == null ) parser = new OptionParser();
		else parser = base;
		parser.accepts( "fromDate", "Date From" ).withRequiredArg().ofType( String.class );
		parser.accepts( "toDate", "Date To" ).withRequiredArg().ofType( String.class );
		parser.accepts( "hostname", "APHostname").withRequiredArg().ofType( String.class );
		return parser;
	}

	public static void setApplicationContext(ApplicationContext ctx) {
		context = ctx;
	}

	public static void main(String args[]) throws ASException {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			TimeZone tz = TimeZone.getTimeZone("GMT");
			sdf.setTimeZone(tz);

			APHHelper helper = (APHHelper)getApplicationContext().getBean("aphentry.helper");

			// Option parser help is in http://pholser.github.io/jopt-simple/examples.html
			OptionSet options = parser.parse(args);

			String sFromDate = null;
			String sToDate = null;
			Date fromDate = null;
			Date toDate = null;
			String hostname = null;

			try {
				if( options.has("fromDate")) sFromDate = (String)options.valueOf("fromDate");
				if( options.has("toDate")) sToDate = (String)options.valueOf("toDate");

				if( StringUtils.hasText(sFromDate)) {
					fromDate = sdf.parse(sFromDate);
				} else {
					fromDate = sdf.parse(sdf.format(new Date(System.currentTimeMillis() - Constants.DAY_IN_MILLIS)));
				}

				if( StringUtils.hasText(sToDate)) {
					toDate = sdf.parse(sToDate);
				} else {
					toDate = new Date(fromDate.getTime() + Constants.DAY_IN_MILLIS);
				}

				if( options.has("hostname")) {
					hostname = (String)options.valueOf("hostname");
				}

			} catch( Exception e ) {
				e.printStackTrace();
				usage(parser);
			}

			log.log(Level.INFO, "Generating APHEntries");
			List<String> hostnames = CollectionFactory.createList();
			if(StringUtils.hasText(hostname)) {
				hostnames.add(hostname);
			}

			helper.setScanInDevices(false);
			helper.setUseCache(true);

			Date ffromDate = new Date(fromDate.getTime());
			Date ftoDate = new Date(fromDate.getTime());
			while( ftoDate.before(toDate)) {

				ffromDate = new Date(ftoDate.getTime());
				ftoDate = new Date(ftoDate.getTime() + Constants.DAY_IN_MILLIS);

				helper.generateAPHEntriesFromExternalAPH(ffromDate, ftoDate, hostnames, false);

			}

		} catch( Exception e ) {
			throw ASExceptionHelper.defaultException(e.getMessage(), e);
		}
		System.exit(0);
	}

}
