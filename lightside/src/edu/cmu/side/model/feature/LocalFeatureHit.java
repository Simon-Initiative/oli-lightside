package edu.cmu.side.model.feature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
//import java.util.logging.Logger;

/**
 *  A feature hit with a particular location within a document
 */
public class LocalFeatureHit extends FeatureHit
{
	/**
	 * {start, end} pairs indicating where this feature is expressed in this document.
	 * end - start = length of hit.
	 */
	private Collection<HitLocation> hits;
//	protected static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	public static class HitLocation implements Serializable
	{
		//backwards compatible serial ID with current serialized models - change if this class changes!
		private static final long serialVersionUID = -9160776027399318283L; 
		private int start;
		private int end;
		private String column;
		public HitLocation(String column, int start, int end)
		{	
			super();
//	        logger.info("LocalFeatureHit.java: Entered constructor #1");
			this.start = start;
			this.end = end;
			this.column = column;
		}
		public int getStart()
		{
			return start;
		}
		public int getEnd()
		{
			return end;
		}
		public String getColumn()
		{
			return column;
		}
		
	}
	
	/**
	 * 
	 * @param feature the feature which has hit the document in these spots.
	 * @param value the value expressed by the feature for this document. Its type should agree with feature.getFeatureType()
	 * @param documentIndex the index of the document in the current document set.
	 * @param hits {start, end} pairs indicating exactly where this feature hits in this document.
	 */
	public LocalFeatureHit(Feature feature, Object value, int documentIndex, Collection<HitLocation> hits)
	{
		super(feature, value, documentIndex);
//        logger.info("LocalFeatureHit.java: Entered constructor #1");
		this.hits = hits;
	}
	
	public LocalFeatureHit(Feature feature, Object value, int documentIndex, String column, int start, int end)
	{
		super(feature, value, documentIndex);
//        System.out.println("LocalFeatureHit.java: Entered constructor #2");
//		System.out.println("LocalFeatureHit constructor #2 caller: "); 
//		try
//		{
//			System.out.println("   class:  " + Class.forName(Thread.currentThread().getStackTrace()[2].getClassName()));
//			System.out.println("   method: " + Class.forName(Thread.currentThread().getStackTrace()[2].getMethodName()));
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
		hits = new ArrayList<HitLocation>();    // IS THIS NECESSARY ???
		addHit(column, start, end);
	}
	
//	public LocalFeatureHit(Feature feature, Object value, int documentIndex, int[] h)
//	{
//		super(feature, value, documentIndex);
//		this.singleHit = h;
//		hits = new ArrayList<int[]>();
//		addHit(h[0], h[1]);
//	}
	
	public Collection<HitLocation> getHits()
	{
		return hits;
	}
	
	@Override
	public String toString()
	{
		String x = feature+"@"+documentIndex+"("+value+"):";
		if(hits != null)
		{
			for(HitLocation h : hits)
				x+="("+h.column+": "+h.start+","+h.end+") ";
		}
		return x;
	}

	public void addHit(String column, int start, int end)
	{
 		hits.add(new HitLocation(column, start, end));
	}
}
