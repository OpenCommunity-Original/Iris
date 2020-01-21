package ninja.bytecode.iris.generator;

import java.util.function.Consumer;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import mortar.api.nms.NMP;
import mortar.api.sched.J;
import mortar.compute.math.M;
import mortar.lang.collection.FinalDouble;
import ninja.bytecode.iris.Iris;
import ninja.bytecode.iris.util.ChronoQueue;
import ninja.bytecode.iris.util.ObjectMode;

public class WorldReactor
{
	private final World world;

	public WorldReactor(World world)
	{
		this.world = world;
	}

	public void generateRegionNormal(Player p, boolean force, double mst, Consumer<Double> progress, Runnable done)
	{
		ChronoQueue q = new ChronoQueue(mst, 10240);
		FinalDouble of = new FinalDouble(0D);
		FinalDouble max = new FinalDouble(0D);

		for(int xx = p.getLocation().getChunk().getX() - 32; xx < p.getLocation().getChunk().getX() + 32; xx++)
		{
			int x = xx;

			for(int zz = p.getLocation().getChunk().getZ() - 32; zz < p.getLocation().getChunk().getZ() + 32; zz++)
			{
				int z = zz;

				if(world.isChunkLoaded(x, z) || world.loadChunk(x, z, false))
				{
					if(Iris.settings.performance.objectMode.equals(ObjectMode.PARALLAX) && world.getGenerator() instanceof IrisGenerator)
					{
						IrisGenerator gg = ((IrisGenerator) world.getGenerator());
						gg.getWorldData().deleteChunk(x, z);
					}

					max.add(1);
					q.queue(() ->
					{
						world.regenerateChunk(x, z);

						Chunk cc = world.getChunkAt(x, z);
						NMP.host.relight(cc);
						of.add(1);

						if(of.get() == max.get())
						{
							progress.accept(1D);
							q.dieSlowly();
							done.run();
						}

						else
						{
							progress.accept(M.clip(of.get() / max.get(), 0D, 1D));
						}

					});
				}

			}
		}

		J.s(() ->
		{
			q.dieSlowly();
		}, 20);
	}
}