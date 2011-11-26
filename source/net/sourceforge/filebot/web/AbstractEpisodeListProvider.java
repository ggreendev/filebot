
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeUtilities.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;


public abstract class AbstractEpisodeListProvider implements EpisodeListProvider {
	
	@Override
	public boolean hasSingleSeasonSupport() {
		return true;
	}
	

	@Override
	public boolean hasLocaleSupport() {
		return false;
	}
	

	protected abstract List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception;
	

	protected abstract List<Episode> fetchEpisodeList(SearchResult searchResult, Locale locale) throws Exception;
	

	public List<SearchResult> search(String query) throws Exception {
		return search(query, getDefaultLocale());
	}
	

	public List<SearchResult> search(String query, Locale locale) throws Exception {
		ResultCache cache = getCache();
		List<SearchResult> results = (cache != null) ? cache.getSearchResult(query, locale) : null;
		if (results != null) {
			return results;
		}
		
		// perform actual search
		results = fetchSearchResult(query, locale);
		
		// cache results and return
		return (cache != null) ? cache.putSearchResult(query, locale, results) : results;
	}
	

	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception {
		return getEpisodeList(searchResult, getDefaultLocale());
	}
	

	public List<Episode> getEpisodeList(SearchResult searchResult, Locale locale) throws Exception {
		ResultCache cache = getCache();
		List<Episode> episodes = (cache != null) ? cache.getEpisodeList(searchResult, locale) : null;
		if (episodes != null) {
			return episodes;
		}
		
		// perform actual search
		episodes = fetchEpisodeList(searchResult, locale);
		
		// cache results and return
		return (cache != null) ? cache.putEpisodeList(searchResult, locale, episodes) : episodes;
	}
	

	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception {
		return getEpisodeList(searchResult, season, getDefaultLocale());
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season, Locale locale) throws Exception {
		List<Episode> all = getEpisodeList(searchResult, locale);
		List<Episode> eps = filterBySeason(all, season);
		
		if (eps.isEmpty()) {
			throw new SeasonOutOfBoundsException(searchResult.getName(), season, getLastSeason(all));
		}
		
		return eps;
	}
	

	public Locale getDefaultLocale() {
		return Locale.ENGLISH;
	}
	

	public ResultCache getCache() {
		return null;
	}
	

	protected static class ResultCache {
		
		private final String id;
		private final Cache cache;
		

		public ResultCache(String id, Cache cache) {
			this.id = id;
			this.cache = cache;
		}
		

		protected String normalize(String query) {
			return query == null ? null : query.trim().toLowerCase();
		}
		

		public <T extends SearchResult> List<T> putSearchResult(String query, Locale locale, List<T> value) {
			try {
				cache.put(new Element(new Key(id, normalize(query), locale), value.toArray(new SearchResult[0])));
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			
			return value;
		}
		

		public List<SearchResult> getSearchResult(String query, Locale locale) {
			try {
				Element element = cache.get(new Key(id, normalize(query), locale));
				if (element != null) {
					return Arrays.asList(((SearchResult[]) element.getValue()));
				}
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			
			return null;
		}
		

		public List<Episode> putEpisodeList(SearchResult key, Locale locale, List<Episode> episodes) {
			try {
				cache.put(new Element(new Key(id, key, locale), episodes.toArray(new Episode[0])));
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			
			return episodes;
		}
		

		public List<Episode> getEpisodeList(SearchResult key, Locale locale) {
			try {
				Element element = cache.get(new Key(id, key, locale));
				if (element != null) {
					return Arrays.asList((Episode[]) element.getValue());
				}
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			
			return null;
		}
		

		private static class Key implements Serializable {
			
			protected Object[] fields;
			

			public Key(Object... fields) {
				this.fields = fields;
			}
			

			@Override
			public int hashCode() {
				return Arrays.hashCode(fields);
			}
			

			@Override
			public boolean equals(Object other) {
				if (other instanceof Key) {
					return Arrays.equals(this.fields, ((Key) other).fields);
				}
				
				return false;
			}
		}
	}
	
}
