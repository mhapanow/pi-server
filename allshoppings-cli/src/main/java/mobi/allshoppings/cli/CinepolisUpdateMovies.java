package mobi.allshoppings.cli;

import joptsimple.OptionParser;
import mobi.allshoppings.cinepolis.services.UpdateMoviesService;
import mobi.allshoppings.exception.ASException;


public class CinepolisUpdateMovies extends AbstractCLI {

	public static OptionParser buildOptionParser(OptionParser base) {
		if( base == null ) parser = new OptionParser();
		else parser = base;
		return parser;
	}

	public static void main(String args[]) throws ASException {
		
		UpdateMoviesService service = new UpdateMoviesService();
		service.doUpdate();
		System.exit(0);

	}
}