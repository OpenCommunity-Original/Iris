package ninja.bytecode.iris.spec;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.World.Environment;

import ninja.bytecode.iris.Iris;
import ninja.bytecode.shuriken.collections.GList;
import ninja.bytecode.shuriken.execution.J;
import ninja.bytecode.shuriken.execution.TaskExecutor.TaskGroup;
import ninja.bytecode.shuriken.json.JSONArray;
import ninja.bytecode.shuriken.json.JSONException;
import ninja.bytecode.shuriken.json.JSONObject;

public class IrisDimension
{
	private String name;
	private Environment environment;
	GList<IrisBiome> biomes;
	
	public IrisDimension(JSONObject o) throws JSONException, IOException
	{
		this();
		fromJSON(o);
	}
	
	public IrisDimension()
	{
		biomes = new GList<IrisBiome>();
		environment = Environment.NORMAL;
	}
	
	public void fromJSON(JSONObject o) throws JSONException, IOException
	{
		name = o.getString("name");
		J.attempt(() -> environment = Environment.valueOf(o.getString("environment").toUpperCase().replaceAll(" ", "_")));
		
		try
		{
			biomes = biomesFromArray(o.getJSONArray("biomes"));
		}
		
		catch(Throwable e)
		{
			e.printStackTrace();
		}
		
		if(o.has("focus"))
		{
			String focus = o.getString("focus");
			
			for(IrisBiome i : biomes.copy())
			{
				if(!i.getName().toLowerCase().replaceAll(" ", "_").equals(focus))
				{
					biomes.remove(i);
				}
			}
		}
	}
	
	public JSONObject toJSON()
	{
		JSONObject o = new JSONObject();
		
		o.put("name", name);
		o.put("environment", environment.name().toLowerCase().replaceAll("_", " "));
		o.put("biomes", biomesToArray(biomes));
		
		return o;
	}
	
	private GList<IrisBiome> biomesFromArray(JSONArray a) throws JSONException, IOException
	{
		GList<IrisBiome> b = new GList<>();
		TaskGroup g = Iris.buildPool.startWork();
		ReentrantLock lock = new ReentrantLock();
		
		for(int i = 0; i < a.length(); i++)
		{
			int ii = i;
			g.queue(() -> {
				IrisBiome bb = Iris.loadBiome(a.getString(ii));
				lock.lock();
				Iris.biomes.put(a.getString(ii), bb);
				b.add(bb);
				lock.unlock();
			});
		}
		
		g.execute();
		
		return b;
	}

	private JSONArray biomesToArray(GList<IrisBiome> b)
	{
		JSONArray a = new JSONArray();
		
		for(IrisBiome i : b)
		{
			a.put(i.getName().toLowerCase().replaceAll(" ", "_"));
		}
		
		return a;
	}

	public GList<IrisBiome> getBiomes()
	{
		return biomes;
	}

	public String getName()
	{
		return name;
	}
}